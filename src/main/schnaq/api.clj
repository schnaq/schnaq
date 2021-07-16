(ns schnaq.api
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [ghostwheel.core :refer [>defn-]]
            [muuntaja.core :as m]
            [org.httpkit.client :as http-client]
            [org.httpkit.server :as server]
            [reitit.coercion.spec]
            [reitit.dev.pretty :as pretty]
            [reitit.middleware :as middleware]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.spec :as rrs]
            [reitit.spec :as rs]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.http-response :refer [ok created bad-request forbidden]]
            [schnaq.api.analytics :refer [analytics-routes]]
            [schnaq.api.discussion :refer [discussion-routes]]
            [schnaq.api.emails :refer [email-routes]]
            [schnaq.api.feedback :refer [feedback-routes]]
            [schnaq.api.hub :refer [hub-routes]]
            [schnaq.api.schnaq :refer [schnaq-routes]]
            [schnaq.api.summaries :refer [summary-routes]]
            [schnaq.api.toolbelt :as at]
            [schnaq.api.user :refer [user-routes]]
            [schnaq.auth :as auth]
            [schnaq.config :as config]
            [schnaq.config.keycloak :as keycloak-config]
            [schnaq.config.mailchimp :as mailchimp-config]
            [schnaq.config.shared :as shared-config]
            [schnaq.core :as schnaq-core]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.user :as user-db]
            [schnaq.emails :as emails]
            [schnaq.export :as export]
            [schnaq.validator :as validator]
            [taoensso.timbre :as log])
  (:gen-class))

(s/def :http/status nat-int?)
(s/def :http/headers map?)
(s/def :ring/response (s/keys :req-un [:http/status :http/headers]))
(s/def :ring/body-params map?)
(s/def :ring/route-params map?)
(s/def :ring/request (s/keys :opt [:ring/body-params :ring/route-params]))

(defn- ping
  "Route to ping the API. Used in our monitoring system."
  [_]
  (ok {:text "🧙‍♂️"}))

(defn- add-author
  "Generate a user based on the nickname. This is an *anonymous* user, and we
  can only refer to the user by the nickname. So this function is idempotent and
  returns always the same id when providing the same nickname."
  [{:keys [parameters]}]
  (let [author-name (get-in parameters [:body :nickname])
        user-id (user-db/add-user-if-not-exists author-name)]
    (created "" {:user-id user-id})))

(defn- make-discussion-read-only!
  "Makes a discussion read-only if share- and edit-hash are correct and present."
  [{:keys [parameters]}]
  (let [{:keys [share-hash edit-hash]} (:body parameters)]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/info "Setting discussion to read-only: " share-hash)
          (discussion-db/set-discussion-read-only share-hash)
          (ok {:share-hash share-hash}))
      (validator/deny-access))))

(defn- make-discussion-writeable!
  "Makes a discussion writeable if discussion-admin credentials are there."
  [{:keys [parameters]}]
  (let [{:keys [share-hash edit-hash]} (:body parameters)]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/info "Removing read-only from discussion: " share-hash)
          (discussion-db/remove-read-only share-hash)
          (ok {:share-hash share-hash}))
      (validator/deny-access))))

(defn- disable-pro-con!
  "Disable pro-con option for a schnaq."
  [{:keys [parameters]}]
  (let [{:keys [disable-pro-con? share-hash edit-hash]} (:body parameters)]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/info "Setting \"disable-pro-con option\" to" disable-pro-con? "for schnaq:" share-hash)
          (discussion-db/set-disable-pro-con share-hash disable-pro-con?)
          (ok {:share-hash share-hash}))
      (validator/deny-access))))

(defn- delete-schnaq!
  "Sets the state of a schnaq to delete. Should be only available to superusers (admins)."
  [{:keys [parameters]}]
  (let [{:keys [share-hash]} (:body parameters)]
    (if (discussion-db/delete-discussion share-hash)
      (ok {:share-hash share-hash})
      (bad-request (at/build-error-body :error-deleting-schnaq "An error occurred, while deleting the schnaq.")))))

