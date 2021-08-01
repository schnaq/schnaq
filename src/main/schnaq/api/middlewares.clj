(ns schnaq.api.middlewares
  (:require [ring.util.http-response :refer [bad-request]]
            [schnaq.api.toolbelt :as at]
            [schnaq.validator :as validator]))

(defn valid-discussion?-middleware
  "Verify, that a valid share-hash was provided."
  [handler]
  (fn [request]
    (if-let [share-hash (or (get-in request [:parameters :body :share-hash])
                            (get-in request [:parameters :query :share-hash]))]
      (if (validator/valid-discussion? share-hash)
        (handler request)
        at/not-found-hash-invalid)
      (bad-request (at/response-error-body :share-hash/not-found "You must provide a valid share-hash.")))))




