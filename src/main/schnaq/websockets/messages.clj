(ns schnaq.websockets.messages
  (:require [schnaq.api.activation :as activation-api]
            [schnaq.api.discussion :as discussion-api]
            [schnaq.api.poll :as poll-api]
            [schnaq.database.specs]
            [schnaq.database.visible-entity :as visible-entity]
            [schnaq.shared-toolbelt :as shared-tools]
            [schnaq.websockets.handler :refer [handle-message]]))

(defmethod handle-message :discussion.starting/update [{:keys [?data]}]
  (when ?data
    (let [parameters {:parameters {:query ?data}}
          {{:keys [starting-conclusions]} :body} (discussion-api/get-starting-conclusions parameters)
          {{:keys [polls]} :body} (poll-api/polls-for-discussion parameters)
          {{:keys [activation]} :body} (activation-api/get-activation parameters)]
      (shared-tools/remove-nil-values-from-map
       {:starting-conclusions starting-conclusions
        :polls polls
        :activation activation
        :visible-entities (visible-entity/get-entities (:share-hash ?data))}))))

(defmethod handle-message :discussion.graph/update [{:keys [?data]}]
  (when ?data
    (let [parameters {:parameters {:query ?data}}
          {{:keys [graph]} :body} (discussion-api/graph-for-discussion parameters)]
      {:graph graph})))

(defmethod handle-message :schnaq.poll/update [{:keys [?data]}]
  (when ?data
    (let [parameters {:parameters {:query ?data}}
          {{:keys [poll]} :body} (poll-api/get-poll parameters)]
      {:poll poll})))