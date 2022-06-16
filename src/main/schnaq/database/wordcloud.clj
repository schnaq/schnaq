(ns schnaq.database.wordcloud
  (:require [com.fulcrologic.guardrails.core :refer [=> >defn ?]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :as db :refer [transact]]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs]))

(>defn wordcloud-by-share-hash
  "Return wordcloud by a discussion's share-hash."
  [share-hash]
  [:discussion/share-hash => (? :discussion/wordcloud)]
  (db/query '[:find (pull ?wordcloud pattern) .
              :in $ ?share-hash pattern
              :where [?discussion :discussion/share-hash ?share-hash]
              [?discussion :discussion/wordcloud ?wordcloud]]
            share-hash patterns/wordcloud))

(>defn show-discussion-wordcloud
  "Change visibility of wordcloud."
  [share-hash show-wordcloud?]
  [:discussion/share-hash boolean? => (? map?)]
  (let [{:keys [db/id]} (wordcloud-by-share-hash share-hash)
        wordcloud-id (or id (format "new-wordcloud-%s" share-hash))]
    @(transact [{:db/id wordcloud-id
                 :wordcloud/visible? show-wordcloud?}])))
