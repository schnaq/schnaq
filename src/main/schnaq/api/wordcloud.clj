(ns schnaq.api.wordcloud
  (:require [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.visible-entity :as visible-entity]))

(defn- toggle-wordcloud
  "Toggle word cloud display for a schnaq."
  [{{{:keys [share-hash display-wordcloud?]} :body} :parameters}]
  (if display-wordcloud?
    (visible-entity/add-entity! share-hash :discussion.visible.entities/wordcloud)
    (visible-entity/retract-entity! share-hash :discussion.visible.entities/wordcloud))
  (ok {:display-wordcloud? display-wordcloud?}))

(def wordcloud-routes
  [["/wordcloud" {:swagger {:tags ["wordcloud"]}}
    ["/discussion" {:put toggle-wordcloud
                    :description (at/get-doc #'toggle-wordcloud)
                    :middleware [:user/authenticated?
                                 :user/pro-user?
                                 :discussion/valid-credentials?]
                    :name :wordcloud/display
                    :parameters {:body {:share-hash :discussion/share-hash
                                        :edit-hash :discussion/edit-hash
                                        :display-wordcloud? boolean?}}
                    :responses {200 {:body {:display-wordcloud? boolean?}}
                                400 at/response-error-body}}]]])
