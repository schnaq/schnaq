(ns schnaq.auth.middlewares
  (:require [buddy.auth :refer [authenticated?]]
            [clojure.string :as string]
            [ring.util.http-response :refer [unauthorized forbidden]]
            [schnaq.api.toolbelt :as at]
            [schnaq.auth.lib :as auth-lib]
            [schnaq.config :as config]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.user :as user-db]))

(defn- valid-app-code?
  "Check if an app-code was provided via the request-body."
  [request]
  (string? (some config/app-codes
                 #{(get-in request [:parameters :body :app-code])})))

;; -----------------------------------------------------------------------------

(defn authenticated?-middleware
  "Validate, that user is logged-in."
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
    (if (auth-lib/has-role? (:identity request) shared-config/admin-roles)
      (handler request)
      (forbidden (at/build-error-body :auth/not-an-admin "You are not an admin.")))))

(defn beta-tester?-middleware
  "Check if is eligible for our beta-testers program."
  [handler]
  (fn [request]
    (if (auth-lib/beta-tester? request)
      (handler request)
      (forbidden (at/build-error-body :auth/not-a-beta-tester "You are not a beta tester.")))))

(defn pro-user?-middleware
  "Validate, that user has a subscription in our database or is a beta user."
  [handler]
  (fn [request]
    (if (auth-lib/pro-user? (:identity request))
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

(defn update-jwt-middleware
  "Always update identity-map, if provided. Else just passes the request through."
  [handler]
  (fn [request]
    (if (authenticated? request)
      (handler (auth-lib/prepare-identity-map request))
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
