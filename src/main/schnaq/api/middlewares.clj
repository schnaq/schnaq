(ns schnaq.api.middlewares
  (:require [reitit.ring.middleware.exception :as exception]
            [ring.util.http-response :refer [forbidden not-found]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.wordcloud :as wordcloud-db]
            [schnaq.validator :as validator]
            [taoensso.timbre :as log])
  (:import (java.util UUID)))

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
      (if (and (not shared-config/production?)
               (= share-hash shared-config/allowed-share-hash-in-development))
        (handler request)
        (if (validator/valid-discussion? share-hash)
          (handler request)
          at/not-found-hash-invalid)))))

(defn wordcloud-belongs-to-discussion
  "Check whether a wordcloud and share-hash match."
  [handler]
  (fn [request]
    (let [share-hash (extract-parameter-from-request request :share-hash)
          wordcloud-id (extract-parameter-from-request request :wordcloud-id)]
      (if (wordcloud-db/matching-wordcloud wordcloud-id share-hash)
        (handler request)
        (forbidden (at/build-error-body
                    :wordcloud-not-from-discussion
                    "The information you submitted did not match."))))))

(defn valid-open-discussion?
  "Verify that a discussion is valid and writing to it / modifying it is allowed."
  [handler]
  (fn [request]
    (let [share-hash (extract-parameter-from-request request :share-hash)]
      (if (validator/valid-open-discussion? share-hash)
        (handler request)
        (at/build-error-body :schnaq.error/discussion-invalid "You are not allowed to modify this discussion")))))

(defn posts-allowed?
  "Verify that a discussion allows posts."
  [handler]
  (fn [request]
    (let [share-hash (extract-parameter-from-request request :share-hash)]
      (if (validator/posts-allowed? share-hash)
        (handler request)
        (at/build-error-body :schnaq.error/discussion-invalid "You are not allowed to write any posts for this discussion")))))

(defn valid-statement?-middleware
  "Verify, that a valid share-hash was provided matching the statement-id."
  [handler]
  (fn [request]
    (let [share-hash (extract-parameter-from-request request :share-hash)
          statement-id (extract-parameter-from-request request :statement-id)]
      (if (validator/valid-discussion-and-statement? statement-id share-hash)
        (handler request)
        (not-found (at/build-error-body :statement/invalid "Invalid parameters provided."))))))

(defn valid-author?-middleware
  "Verify the requesting user being the author of the discussion."
  [handler]
  (fn [request]
    (let [author-keycloak-id (-> (extract-parameter-from-request request :share-hash)
                                 discussion-db/discussion-by-share-hash
                                 :discussion/author
                                 :user.registered/keycloak-id)]
      (if (= author-keycloak-id (-> request :user :user.registered/keycloak-id))
        (handler request)
        (forbidden (at/build-error-body :credentials/invalid "Only the author is allowed to modify the schnaq."))))))

(defn user-moderator?-middleware
  "Verify the requesting user being the author or moderator of the discussion."
  [handler]
  (fn [request]
    (let [share-hash (extract-parameter-from-request request :share-hash)
          user-id (:db/id (:user request))]
      (if (validator/user-moderator? share-hash user-id)
        (handler request)
        (forbidden (at/build-error-body :credentials/not-moderator
                                        "This operation is only allowed for moderators."))))))

(defn parent-unlocked?-middleware
  "Verify that the parent statement is unlocked and can be written to."
  [handler]
  (fn [request]
    (let [parent-id (extract-parameter-from-request request :conclusion-id)
          parent (when parent-id (fast-pull parent-id patterns/statement))]
      (if (:statement/locked? parent)
        (forbidden (at/build-error-body :statement/locked "This statement is locked and can not be reacted to."))
        (handler request)))))

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

(defn add-device-id
  "Extracts the device-id and saves it to the known ids of the queries schnaq"
  [handler]
  (fn [request]
    (let [device-id (get-in request [:headers "device-id"])
          share-hash (extract-parameter-from-request request :share-hash)]
      (when (and device-id share-hash)
        (discussion-db/add-device-id share-hash (UUID/fromString device-id))))
    (handler request)))