(defn- check-credentials!
  "Checks whether share-hash and edit-hash match.
  If the user is logged in and the credentials are valid, they are added as an admin."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash edit-hash]} (:body parameters)
        valid-credentials? (validator/valid-credentials? share-hash edit-hash)
        keycloak-id (:sub identity)]
    (when (and valid-credentials? keycloak-id)
      (discussion-db/add-admin-to-discussion share-hash keycloak-id))
    (if valid-credentials?
      (ok {:valid-credentials? valid-credentials?})
      (forbidden {:valid-credentials? valid-credentials?}))))

(defn- export-txt-data
  "Exports the discussion data as a string."
  [{:keys [parameters]}]
  (let [{:keys [share-hash]} (:query parameters)]
    (if (validator/valid-discussion? share-hash)
      (do (log/info "User is generating a txt export for discussion" share-hash)
          (ok {:string-representation (export/generate-text-export share-hash)}))
      at/not-found-hash-invalid)))

(defn- subscribe-lead-magnet!
  "Subscribes to the mailing list and sends the lead magnet to the email-address."
  [{:keys [parameters]}]
  (let [email (get-in parameters [:body :email])
        options {:timeout 10000
                 :basic-auth ["user" mailchimp-config/api-key]
                 :body (json/write-str {:email_address email
                                        :status "subscribed"
                                        :email_type "html"
                                        :tags ["lead-magnet" "datenschutz"]})
                 :user-agent "schnaq Backend Application"}]
    (http-client/post mailchimp-config/subscribe-uri options)
    (if (emails/send-remote-work-lead-magnet email)
      (ok {:status :ok})
      (bad-request (at/build-error-body :failed-subscription "Something went wrong. Check your Email-Address and try again.")))))


;; -----------------------------------------------------------------------------
;; General

(defonce current-server (atom nil))

(defn- stop-server []
  (when-not (nil? @current-server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@current-server :timeout 100)
    (reset! current-server nil)))

(defn- say-hello
  "Print some debug information to the console when the system is loaded."
  []
  (log/info "Welcome to schnaq's Backend 🧙")
  (log/info (format "Build Hash: %s" config/build-hash))
  (log/info (format "Environment: %s" shared-config/environment))
  (log/info (format "Database Name: %s" config/db-name))
  (log/info (format "Database URI (truncated): %s" (subs config/datomic-uri 0 30)))
  (log/info (format "[Keycloak] Server: %s, Realm: %s" keycloak-config/server keycloak-config/realm)))

