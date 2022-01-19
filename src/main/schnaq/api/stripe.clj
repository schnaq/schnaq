(ns schnaq.api.stripe
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn- => ?]]
            [ring.util.http-response :refer [ok forbidden not-found bad-request]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.database.user :as user-db]
            [taoensso.timbre :as log])
  (:import [com.stripe Stripe]
           [com.stripe.exception InvalidRequestException SignatureVerificationException]
           [com.stripe.model Price Subscription]
           [com.stripe.model.checkout Session]
           [com.stripe.net Webhook]
           [com.stripe.param SubscriptionUpdateParams]))

(def ^:private error-article-not-found
  (at/build-error-body :article/not-found "Article could not be found."))

(set! (. Stripe -apiKey) config/stripe-secret-api-key)

(s/def ::subscription (partial instance? Subscription))

(s/def ::status #{:incomplete :incomplete_expired :trialing :active :past_due :canceled :unpaid})
(s/def ::cancelled? boolean?)
(s/def ::period-start nat-int?)
(s/def ::period-start nat-int?)
(s/def ::cancel-at nat-int?)
(s/def ::cancelled-at nat-int?)
(s/def ::subscription-status
  (s/keys :req-un [::status ::cancelled? ::period-start ::period-end]
          :opt-un [::cancel-at ::cancelled-at]))

;; -----------------------------------------------------------------------------


(>defn- build-checkout-session-parameters
  "Configure all checkout-session parameters. Adds items, defines URLs and adds
        costumer metadata to the user."
  [price-id keycloak-id email]
  [:stripe.price/id :user.registered/keycloak-id :user.registered/email => map?]
  (let [items [{"price" price-id
                "quantity" 1}]]
    {"success_url" (format "%s/subscription/success" config/frontend-url)
     "cancel_url" (format "%s/subscription/cancel" config/frontend-url)
     "mode" "subscription"
     "client_reference_id" keycloak-id
     "customer_email" email
     "metadata" {"keycloak-id" keycloak-id}
     "subscription_data" {"metadata" {"keycloak-id" keycloak-id}}
     "line_items" items}))

(defn- create-checkout-session
  "Open stripe's checkout page with the currently selected item."
  [{:keys [identity parameters]}]
  (try
    (let [price-id (get-in parameters [:body :price-id])
          checkout-session-parameters (build-checkout-session-parameters price-id (:id identity) (:email identity))
          session (Session/create checkout-session-parameters)]
      (ok {:redirect (.getUrl session)}))
    (catch InvalidRequestException _
      (bad-request error-article-not-found))))

(>defn- retrieve-price [price-id]
  [:stripe.price/id => (? :stripe/price)]
  (try
    (let [price (Price/retrieve price-id)]
      (if (.getActive price)
        {:id price-id
         :cost (-> price .getUnitAmount (/ 100) float)}
        (do (log/error "Queried article is not active:" price-id)
            (at/build-error-body
             :stripe.price/inactive
             (format "Queried article is not active: %s" price-id)))))
    (catch InvalidRequestException _
      (log/error "Price could not be found:" price-id)
      (at/build-error-body
       :stripe.price/invalid-request
       (format "Request could not be fulfilled. Maybe the queried price is not available: %s" price-id)))))

(defn- get-product-price
  "Query a product's price by its stripe-identifier, which is a string, e.g.
  `\"price_4242424242\"`."
  ([request]
   (get-product-price request retrieve-price))
  ([{{{:keys [price-id]} :query} :parameters} price-by-id]
   (if-let [price (price-by-id price-id)]
     (ok price)
     (not-found error-article-not-found))))

(comment

  (retrieve-price "price_1K9S66FrKCGqvoMokD1SoBic")
  (retrieve-price "price_1K9S66FrKCGqvoMokD1SoBica")

  (get-product-price
   {:parameters {:query {:price-id "foo"}}})

  nil)

;; -----------------------------------------------------------------------------

(def events (atom {}))
(def n2o-id "TODO: Delete me"
  "d6d8a351-2074-46ff-aa9b-9c57ab6c6a18")

(defmulti ^:private stripe-event
  "Dispatch incoming stripe events."
  (fn [event] (:type event)))

(defmethod stripe-event "customer.subscription.created" [event]
  '"This event is triggered when a new user creates a subscription on stripe. We
   extract all information from the event and store the relevant information in
   our database."
  (let [keycloak-id (get-in event [:data :object :metadata :keycloak-id])
        stripe-customer-id (get-in event [:data :object :customer])
        stripe-subscription-id (get-in event [:data :object :id])]
    (user-db/subscribe-pro-tier keycloak-id stripe-subscription-id stripe-customer-id))
  (log/info "Subscription successfully created 🤑"))

(defmethod stripe-event :default [event]
  (swap! events assoc (keyword (:type event)) event))

;; -----------------------------------------------------------------------------

(defn- verify-signature
  "Verify the signature of the incoming stripe-request."
  [request]
  (try
    (Webhook/constructEvent (:body request)
                            (get-in request [:headers "stripe-signature"])
                            config/stripe-webhook-access-key)
    {:passed? true}
    (catch SignatureVerificationException e
      {:passed? false
       :error :stripe.verification/invalid-signature
       :message (.getMessage e)})
    (catch Exception e
      {:passed? false
       :error :stripe.verification/error
       :message (.getMessage e)})))

(defn- verify-signature-middleware
  "Verify the signature of incoming stripe requests."
  [handler]
  (fn [request]
    (let [{:keys [passed? error message]} (verify-signature request)]
      (if passed?
        (handler request)
        (bad-request (at/build-error-body error message))))))

(>defn- keycloak-id->subscription
  "Retrieve current subscription status from stripe."
  [keycloak-id]
  [:user.registered/keycloak-id => (? ::subscription)]
  (try
    (Subscription/retrieve
     (:user.registered.subscription/stripe-id
      (user-db/private-user-by-keycloak-id keycloak-id)))
    (catch InvalidRequestException _e)))

(>defn- cancel-subscription
  "Toggle subscription. If `cancel?` is true, the subscription ends at the next 
  payment period. If it is false, the cancelled subscription is re-activated."
  [keycloak-id cancel?]
  [:user.registered/keycloak-id boolean? => ::subscription]
  (let [subscription (keycloak-id->subscription keycloak-id)
        parameters (-> (SubscriptionUpdateParams/builder)
                       (.setCancelAtPeriodEnd cancel?)
                       (.build))]
    (.update subscription parameters)))

(>defn- subscription->edn
  "Take the subscription and convert interesting information to EDN."
  [subscription]
  [::subscription => ::subscription-status]
  (let [cancelled? (.getCancelAtPeriodEnd subscription)]
    (cond->
     {:status (keyword (.getStatus subscription))
      :cancelled? (.getCancelAtPeriodEnd subscription)
      :period-start (.getCurrentPeriodStart subscription)
      :period-end (.getCurrentPeriodEnd subscription)}
      cancelled? (assoc :cancel-at (.getCancelAt subscription)
                        :cancelled-at (.getCanceledAt subscription)))))

;; -----------------------------------------------------------------------------

(defn- webhook
  "Handle incoming stripe requests. This function receives all events from 
  stripe and dispatches them further."
  [{:keys [body-params]}]
  (log/info "Event type:" (:type body-params))
  (stripe-event body-params)
  (ok {:message "Always return 200 to stripe."}))

(defn- cancel-user-subscription
  "Cancel a user's subscription"
  [{:keys [body-params identity]}]
  (ok (subscription->edn (cancel-subscription (:sub identity) (:cancel? body-params)))))

(defn- retrieve-subscription-status
  "Return the subscription-status."
  [{{:keys [sub]} :identity}]
  (if-let [subscription (keycloak-id->subscription sub)]
    (ok (subscription->edn subscription))
    (ok)))

;; -----------------------------------------------------------------------------

(def stripe-routes
  [["/stripe" {:swagger {:tags ["subscription" "stripe"]}}
    [""
     ["/create-checkout-session"
      {:post create-checkout-session
       :description (at/get-doc #'create-checkout-session)
       :parameters {:body {:price-id :stripe.price/id}}
       :responses {200 {:body {:redirect string?}}
                   400 at/response-error-body}
       :middleware [:user/authenticated?]}]
     ["/price"
      {:get get-product-price
       :description (at/get-doc #'get-product-price)
       :parameters {:query {:price-id :stripe.price/id}}
       :responses {200 {:body :stripe/price}
                   403 at/response-error-body
                   404 at/response-error-body}}]
     ["/subscription"
      ["/status" {:get retrieve-subscription-status
                  :description (at/get-doc #'retrieve-subscription-status)
                  :responses {200 {:body (s/or :subscription ::subscription-status
                                               :no-subscription nil?)}}}]
      ["/cancel" {:post cancel-user-subscription
                  :description (at/get-doc #'cancel-user-subscription)
                  :parameters {:body {:cancel? boolean?}}
                  :responses {200 {:body ::subscription-status}}}]]]
    ["/webhook"
     {:post webhook
      :middleware [verify-signature-middleware]
      :name :stripe/webhook
      :description (at/get-doc #'webhook)
      :responses {200 {:body {:message string?}}}}]]])

