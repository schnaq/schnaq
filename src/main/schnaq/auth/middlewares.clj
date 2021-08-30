(ns schnaq.auth.middlewares
  (:require [buddy.auth :refer [authenticated?]]
            [ghostwheel.core :refer [>defn >defn-]]
            [ring.util.http-response :refer [unauthorized forbidden]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.config.shared :as shared-config]))

(>defn- has-role?
  "Check if user has realm-wide admin access."
  [request roles]
  [map? coll? :ret boolean?]
  (string? (some roles
                 (get-in request [:identity :roles]))))

(defn valid-app-code?
  "Check if an app-code was provided via the request-body."
  [request]
  (string? (some config/app-codes
                 #{(get-in request [:parameters :body :app-code])})))

(defn authenticated?-middleware
  "Validate, that user is logged-in."
  [handler]
  (fn [request]
    (if (authenticated? request)
      (-> request
          (assoc-in [:identity :id] (get-in request [:identity :sub]))
          (assoc-in [:identity :roles] (get-in request [:identity :realm_access :roles]))
          (assoc-in [:identity :admin?] (has-role? request shared-config/admin-roles))
          handler)
      (unauthorized (at/build-error-body :auth/not-logged-in
                                         "You are not logged in. Maybe your token is malformed / expired.")))))

(defn admin?-middleware
  "Check if user has admin-role."
  [handler]
  (fn [request]
    (if (has-role? request shared-config/admin-roles)
      (handler request)
      (forbidden (at/build-error-body :auth/not-an-admin "You are not an admin.")))))

(defn beta-tester?-middleware
  "Check if is eligible for our beta-testers program."
  [handler]
  (fn [request]
    (if (has-role? request shared-config/beta-tester-roles)
      (handler request)
      (forbidden (at/build-error-body :auth/not-a-beta-tester "You are not a beta tester.")))))

(defn valid-app-code?-middleware
  "Validate the app code provided by the application. Only registered
  microservices should be allowed to post data to our servers."
  [handler]
  (fn [request]
    (if (valid-app-code? request)
      (handler request)
      (forbidden (at/build-error-body :app/invalid-code "Your application has no permission to access this API. Please provide a valid app-code in your request.")))))

(>defn member-of-group?
  "Check if group is available in the JWT token."
  [identity group]
  [map? string? :ret boolean?]
  (some #(= group %) (:groups identity)))
