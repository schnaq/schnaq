(ns schnaq.api.feedback-form
  (:require
   [com.fulcrologic.guardrails.core :refer [=> >defn-]]
   [schnaq.api.toolbelt :as at]))

(>defn- create-form
  "Create a new feedback-form based on the items the user sends. Does not check amount of items."
  [{:keys [parameters]}]
  [:ring/request => :ring/response]
  (let [{:keys [share-hash items]} (:body parameters)]
    ;; TODO do the logic dance, bruv
    ))

(def feedback-form-routes
  ["/feedback"
   ["/form"
    ["" {:post create-form
         :description (at/get-doc #'create-form)
         :name :api.discussion.feedback/form
         :middleware [:discussion/valid-share-hash? :discussion/user-moderator?]
         :parameters {:query {:share-hash :discussion/share-hash
                              :items :feedback/items}}
         ;; TODO hier bei items muss eventuell ein dto ran, welches keywords umwandelt
         :responses {200 {:body {:feedback-form-id :db/id}}}}]]])
