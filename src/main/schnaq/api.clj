(ns schnaq.api
  (:require [expound.alpha :as expound]
            [muuntaja.core :as m]
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
            [schnaq.api.analytics :refer [analytics-routes]]
            [schnaq.api.common :refer [other-routes]]
            [schnaq.api.debug :refer [debug-routes]]
            [schnaq.api.discussion :refer [discussion-routes]]
            [schnaq.api.emails :refer [email-routes]]
            [schnaq.api.feedback :refer [feedback-routes]]
            [schnaq.api.hub :refer [hub-routes]]
            [schnaq.api.middlewares :as middlewares]
            [schnaq.api.schnaq :refer [schnaq-routes]]
            [schnaq.api.summaries :refer [summary-routes]]
            [schnaq.api.user :refer [user-routes]]
            [schnaq.auth :as auth]
            [schnaq.config :as config]
            [schnaq.config.keycloak :as keycloak-config]
            [schnaq.config.shared :as shared-config]
            [schnaq.core :as schnaq-core]
            [taoensso.timbre :as log])
  (:gen-class))


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

  ## General
  Most routes work with anonymous users, where a `nickname` can be set. If you are authenticated and send back a valid JWT token, you most often can omit the `nickname` field in the request. Then, you act as a registered user.

  ## Authentication
  Many routes require authentication. To authenticate you against the backend, grab a JWT token from the authorized Keycloak instance and put in in your header. Or use the `Authorize`-Button on the right side. Use `swagger` as your client_id.

  The header should look like this: `Authorization: Token <your token>`. Configure your JWT token in by using the \"Authorize\"-Button.

  ## Content Negotiation
  You can choose the format of your response by specifying the corresponding header. `json`, `edn`, `transit+json` and `transit+msgpack` are currently supported. For example:
  `curl https://api.staging.schnaq.com/ping -H \"Accept: application/edn\"`")

(def app
  (ring/ring-handler
    (ring/router
      [analytics-routes
       debug-routes
       discussion-routes
       email-routes
       feedback-routes
       hub-routes
       other-routes
       schnaq-routes
       summary-routes
       user-routes

       ["/swagger.json"
        {:get {:no-doc true
               :swagger {:info {:title "schnaq API"
                                :basePath "/"
                                :version "1.0.0"
                                :description description}
                         :securityDefinitions {:keycloak {:type "oauth2"
                                                          :flow "implicit"
                                                          :name "Authorization"
                                                          :description "Use `swagger` as the client-id."
                                                          :authorizationUrl (format "%s" keycloak-config/openid-endpoint)}}
                         :security [{:keycloak []}]}
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
                           auth/replace-bearer-with-token
                           auth/wrap-jwt-authentication]}
       ::middleware/registry {:user/authenticated? auth/authenticated?-middleware
                              :user/admin? auth/admin?-middleware
                              :user/beta-tester? auth/beta-tester?-middleware
                              :discussion/valid-share-hash? middlewares/valid-discussion?-middleware
                              :discussion/valid-credentials? middlewares/valid-credentials?-middleware}})
    (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path "/"
         :config {:validatorUrl nil
                  :operationsSorter "alpha"}})
      (ring/redirect-trailing-slash-handler {:method :strip})
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
