(ns schnaq.api.qa-box
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [bad-request ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.question-box :as qa-box-db]
            [schnaq.database.specs :as specs]))

(defn create-qa-box
  "Allow admins to create a qa-box"
  [{:keys [parameters]}]
  (let [{:keys [share-hash visible? label]} (:body parameters)]
    (ok {:qa-box (qa-box-db/create-qa-box! share-hash visible? label)})))


(def qa-box-routes
  [["" {:swagger {:tags ["qa-box"]}}
    ["/qa-box" {:name :api/qa-box
                :post {:handler create-qa-box
                       :description (at/get-doc #'create-qa-box)
                       :middleware [:discussion/user-moderator? :user/pro? :discussion/valid-writeable-discussion?]
                       :parameters {:body {:share-hash :discussion/share-hash
                                           :visible? :qa-box/visible
                                           :label (s/nilable :qa-box/label)}}
                       :responses {200 {:body {:qa-box ::specs/qa-box}}
                                   400 at/response-error-body
                                   403 at/response-error-body}}}]]])