(def allowed-origin
  "Regular expression, which defines the allowed origins for API requests."
  #"^((https?:\/\/)?(.*\.)?(schnaq\.(com|de)))($|\/.*$)")

(def ^:private description
  "This is the main Backend for schnaq.

  ## Authentication
  Many routes require authentication. To authenticate you against the backend, grab a JWT token from the authorized Keycloak instance and put in in your header.

  The header should look like this: `Authorization: Token <your token>`.")

(def app
  (ring/ring-handler
    (ring/router
      [(when-not shared-config/production?
         ["" {:swagger {:tags ["debug"]}}
          ["/debug/headers" {:get identity}]])
       ["/ping" {:get ping
                 :description (at/get-doc #'ping)
                 :responses {200 {:body {:text string?}}}}]
       ["/export/txt" {:get export-txt-data
                       :description (at/get-doc #'export-txt-data)
                       :swagger {:tags ["exports"]}
                       :parameters {:query {:share-hash :discussion/share-hash}}
                       :responses {200 {:body {:string-representation string?}}
                                   404 at/response-error-body}}]
       ["/author/add" {:put add-author
                       :description (at/get-doc #'add-author)
                       :parameters {:body {:nickname :user/nickname}}
                       :responses {201 {:body {:user-id :db/id}}}}]
       ["/credentials/validate" {:post check-credentials!
                                 :description (at/get-doc #'check-credentials!)
                                 :responses {200 {:body {:valid-credentials? boolean?}}
                                             403 {:body {:valid-credentials? boolean?}}}
                                 :parameters {:body {:share-hash :discussion/share-hash
                                                     :edit-hash :discussion/edit-hash}}}]
       ["/lead-magnet/subscribe" {:post subscribe-lead-magnet!
                                  :description (at/get-doc #'subscribe-lead-magnet!)
                                  :parameters {:body {:email string?}}
                                  :responses {200 {:body {:status keyword?}}
                                              400 at/response-error-body}}]

       ["/admin" {:swagger {:tags ["admin"]}
                  :responses {401 at/response-error-body}
                  :middleware [:authenticated? :admin?]}
        ["/schnaq/delete" {:delete delete-schnaq!
                           :description (at/get-doc #'delete-schnaq!)
                           :parameters {:body {:share-hash :discussion/share-hash}}
                           :responses {200 {:share-hash :discussion/share-hash}
                                       400 at/response-error-body}}]]
       ["/manage" {:swagger {:tags ["manage"]}
                   :parameters {:body {:share-hash :discussion/share-hash
                                       :edit-hash :discussion/edit-hash}}
                   :responses {403 at/response-error-body}}
        ["/schnaq" {:responses {200 {:body {:share-hash :discussion/share-hash}}}}
         ["/disable-pro-con" {:put disable-pro-con!
                              :description (at/get-doc #'disable-pro-con!)
                              :parameters {:body {:disable-pro-con? boolean?}}}]
         ["/make-read-only" {:put make-discussion-read-only!
                             :description (at/get-doc #'make-discussion-read-only!)}]
         ["/make-writeable" {:put make-discussion-writeable!
                             :description (at/get-doc #'make-discussion-writeable!)}]]]

       analytics-routes
       discussion-routes
       email-routes
       feedback-routes
       hub-routes
       schnaq-routes
       summary-routes
       user-routes

       ["/swagger.json"
        {:get {:no-doc true
               :swagger {:info {:title "schnaq API"
                                :basePath "/"
                                :version "1.0.0"
                                :description description}
                         :securityDefinitions {:bearerAuth {:type "apiKey"
                                                            :name "Authorization"
                                                            :in "header"}}
                         :security [{:bearerAuth []}]}
               :handler (swagger/create-swagger-handler)}}]]
      {:exception pretty/exception
       :validate rrs/validate
       ::rs/explain expound/expound-str
       :data {:coercion reitit.coercion.spec/coercion
              :muuntaja m/instance
              :middleware [swagger/swagger-feature
                           parameters/parameters-middleware ;; query-params & form-params
                           muuntaja/format-middleware
                           exception/exception-middleware   ;; exception handling
                           coercion/coerce-response-middleware ;; coercing response bodys
                           coercion/coerce-request-middleware ;; coercing request parameters
                           multipart/multipart-middleware
                           auth/wrap-jwt-authentication]}
       ::middleware/registry {:authenticated? auth/authenticated?-middleware
                              :admin? auth/admin?-middleware
                              :beta-tester? auth/beta-tester?-middleware}})
    (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path "/"
         :config {:validatorUrl nil
                  :operationsSorter "alpha"}})
      (ring/create-default-handler))))

(defn -main
  "This is our main entry point for the REST API Server."
  [& _args]
  (let [allowed-origins [allowed-origin]
        allowed-origins' (if shared-config/production? allowed-origins (conj allowed-origins #".*"))]
    ; Run the server with Ring.defaults middle-ware
    (say-hello)
    (schnaq-core/-main)
    (reset! current-server
            (server/run-server
              (-> #'app
                  (wrap-cors :access-control-allow-origin allowed-origins'
                             :access-control-allow-methods [:get :put :post :delete]))
              {:port shared-config/api-port}))
    (log/info (format "Running web-server at %s" shared-config/api-url))
    (log/info (format "Allowed Origin: %s" allowed-origins'))))

(comment
  "Start the server from here"
  (-main)
  (stop-server)
  :end)
