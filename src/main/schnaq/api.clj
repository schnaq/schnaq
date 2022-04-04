(ns schnaq.api
  (:require [expound.alpha :as expound]
            [mount.core :as mount :refer [defstate]]
            [muuntaja.core :as m]
            [org.httpkit.server :as server]
            [reitit.coercion.spec]
            [reitit.core :as r]
            [reitit.dev.pretty :as pretty]
            [reitit.middleware :as middleware]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.spec :as rrs]
            [reitit.spec :as rs]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.cors :refer [wrap-cors]]
            [schnaq.api.activation :refer [activation-routes]]
            [schnaq.api.analytics :refer [analytics-routes]]
            [schnaq.api.common :refer [other-routes]]
            [schnaq.api.debug :refer [debug-routes]]
            [schnaq.api.discussion :refer [discussion-routes]]
            [schnaq.api.emails :refer [email-routes]]
            [schnaq.api.feedback :refer [feedback-routes]]
            [schnaq.api.hub :refer [hub-routes]]
            [schnaq.api.middlewares :as middlewares]
            [schnaq.api.poll :refer [poll-routes]]
            [schnaq.api.profiling :as profiling]
            [schnaq.api.schnaq :refer [schnaq-routes]]
            [schnaq.api.subscription.stripe :refer [stripe-routes]]
            [schnaq.api.summaries :refer [summary-routes]]
            [schnaq.api.themes :refer [theme-routes]]
            [schnaq.api.user :refer [user-routes]]
            [schnaq.api.wordcloud :refer [wordcloud-routes]]
            [schnaq.auth :as auth]
            [schnaq.auth.middlewares :as auth-middlewares]
            [schnaq.config :as config]
            [schnaq.config.cleverreach :as cconfig]
            [schnaq.config.keycloak :as keycloak-config]
            [schnaq.config.shared :as shared-config]
            [schnaq.config.stripe :refer [prices]]
            [schnaq.config.summy :as summy-config]
            [schnaq.core] ;; Keep this import to activate database etc.
            [schnaq.toolbelt :as toolbelt]
            [schnaq.websockets.handler :refer [websocket-routes]]
            [schnaq.websockets.messages]
            [taoensso.timbre :as log])
  (:gen-class))

;; -----------------------------------------------------------------------------
;; General

(defn- say-hello
  "Print some debug information to the console when the system is loaded."
  []
  (log/info "Welcome to schnaq's Backend 🧙")
  (log/info (format "Build Hash: %s" config/build-hash))
  (log/info (format "Environment: %s" shared-config/environment))
  (log/info (format "Database Name: %s" config/db-name))
  (log/info (format "Database URI (truncated): %s..." (subs config/datomic-uri 0 30)))
  (log/info (format "Summy URL: %s" summy-config/base-url))
  (log/info (format "Frontend URL: %s, host: %s" config/frontend-url config/frontend-host))
  (log/info (if (:sender-password config/email) "E-Mail configured" "E-Mail not configured"))
  (log/info (format "[Keycloak] Server: %s, Realm: %s" keycloak-config/server keycloak-config/realm))
  (log/info (format "[Stripe] Price ID schnaq pro monthly: %s" (:schnaq.pro/monthly prices)))
  (log/info (format "[Stripe] Price ID schnaq pro yearly: %s" (:schnaq.pro/yearly prices)))
  (log/info (format "[Stripe] Webhook access key (truncated): %s..." (subs config/stripe-webhook-access-key 0 15)))
  (log/info (format "[Stripe] Secret key (truncated): %s..." (subs config/stripe-secret-api-key 0 15)))
  (log/info (format "[CleverReach] Enabled? %b, Receiver group: %d, client-id: %s, client-secret: %s..."
                    cconfig/enabled? cconfig/receiver-group cconfig/client-id (subs cconfig/client-secret 0 10))))

