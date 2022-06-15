(ns schnaq.database.wordcloud
  (:require [com.fulcrologic.guardrails.core :refer [=> >defn ?]]
            [schnaq.database.main :as db :refer [fast-pull transact]]
            [schnaq.database.specs]))

(>defn show-discussion-wordcloud
  "Change visibility of wordcloud."
  [share-hash show-wordcloud?]
  [:discussion/share-hash boolean? => future?]
  (transact [{:discussion/share-hash share-hash
              :discussion/wordcloud {:wordcloud/visible? show-wordcloud?}}]))

(>defn wordcloud-by-share-hash
  "Return wordcloud by a discussion's share-hash."
  [share-hash]
  [:discussion/share-hash => (? :discussion/wordcloud)]
  (:discussion/wordcloud (fast-pull [:discussion/share-hash share-hash])))
