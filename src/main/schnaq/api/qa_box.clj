(ns schnaq.api.qa-box
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok forbidden]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.question-box :as qa-box-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.validator :as validators]
            [taoensso.timbre :as log]))

(defn- create-qa-box
  "Allow admins to create a qa-box"
  [{:keys [parameters]}]
  (let [{:keys [share-hash visible? label]} (:body parameters)]
    (ok {:qa-box (qa-box-db/create-qa-box! share-hash visible? label)})))

(defn get-qa-boxes
  "Get all qa-boxes for a discussion. If moderator, also return the invisible ones."
  [{:keys [parameters identity]}]
  (let [share-hash (get-in parameters [:path :share-hash])
        user-sub (:sub identity)
        user-id (when user-sub (:db/id (user-db/private-user-by-keycloak-id user-sub)))
        user-moderator? (true? (validators/user-moderator? share-hash user-id))]
    (ok {:qa-boxes (qa-box-db/qa-boxes-for-share-hash share-hash user-moderator?)})))

(defn- delete-qa-box
  "Delete a single qa-box from a discussion."
  [{:keys [parameters identity]}]
  (let [qa-box-id (get-in parameters [:path :qa-box-id])
        user-sub (:sub identity)
        user-id (:db/id (user-db/private-user-by-keycloak-id user-sub))]
    (if (qa-box-db/qa-box-moderator? user-id qa-box-id)
      (do
        (qa-box-db/delete-qa-box! qa-box-id)
        (ok {:db/id qa-box-id}))
      (forbidden (at/response-error-body :not-a-moderator "You are not allowed to delete this Q&A box.")))))

(defn- add-question
  "Add a question to a qa-box"
  [{:keys [parameters]}]
  (let [qa-box-id (get-in parameters [:path :qa-box-id])
        {:keys [question share-hash]} (:body parameters)]
    (if (qa-box-db/qa-box-id-matches-hash? qa-box-id share-hash)
      (ok {:new-question (qa-box-db/add-question qa-box-id question)})
      (forbidden (at/response-error-body :invalid-share-hash "The share-hash does not match the qa-box.")))))

(defn- upvote-question
  "Upvote a single question in a qa-box."
  [{:keys [parameters]}]
  (let [{:keys [question-id qa-box-id]} (:path parameters)
        {:keys [share-hash]} (:body parameters)]
    (if (qa-box-db/question-id-plausible? question-id qa-box-id share-hash)
      (ok {:upvoted? (qa-box-db/upvote-question question-id)})
      (forbidden (at/response-error-body :invalid-credentials "The ids and share-hash you provided do not match.")))))

(defn- update-qa-box-visibility
  "Toggle visibility of a qa-box."
  [{:keys [parameters identity]}]
  (let [qa-box-id (get-in parameters [:path :qa-box-id])
        user-sub (:sub identity)
        user-id (:db/id (user-db/private-user-by-keycloak-id user-sub))]
    (log/debug "Updating qa-box visibility for box" qa-box-id)
    (if (qa-box-db/qa-box-moderator? user-id qa-box-id)
      (ok {:updated-qa-box (qa-box-db/update-qa-box qa-box-id (:make-visible? (:body parameters)))})
      (forbidden (at/response-error-body :not-a-moderator "You are not allowed to modify this Q&A box.")))))

(defn- delete-question
  "Delete a single question in a qa-box."
  [{:keys [parameters]}]
  (let [{:keys [question-id qa-box-id]} (:path parameters)
        {:keys [share-hash]} (:body parameters)]
    (if (qa-box-db/question-id-plausible? question-id qa-box-id share-hash)
      (ok {:deleted? (map? @(qa-box-db/delete-question question-id))})
      (forbidden (at/response-error-body :invalid-credentials "The ids and share-hash you provided do not match.")))))

(defn- answer-question
  "Mark a single question as answered or unanswered in a qa-box."
  [{:keys [parameters]}]
  (let [{:keys [question-id qa-box-id]} (:path parameters)
        {:keys [share-hash answered]} (:body parameters)]
    (if (qa-box-db/question-id-plausible? question-id qa-box-id share-hash)
      (ok {:answered? (map? (qa-box-db/mark-question question-id answered))})
      (forbidden (at/response-error-body :invalid-credentials "The ids and share-hash you provided do not match.")))))

