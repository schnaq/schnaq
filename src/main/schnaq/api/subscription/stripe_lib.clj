(ns schnaq.api.subscription.stripe-lib
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn => ?]]
            [ring.util.http-response :refer [bad-request]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.database.user :as user-db]
            [taoensso.timbre :as log])
  (:import [com.stripe.exception InvalidRequestException SignatureVerificationException]
           [com.stripe.model Price Product Subscription]
           [com.stripe.net Webhook]
           [com.stripe.param SubscriptionUpdateParams]))

(s/def ::subscription-obj (partial instance? Subscription))

(>defn retrieve-price
  "Query current price via stripe's api."
  [price-id]
  [:stripe.price/id => (? :stripe/price)]
  (try
    (let [price (Price/retrieve price-id)]
      (if (.getActive price)
        {:id price-id
         :cost (-> price .getUnitAmount (/ 100) float)}
        (do (log/error "Queried price is not active:" price-id)
            (at/build-error-body
             :stripe.price/inactive
             (format "Queried price is not active: %s" price-id)))))
    (catch InvalidRequestException _
      (log/error "Price could not be found:" price-id)
      (at/build-error-body
       :stripe.price/invalid-request
       (format "Request could not be fulfilled. Maybe the queried price is not available: %s" price-id)))))

(>defn keycloak-id->subscription
  "Retrieve current subscription status from stripe."
  [keycloak-id]
  [:user.registered/keycloak-id => (? ::subscription-obj)]
  (try
    (Subscription/retrieve
     (:user.registered.subscription/stripe-id
      (user-db/private-user-by-keycloak-id keycloak-id)))
    (catch InvalidRequestException _e)))

(>defn subscription->edn
  "Take the subscription and convert interesting information to EDN."
  [subscription]
  [::subscription-obj => (? :stripe/subscription)]
  (when subscription
    (let [cancelled? (.getCancelAtPeriodEnd subscription)]
      (cond->
       {:status (keyword (.getStatus subscription))
        :cancelled? cancelled?
        :period-start (.getCurrentPeriodStart subscription)
        :period-end (.getCurrentPeriodEnd subscription)}
        cancelled? (assoc :cancel-at (.getCancelAt subscription)
                          :cancelled-at (.getCanceledAt subscription))))))

(>defn cancel-subscription!
  "Toggle subscription. If `cancel?` is true, the subscription ends at the next 
  payment period. If it is false, the cancelled subscription is re-activated."
  [keycloak-id cancel?]
  [:user.registered/keycloak-id boolean? => (? ::subscription-obj)]
  (when-let [subscription (keycloak-id->subscription keycloak-id)]
    (let [parameters (-> (SubscriptionUpdateParams/builder)
                         (.setCancelAtPeriodEnd cancel?)
                         (.build))]
      (.update subscription parameters))))

;; -----------------------------------------------------------------------------

(defn- verify-signature
  "Verify the signature of the incoming stripe request."
  ([request]
   (verify-signature request #(Webhook/constructEvent %1 %2 %3)))
  ([request construct-event]
   (try
     (construct-event (:body request)
                      (get-in request [:headers "stripe-signature"])
                      config/stripe-webhook-access-key)
     {:passed? true}
     (catch SignatureVerificationException e
       (assoc (at/build-error-body :stripe.verification/invalid-signature (.getMessage e))
              :passed? false))
     (catch Exception e
       (assoc (at/build-error-body :stripe.verification/error (.getMessage e))
              :passed? false)))))

(defn verify-signature-middleware
  "Verify the signature of incoming stripe requests."
  [handler]
  (fn [request]
    (let [{:keys [passed? error message]} (verify-signature request)]
      (if passed?
        (handler request)
        (bad-request (at/build-error-body error message))))))
