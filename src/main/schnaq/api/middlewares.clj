(ns schnaq.api.middlewares
  (:require [ring.util.http-response :refer [forbidden not-found]]
            [schnaq.api.toolbelt :as at]
            [schnaq.validator :as validator]))

(defn- extract-parameter-from-request
  "Look up parameter in request and return its value."
  [request parameter]
  (or (get-in request [:parameters :body parameter])
      (get-in request [:parameters :query parameter])
      (get-in request [:parameters :path parameter])))

(defn valid-discussion?-middleware
  "Verify, that a valid share-hash was provided."
  [handler]
  (fn [request]
    (let [share-hash (extract-parameter-from-request request :share-hash)]
      (if (validator/valid-discussion? share-hash)
        (handler request)
        at/not-found-hash-invalid))))

(defn valid-statement?-middleware
  "Verify, that a valid share-hash was provided matching the statement-id."
  [handler]
  (fn [request]
    (let [share-hash (extract-parameter-from-request request :share-hash)
          statement-id (extract-parameter-from-request request :statement-id)]
      (if (validator/valid-discussion-and-statement? statement-id share-hash)
        (handler request)
        (not-found (at/build-error-body :statement/invalid "Invalid parameters provided."))))))


(defn valid-credentials?-middleware
  "Verify valid share-hash and edit-hash via middleware."
  [handler]
  (fn [request]
    (let [share-hash (extract-parameter-from-request request :share-hash)
          edit-hash (extract-parameter-from-request request :edit-hash)]
      (if (validator/valid-credentials? share-hash edit-hash)
        (handler request)
        (forbidden (at/build-error-body :credentials/invalid "Your share-hash and edit-hash do fit together."))))))

(defn wrap-custom-schnaq-csrf-header
  "A handler, that checks for a custom schnaq-csrf header. This can only be present when sent from an allowed origin
  via XMLHttpRequest."
  [handler]
  (fn [request]
    ;; Only relevant for those three methods
    (if (#{:post :put :delete} (:request-method request))
      ;; X-schnaq-csrf header must be present, otherwise raise error
      (if (get-in request [:headers "x-schnaq-csrf"])
        (handler request)
        (forbidden (at/build-error-body
                     :csrf/missing-header
                     "You are trying to access the route without the proper headers: \"x-schnaq-csrf\"")))
      (handler request))))