(def qa-box-routes
  [["" {:swagger {:tags ["qa-box"]}}
    ["/qa-box"
     ["" {:name :api.qa-box/create
          :post {:handler create-qa-box
                 :description (at/get-doc #'create-qa-box)
                 :middleware [:discussion/user-moderator? :user/pro? :discussion/valid-writeable-discussion?]
                 :parameters {:body {:share-hash :discussion/share-hash
                                     :visible? :qa-box/visible
                                     :label (s/nilable :qa-box/label)}}
                 :responses {200 {:body {:qa-box ::specs/qa-box}}
                             400 at/response-error-body
                             403 at/response-error-body}}}]
     ["/:qa-box-id"
      ["" {:name :api.qa-box/delete
           :delete {:handler delete-qa-box
                    :description (at/get-doc #'delete-qa-box)
                    :middleware [:discussion/user-moderator? :user/pro? :discussion/valid-writeable-discussion?]
                    :parameters {:path {:qa-box-id :db/id}
                                 :body {:share-hash :discussion/share-hash}}
                    :responses {200 {:body {:db/id :db/id}}
                                400 at/response-error-body
                                403 at/response-error-body}}}]
      ["/visibility" {:name :api.qa-box/visibility
                      :patch {:handler update-qa-box-visibility
                              :description (at/get-doc #'update-qa-box-visibility)
                              :middleware [:discussion/user-moderator? :user/pro? :discussion/valid-writeable-discussion?]
                              :parameters {:path {:qa-box-id :db/id}
                                           :body {:share-hash :discussion/share-hash
                                                  :make-visible? :qa-box/visible}}
                              :responses {200 {:body {:updated-qa-box ::specs/qa-box}}
                                          400 at/response-error-body
                                          403 at/response-error-body}}}]
      ["/question/:question-id"
       ["" {:name :api.qa-box.question.delete
            :delete {:handler delete-question
                     :description (at/get-doc #'delete-question)
                     :middleware [:discussion/user-moderator? :user/pro? :discussion/valid-writeable-discussion?]
                     :parameters {:path {:qa-box-id :db/id
                                         :question-id :db/id}
                                  :body {:share-hash :discussion/share-hash}}
                     :responses {200 {:body {:deleted? boolean?}}
                                 400 at/response-error-body
                                 403 at/response-error-body}}}]
       ["/answer" {:name :api.qa-box.question/answer
                   :post {:handler answer-question
                          :description (at/get-doc #'answer-question)
                          :middleware [:discussion/user-moderator? :user/pro? :discussion/valid-writeable-discussion?]
                          :parameters {:path {:qa-box-id :db/id
                                              :question-id :db/id}
                                       :body {:share-hash :discussion/share-hash
                                              :answered :qa-box.question/answered}}
                          :responses {200 {:body {:answered? boolean?}}
                                      400 at/response-error-body
                                      403 at/response-error-body}}}]
       ["/upvote" {:name :api.qa-box.question/upvote
                   :post {:handler upvote-question
                          :description (at/get-doc #'upvote-question)
                          :middleware [:discussion/valid-writeable-discussion?]
                          :parameters {:path {:qa-box-id :db/id
                                              :question-id :db/id}
                                       :body {:share-hash :discussion/share-hash}}
                          :responses {200 {:body {:upvoted? boolean?}}
                                      400 at/response-error-body
                                      403 at/response-error-body}}}]]
      ["/questions" {:name :api.qa-box.questions/add
                     :post {:handler add-question
                            :description (at/get-doc #'add-question)
                            :middleware [:discussion/valid-writeable-discussion?]
                            :parameters {:path {:qa-box-id :db/id}
                                         :body {:question :qa-box.question/value
                                                :share-hash :discussion/share-hash}}
                            :responses {200 {:body {:new-question :qa-box/question}}
                                        400 at/response-error-body}}}]]]
    ["/qa-boxes/:share-hash" {:name :api.qa-box/get-by-hash
                              :get {:handler get-qa-boxes
                                    :description (at/get-doc #'get-qa-boxes)
                                    :middleware [:discussion/valid-share-hash?]
                                    :parameters {:path {:share-hash :discussion/share-hash}}
                                    :responses {200 {:body {:qa-boxes (s/coll-of ::specs/qa-box)}}
                                                400 at/response-error-body
                                                403 at/response-error-body}}}]]])
