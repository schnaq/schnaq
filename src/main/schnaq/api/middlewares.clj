(ns schnaq.api.middlewares
  (:require [reitit.ring.middleware.exception :as exception]
            [ring.util.http-response :refer [bad-request forbidden not-found]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.shared-toolbelt :as shared-tools]
            [schnaq.validator :as validator]
            [taoensso.timbre :as log]
            [taoensso.tufte :as tufte]))

(defn extract-parameter-from-request
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
      (if (and share-hash edit-hash)
        (if (validator/valid-credentials? share-hash edit-hash)
          (handler request)
          (forbidden (at/build-error-body :credentials/invalid "Your share-hash and edit-hash do not fit together.")))
        (bad-request (at/build-error-body :parameters/missing (format "Share-hash oder edit-hash is missing, share-hash: %s, edit-hash: %s" share-hash edit-hash)))))))

(defn wrap-custom-schnaq-csrf-header
  "A handler, that checks for a custom schnaq-csrf header. This can only be present when sent from an allowed origin
  via XMLHttpRequest. Allows whitelists for incoming requests, e.g. from the Stripe API."
  [handler]
  (fn [request]
    ;; Only relevant for those three verbs
    (if (#{:post :put :delete} (:request-method request))
      (let [route-name (get-in request [:reitit.core/match :data :name])]
        ;; Either the route is whitelisted, or there must be a x-schnaq-csrf header.
        (if (or (and route-name (route-name config/routes-without-csrf-check))
                (get-in request [:headers "x-schnaq-csrf"]))
          (handler request)
          (forbidden (at/build-error-body
                      :csrf/missing-header
                      "You are trying to access the route without the proper headers: \"x-schnaq-csrf\""))))
      (handler request))))

(defn convert-body-middleware
  "Convert InputStream to String to have the \"raw\"-request available for
  further processing.
  Must be called before other functions read the body's input stream!"
  [handler]
  (fn [{:keys [body] :as request}]
    (if body
      (handler (assoc request :body (slurp (:body request))))
      (handler request))))

;; -----------------------------------------------------------------------------
;; Error handling

;; type hierarchy
(derive ::error ::exception)
(derive ::failure ::exception)
(derive ::horror ::exception)

(defn- error-handler-with-stacktrace-printing
  "Response to be returned to the client."
  [message exception request]
  {:status 500
   :body {:message message
          :data (ex-data exception)
          :uri (:uri request)}})

(def exception-printing-middleware
  "Ring middleware to print stacktrace to stdout and return a valid response to
  the client."
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {;; ex-data with :type ::error
     ::error (partial error-handler-with-stacktrace-printing "error")
     ;; ex-data with ::exception or ::failure
     ::exception (partial error-handler-with-stacktrace-printing "exception")
     ;; override the default handler
     ::exception/default (partial error-handler-with-stacktrace-printing "default")
     ;; print stack-traces for all exceptions
     ::exception/wrap (fn [handler e request]
                        (log/error "ERROR" (pr-str (:uri request)))
                        (.printStackTrace e)
                        (handler e request))})))
