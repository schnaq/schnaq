(ns schnaq.api.wordcloud
  (:require [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.main :refer [set-activation-focus]]
            [schnaq.database.wordcloud :as wordcloud-db]))

(defn- toggle-wordcloud
  "Toggle word cloud display for a schnaq."
  [{{{:keys [share-hash display-wordcloud?]} :body} :parameters}]
  (wordcloud-db/show-discussion-wordcloud share-hash display-wordcloud?)
  (when display-wordcloud?
    (let [wordcloud-id (:db/id (wordcloud-db/wordcloud-by-share-hash share-hash))]
      (set-activation-focus [:discussion/share-hash share-hash] wordcloud-id)))
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
