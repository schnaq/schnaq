(ns schnaq.api.feedback-form
  (:require
   [com.fulcrologic.guardrails.core :refer [=> >defn-]]
   [ring.util.http-response :refer [ok bad-request]]
   [schnaq.api.toolbelt :as at]
   [schnaq.database.feedback-form :as feedback-db]))

(>defn- create-form
  "Create a new feedback-form based on the items the user sends. Does not check amount of items."
  [{:keys [parameters]}]
  [:ring/request => :ring/response]
  (let [{:keys [share-hash items]} (:body parameters)
        new-feedback-id (feedback-db/new-feedback-form! share-hash items)]
    (if new-feedback-id
      (ok {:feedback-form-id new-feedback-id})
      (bad-request (at/build-error-body :missing-items "Please provide items for the form")))))

(>defn- update-items
  "Set a new collection of items on a feedback form."
  [{:keys [parameters]}]
  [:ring/request => :ring/response]
  (let [{:keys [share-hash items]} (:body parameters)
        new-feedback-id (feedback-db/update-feedback-form-items! share-hash items)]
    (if new-feedback-id
      (ok {:updated-form? true})
      (bad-request (at/build-error-body :malformed-update "No feedback created or empty items."))))

  )

(def feedback-form-routes
  ["/feedback"
   ["/form"
    ["" {:post {:handler create-form
                :description (at/get-doc #'create-form)
                :responses {200 {:body {:feedback-form-id :db/id}}
                            400 at/response-error-body}}
         :put {:handler update-items
               :description (at/get-doc #'update-items)
               :responses {200 {:body {:updated-form? boolean?}}
                           400 at/response-error-body}}
         :name :api.discussion.feedback/form
         :middleware [:discussion/valid-share-hash? :discussion/user-moderator?]
         :parameters {:body {:share-hash :discussion/share-hash
                             :items :feedback/items}}}]]])
