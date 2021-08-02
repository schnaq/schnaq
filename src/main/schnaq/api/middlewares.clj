(ns schnaq.api.middlewares
  (:require [ring.util.http-response :refer [forbidden]]
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

(defn valid-credentials?-middleware
  "Verify valid share-hash and edit-hash via middleware."
  [handler]
  (fn [request]
    (let [share-hash (extract-parameter-from-request request :share-hash)
          edit-hash (extract-parameter-from-request request :edit-hash)]
      (if (validator/valid-credentials? share-hash edit-hash)
        (handler request)
        (forbidden (at/build-error-body :credentials/invalid "Your share-hash and edit-hash do fit together."))))))