(def ^:private description
  "This is the main Backend for schnaq.

  ## General
  Most routes work with anonymous users, where a `nickname` can be set. If you are authenticated and send back a valid JWT token, you most often can omit the `nickname` field in the request. Then, you act as a registered user.

  ## Authentication
  Many routes require authentication. To authenticate you against the backend, grab a JWT token from the authorized Keycloak instance and put in your header. Or use the `Authorize`-Button on the right side. Use `swagger` as your client_id.

  The header should look like this: `Authorization: Token <your token>`. Configure your JWT token in by using the \"Authorize\"-Button.

  ## CSRF

  A special header needs to be set for POST, PUT and DELETE to work. Use the Authorize button and give it any value.

  ## Content Negotiation
  You can choose the format of your response by specifying the corresponding header. `json`, `edn`, `transit+json` and `transit+msgpack` are currently supported. For example:
  `curl https://api.staging.schnaq.com/ping -H \"Accept: application/edn\"`")

(defn- router
  "Create a router with all routes.
  Websockets are stateful, so this is a special case, which can be excluded."
  ([] (router false))
  ([with-websockets?]
   (ring/router
    [activation-routes
     analytics-routes
     debug-routes
     discussion-routes
     email-routes
     feedback-routes
     hub-routes
     other-routes
     poll-routes
     schnaq-routes
     stripe-routes
     summary-routes
     theme-routes
     user-routes
     (when with-websockets? (websocket-routes))
     wordcloud-routes

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
                                                        :authorizationUrl (format "%s" keycloak-config/openid-endpoint)}
                                             :schnaq-csrf-header {:type "apiKey"
                                                                  :in "header"
                                                                  :name "X-Schnaq-CSRF"
                                                                  :description "Use any value, the header needs to be set, that's it."
                                                                  :example "Elephants like security"
                                                                  :default "Phanty"}}
                       :security [{:keycloak []
                                   :schnaq-csrf-header []}]}
             :handler (swagger/create-swagger-handler)}}]]
    {:exception pretty/exception
     :validate rrs/validate
     ::rs/explain expound/expound-str
     :data {:coercion reitit.coercion.spec/coercion
            :muuntaja m/instance
            :middleware [swagger/swagger-feature
                         parameters/parameters-middleware ;; query-params & form-params
                         middlewares/convert-body-middleware ;; must be called *before* muuntaja/format-middleware
                         muuntaja/format-middleware
                         middlewares/exception-printing-middleware
                         coercion/coerce-response-middleware ;; coercing response bodies
                         coercion/coerce-request-middleware ;; coercing request parameters
                         multipart/multipart-middleware
                         auth-middlewares/replace-bearer-with-token
                         auth/wrap-jwt-authentication
                         auth-middlewares/update-jwt-middleware
                         middlewares/wrap-custom-schnaq-csrf-header
                         profiling/profiling-middleware]}
     ::middleware/registry {:app/valid-code? auth-middlewares/valid-app-code?-middleware
                            :discussion/parent-unlocked? middlewares/parent-unlocked?-middleware
                            :discussion/valid-credentials? middlewares/valid-credentials?-middleware
                            :discussion/valid-share-hash? middlewares/valid-discussion?-middleware
                            :discussion/valid-statement? middlewares/valid-statement?-middleware
                            :user/admin? auth-middlewares/admin?-middleware
                            :user/analytics-admin? auth-middlewares/analytics-admin?-middleware
                            :user/authenticated? auth-middlewares/authenticated?-middleware
                            :user/beta-tester? auth-middlewares/beta-tester?-middleware
                            :user/pro-user? auth-middlewares/pro-user?-middleware}})))

(defn route-by-name
  "Return a route by its name."
  ([route-name]
   (route-by-name route-name false))
  ([route-name with-websockets?]
   (r/match-by-name (router with-websockets?) route-name)))

(defn app
  ([] (app false))
  ([with-websockets?]
   (ring/ring-handler
    (router with-websockets?)
    (ring/routes
     (swagger-ui/create-swagger-ui-handler
      {:path "/"
       :config {:validatorUrl nil
                :operationsSorter "alpha"}})
     (ring/redirect-trailing-slash-handler {:method :strip})
     (ring/create-default-handler)))))

(def allowed-origins
  "Calculate valid origins based on the environment configuration and the
  allowed frontend host."
  (->> ["schnaq.com" "schnaq.de" "schnaq.app" config/frontend-host]
       (concat config/additional-cors)
       (remove empty?)
       (mapv toolbelt/build-allowed-origin)
       doall))

(def allowed-http-verbs
  #{:get :put :post :delete :options})

(defstate api
  :start
  (let [origins (if shared-config/production? allowed-origins (conj allowed-origins #".*"))]
    (say-hello)
    (log/info (format "Starting web-server at %s" shared-config/api-url))
    (log/info (format "Allowed Origins: %s" origins))
    (server/run-server
     (wrap-cors (app true)
                :access-control-allow-origin origins
                :access-control-allow-methods allowed-http-verbs)
     {:port shared-config/api-port}))
  :stop (when api (api :timeout 1000)))

(defn -main
  "This is our main entry point for the REST API Server."
  [& _args]
  (log/info (mount/start)))

(comment
  "Start the server from here"
  (-main)
  (mount/start)
  (mount/stop)
  :end)

;; TODO karten nachträglich (als mod) locken und unlocken können - backend + frontend wire
