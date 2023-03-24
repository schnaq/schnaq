(ns schnaq.api.feedback-form
  (:require
   [com.fulcrologic.guardrails.core :refer [=> >defn-]]
   [ring.util.http-response :refer [ok bad-request]]
   [schnaq.api.toolbelt :as at]
   [schnaq.database.feedback-form :as feedback-db]
   [schnaq.database.main :as db]
   [schnaq.database.specs :as specs]))

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
  "Set a new collection of items on a feedback form. Any item that has an id is edited. All others are dropped."
  [{:keys [parameters]}]
  [:ring/request => :ring/response]
  (let [{:keys [share-hash items visible?]} (:body parameters)
        new-feedback-id (feedback-db/update-feedback-form-items! share-hash items visible?)]
    (if new-feedback-id
      (ok {:updated-form? true})
      (bad-request (at/build-error-body :malformed-update "No feedback created or empty items.")))))

(>defn- delete-feedback
  "Deletes the feedback-form including items and answers."
  [{:keys [parameters]}]
  [:ring/request => :ring/response]
  (let [{:keys [share-hash]} (:body parameters)
        deleted? (boolean (feedback-db/delete-feedback! share-hash))]
    (ok {:deleted? deleted?})))

(>defn- feedback-form
  "Returns the current feedback-items for a schnaq. (Do not show answers, this is for the user)."
  [{:keys [parameters]}]
  [:ring/request => :ring/response]
  (let [{:keys [share-hash]} (:body parameters)]
    (ok {:feedback-items (feedback-db/feedback-items share-hash)})))

(>defn- answer-feedback
  "Return some answers to feedback questions anonymously."
  [{:keys [parameters]}]
  [:ring/request => :ring/response]
  (let [{:keys [share-hash answers]} (:body parameters)
        item-existing? #(db/query '[:find ?id .
                                    :in $ ?id
                                    :where [?id :feedback.item/type _]]
                                  %)
        verified-answers (filter #(item-existing? (:feedback.answer/item %)) answers)]
    (if (empty? verified-answers)
      (bad-request (at/build-error-body :no-answers "Add any valid answers, before you submit."))
      (ok {:saved? (feedback-db/add-answers share-hash verified-answers)}))))

(>defn- get-results
  "Returns the feedback with answers (and the items embedded in them)."
  [{:keys [parameters]}]
  [:ring/request => :ring/response]
  (let [{:keys [share-hash]} parameters]
    (ok {:feedback-form (feedback-db/feedback-form-complete share-hash)})))

(def feedback-form-routes
  ["/feedback" {:middleware [:discussion/valid-share-hash?]}
   ["" {:name :api.discussion/feedback
        :get {:handler feedback-form
              :description (at/get-doc #'feedback-form)
              :parameters {:query {:share-hash :discussion/share-hash}}
              :responses {200 {:body {:feedback-items :feedback/items}}}}
        :post {:handler answer-feedback
               :description (at/get-doc #'answer-feedback)
               :parameters {:body {:share-hash :discussion/share-hash
                                   :answers :feedback/answers}}
               :responses {200 {:body {:saved? boolean?}}
                           400 at/response-error-body}}}]
   ["/form" {:name :api.discussion.feedback/form
             :middleware [:discussion/user-moderator?]
             :post {:handler create-form
                    :description (at/get-doc #'create-form)
                    :responses {200 {:body {:feedback-form-id :db/id}}
                                400 at/response-error-body}
                    :parameters {:body {:share-hash :discussion/share-hash
                                        :items :feedback/items}}}
             :put {:handler update-items
                   :description (at/get-doc #'update-items)
                   :responses {200 {:body {:updated-form? boolean?}}
                               400 at/response-error-body}
                   :parameters {:body {:share-hash :discussion/share-hash
                                       :items :feedback/items
                                       :visible? boolean?}}}
             :delete {:handler delete-feedback
                      :description (at/get-doc #'delete-feedback)
                      :parameters {:body {:share-hash :discussion/share-hash}}
                      :responses {200 {:body {:deleted? boolean?}}}}}]
   ["/results" {:name :api.discussion.feedback/results
                :middleware [:discussion/user-moderator?]
                :get get-results
                :description (at/get-doc #'get-results)
                :parameters {:query {:share-hash :discussion/share-hash}}
                :responses {200 {:body {:feedback-form ::specs/feedback-form}}}}]])
