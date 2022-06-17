(ns schnaq.api.wordcloud
  (:require [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.main :refer [set-activation-focus]]
            [schnaq.database.wordcloud :as wordcloud-db]))

(defn- toggle-wordcloud
  "Toggle word cloud display for a schnaq."
  [{{{:keys [share-hash]} :body} :parameters}]
  (wordcloud-db/toggle-wordcloud-visibility share-hash)
  (let [{:keys [db/id wordcloud/visible?] :as wordcloud} (wordcloud-db/wordcloud-by-share-hash share-hash)]
    (when visible?
      (set-activation-focus [:discussion/share-hash share-hash] id))
    (ok {:wordcloud wordcloud})))

(def wordcloud-routes
  [["/wordcloud" {:swagger {:tags ["wordcloud"]}}
    ["/discussion" {:put toggle-wordcloud
                    :description (at/get-doc #'toggle-wordcloud)
                    :middleware [:user/authenticated?
                                 :user/pro-user?
                                 :discussion/valid-credentials?]
                    :name :wordcloud/display
                    :parameters {:body {:share-hash :discussion/share-hash
                                        :edit-hash :discussion/edit-hash}}
                    :responses {200 {:body {:wordcloud :discussion/wordcloud}}
                                400 at/response-error-body}}]]])
