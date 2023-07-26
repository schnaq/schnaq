(ns schnaq.websockets.messages
  (:require [schnaq.api.activation :as activation-api]
            [schnaq.api.discussion :as discussion-api]
            [schnaq.api.poll :as poll-api]
            [schnaq.api.qa-box :as qa-box-api]
            [schnaq.auth.jwt :as jwt]
            [schnaq.config.keycloak :as kc]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.specs]
            [schnaq.database.wordcloud :as wordcloud-db]
            [schnaq.shared-toolbelt :as shared-tools]
            [schnaq.websockets.handler :refer [handle-message]]))

(defmethod handle-message :discussion.starting/update [{:keys [?data]}]
  (when ?data
    (let [request {:parameters {:query ?data}
                   :identity (when-let [jwt (get ?data :jwt)]
                               (jwt/validate-signed-jwt jwt kc/keycloak-public-key))}
          path-request {:parameters {:path ?data}
                        :identity (when-let [jwt (get ?data :jwt)]
                                    (jwt/validate-signed-jwt jwt kc/keycloak-public-key))}
          {{:keys [starting-conclusions children]} :body} (discussion-api/get-starting-conclusions request)
          {{:keys [polls]} :body} (poll-api/polls-for-discussion request)
          {{:keys [activation]} :body} (activation-api/get-activation request)
          {{:keys [qa-boxes]} :body} (qa-box-api/get-qa-boxes path-request)
          share-hash (:share-hash ?data)
          wordcloud (wordcloud-db/wordcloud-by-share-hash share-hash)]
      (shared-tools/remove-nil-values-from-map
       {:starting-conclusions starting-conclusions
        :children children
        :polls polls
        :qa-boxes qa-boxes
        :activation activation
        :wordcloud wordcloud
        ;; below are the local wordclouds
        :wordclouds (wordcloud-db/local-wordclouds share-hash)
        :activation-focus (get-in
                           (fast-pull [:discussion/share-hash share-hash] '[:discussion/activation-focus])
                           [:discussion/activation-focus :db/id])}))))

(defmethod handle-message :discussion.activation/update [{:keys [?data]}]
  (when ?data
    (shared-tools/remove-nil-values-from-map
     (activation-api/get-activation {:parameters {:query ?data}}))))

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
