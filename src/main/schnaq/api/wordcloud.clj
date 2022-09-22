(ns schnaq.api.wordcloud
  (:require
   [clojure.spec.alpha :as s]
   [ring.util.http-response :refer [ok]]
   [schnaq.api.toolbelt :as at]
   [schnaq.database.main :as db :refer [set-activation-focus fast-pull]]
   [schnaq.database.patterns :as patterns]
   [schnaq.database.specs :as specs]
   [schnaq.database.wordcloud :as wordcloud-db]))

(defn- toggle-wordcloud
  "Toggle word cloud display for a schnaq."
  [{{{:keys [share-hash]} :body} :parameters}]
  (wordcloud-db/toggle-wordcloud-visibility share-hash)
  (let [{:keys [db/id wordcloud/visible?] :as wordcloud} (wordcloud-db/wordcloud-by-share-hash share-hash)]
    (when visible?
      (set-activation-focus [:discussion/share-hash share-hash] id))
    (ok {:wordcloud wordcloud})))

(defn- create-local-wordcloud
  "Creates a local, interactive wordcloud, when posted to."
  [{{{:keys [share-hash title]} :body} :parameters}]
  (let [wordcloud (wordcloud-db/create-local-wordcloud share-hash title)]
    (set-activation-focus [:discussion/share-hash share-hash] (:db/id wordcloud))
    (ok {:wordcloud wordcloud})))

(defn- get-local-wordclouds
  "Returns all local wordclouds belonging to a schnaq room."
  [{{{:keys [share-hash]} :query} :parameters}]
  (ok {:wordclouds (wordcloud-db/local-wordclouds share-hash)}))

(defn- add-words-to-local-cloud
  "Adds a list of words to some local wordcloud."
  [{{{:keys [wordcloud-id words]} :body} :parameters}]
  (doseq [word words] (wordcloud-db/add-word-to-wordcloud wordcloud-id word))
  (ok {:wordcloud (fast-pull wordcloud-id patterns/local-wordcloud)}))

(defn- delete-local-wordcloud
  "Delete a single local wordcloud and all of its contents."
  [{{{:keys [wordcloud-id]} :body} :parameters}]
  (ok {:deleted? (not (nil? (db/delete-entity! wordcloud-id)))}))

(def wordcloud-routes
  [["/wordcloud" {:swagger {:tags ["wordcloud"]}}
    ["/discussion" {:put toggle-wordcloud
                    :description (at/get-doc #'toggle-wordcloud)
                    :middleware [:user/pro?
                                 :discussion/user-moderator?]
                    :name :wordcloud/display
                    :parameters {:body {:share-hash :discussion/share-hash}}
                    :responses {200 {:body {:wordcloud :discussion/wordcloud}}
                                400 at/response-error-body}}]
    ["/local"
     ["" {:name :wordcloud/local
          :post {:handler create-local-wordcloud
                 :description (at/get-doc #'create-local-wordcloud)
                 :middleware [:user/pro?
                              :discussion/user-moderator?]
                 :parameters {:body {:share-hash :discussion/share-hash
                                     :title :wordcloud/title}}
                 :responses {200 {:body {:wordcloud ::specs/wordcloud}}
                             400 at/response-error-body
                             403 at/response-error-body}}
          :get {:handler get-local-wordclouds
                :description (at/get-doc #'get-local-wordclouds)
                :middleware [:discussion/valid-share-hash?]
                :parameters {:query {:share-hash :discussion/share-hash}}
                :responses {200 {:body {:wordclouds (s/coll-of ::specs/wordcloud)}}
                            400 at/response-error-body}}
          :delete {:handler delete-local-wordcloud
                   :description (at/get-doc #'delete-local-wordcloud)
                   :middleware [:user/pro?
                                :discussion/user-moderator?
                                :discussion/wordcloud-matching?]
                   :parameters {:body {:share-hash :discussion/share-hash
                                       :wordcloud-id :db/id}}
                   :responses {200 {:body {:deleted? boolean?}}
                               403 at/response-error-body}}}]
     ["/words" {:name :wordcloud.local/words
                :put add-words-to-local-cloud
                :description (at/get-doc #'add-words-to-local-cloud)
                :middleware [:discussion/wordcloud-matching?]
                :parameters {:body {:wordcloud-id :db/id
                                    :share-hash :discussion/share-hash
                                    :words (s/coll-of ::specs/non-blank-string)}}
                :responses {200 {:body {:wordcloud ::specs/wordcloud}}
                            403 at/response-error-body}}]]]])
