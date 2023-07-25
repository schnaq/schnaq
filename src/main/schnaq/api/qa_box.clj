(ns schnaq.api.qa-box
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok forbidden]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.question-box :as qa-box-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]))

(defn create-qa-box
  "Allow admins to create a qa-box"
  [{:keys [parameters]}]
  (let [{:keys [share-hash visible? label]} (:body parameters)]
    (ok {:qa-box (qa-box-db/create-qa-box! share-hash visible? label)})))

(defn get-qa-boxes
  "Get all qa-boxes for a discussion"
  [{:keys [parameters]}]
  (let [share-hash (get-in parameters [:path :share-hash])]
    (ok {:qa-boxes (qa-box-db/qa-boxes-for-share-hash share-hash)})))

(defn delete-qa-box
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
     ["/:qa-box-id" {:name :api.qa-box/delete
                     :delete {:handler delete-qa-box
                              :description (at/get-doc #'delete-qa-box)
                              :middleware [:discussion/user-moderator? :user/pro? :discussion/valid-writeable-discussion?]
                              :parameters {:path {:qa-box-id :db/id}
                                           :body {:share-hash :discussion/share-hash}}
                              :responses {200 {:body {:db/id :db/id}}
                                          400 at/response-error-body
                                          403 at/response-error-body}}}]]
    ["/qa-boxes/:share-hash" {:name :api.qa-box/get-by-hash
                              :get {:handler get-qa-boxes
                                    :description (at/get-doc #'get-qa-boxes)
                                    :middleware [:discussion/valid-share-hash?]
                                    :parameters {:path {:share-hash :discussion/share-hash}}
                                    :responses {200 {:body {:qa-boxes (s/coll-of ::specs/qa-box)}}
                                                400 at/response-error-body
                                                403 at/response-error-body}}}]]])
