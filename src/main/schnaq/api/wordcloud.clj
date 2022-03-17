(ns schnaq.api.wordcloud
  (:require [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [taoensso.timbre :as log]))

(defn- display-word-cloud
  "Toggle word cloud display for a schnaq."
  [{{{:keys [share-hash display-wordcloud?]} :body} :parameters}]
  (log/info "Toggle wordcloud: " display-wordcloud? share-hash)
  (ok {:display-wordcloud? display-wordcloud?}))

(def wordcloud-routes
  [["/wordcloud" {:swagger {:tags ["wordcloud"]}}
    ["/discussion" {:put display-word-cloud
                    :description (at/get-doc #'display-word-cloud)
                    :middleware [:user/authenticated?
                                 :user/pro-user?
                                 :discussion/valid-credentials?]
                    :name :wordcloud/display
                    :parameters {:body {:share-hash :discussion/share-hash
                                        :edit-hash :discussion/edit-hash
                                        :display-wordcloud? boolean?}}
                    :responses {200 {:body {:display-wordcloud? boolean?}}
                                400 at/response-error-body}}]]])