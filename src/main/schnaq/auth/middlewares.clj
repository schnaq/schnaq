(ns schnaq.auth.middlewares
  (:require [buddy.auth :refer [authenticated?]]
            [clojure.string :as string]
            [com.fulcrologic.guardrails.core :refer [>defn-]]
            [ring.util.http-response :refer [forbidden unauthorized]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.database.user :as user-db]
            [schnaq.shared-toolbelt :as shared-tools]))

(defn- valid-app-code?
  "Check if an app-code was provided via the request-body."
  [request]
  (string? (some config/app-codes
                 #{(get-in request [:parameters :body :app-code])})))

;; -----------------------------------------------------------------------------

(defn authenticated?-middleware
  "Validate, that user is logged-in. Lookup this user from database if available."
  [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      (unauthorized (at/build-error-body :auth/not-logged-in
                                         "You are not logged in. Maybe your token is malformed / expired.")))))

(defn admin?-middleware
  "Check if user has admin-role."
  [handler]
  (fn [request]
    (if (shared-tools/admin? (:user.registered/roles (:user request)))
      (handler request)
      (forbidden (at/build-error-body :auth/not-an-admin "You are not an admin.")))))

(defn analytics-admin?-middleware
  "Check if user has analytics-admin or higher role."
  [handler]
  (fn [request]
    (if (shared-tools/analytics-admin? (:user.registered/roles (:user request)))
      (handler request)
      (forbidden (at/build-error-body :auth/not-an-admin "You are not allowed to see analytics.")))))

(defn beta-tester?-middleware
  "Check if is eligible for our beta-testers program."
  [handler]
  (fn [request]
    (if (shared-tools/beta-tester? (:user.registered/roles (:user request)))
      (handler request)
      (forbidden (at/build-error-body :auth/not-a-beta-tester "You are not a beta tester.")))))

(defn pro-user?-middleware
  "Validate, that user has a subscription in our database or is a beta user."
  [handler]
  (fn [request]
    (if (shared-tools/pro-user? (:user.registered/roles (:user request)))
      (handler request)
      (forbidden (at/build-error-body :auth/no-pro-subscription "You have no valid pro-subscription.")))))

(defn valid-app-code?-middleware
  "Validate the app code provided by the application. Only registered
  microservices should be allowed to post data to our servers."
  [handler]
  (fn [request]
    (if (valid-app-code? request)
      (handler request)
      (forbidden (at/build-error-body :app/invalid-code "Your application has no permission to access this API. Please provide a valid app-code in your request.")))))

;; -----------------------------------------------------------------------------

(>defn- extract-user-information-from-jwt
  "Extend identity map parsed from JWT and convert types."
  [request]
  [map? => map?]
  (-> request
      (assoc :user (user-db/private-user-by-keycloak-id (get-in request [:identity :sub])))
      (update-in [:identity :sub] str)
      (assoc-in [:identity :id] (str (get-in request [:identity :sub])))
      (assoc-in [:identity :preferred_username] (or (get-in request [:identity :preferred_username])
                                                    (get-in request [:identity :name])))
      (assoc-in [:identity :roles] (get-in request [:identity :realm_access :roles]))))

(defn parse-jwt-middleware
  "Always update identity-map, if provided. Else just passes the request through."
  [handler]
  (fn [request]
    (if (authenticated? request)
      (handler (extract-user-information-from-jwt request))
      (handler request))))

(defn replace-bearer-with-token
  "Most tools send the an authorization header as \"Bearer <token>\", but buddy
  wants it as \"Token <token>\". This middleware transforms the request, if a
  JWT is sent in the header."
  [handler]
  (fn [request]
    (if-let [bearer-token (get-in request [:headers "authorization"])]
      (let [[_bearer token] (string/split bearer-token #" ")]
        (handler (assoc-in request [:headers "authorization"] (format "Token %s" token))))
      (handler request))))
