(ns schnaq.database.wordcloud
  (:require [com.fulcrologic.guardrails.core :refer [>defn >defn- =>]]
            [schnaq.database.main :as db :refer [transact]]))

(>defn- toggle-discussion-wordcloud
  "Change visibility of wordcloud."
  [share-hash toggle]
  [:discussion/share-hash boolean? => future?]
  (transact [{:discussion/share-hash share-hash
              :discussion/wordcloud {:wordcloud/visible? toggle}}]))

(>defn show-discussion-wordcloud
  "Activate wordcloud for a discussion."
  [share-hash]
  [:discussion/share-hash :ret future?]
  (toggle-discussion-wordcloud share-hash true))

(>defn hide-discussion-wordcloud
  "Remove wordcloud from discussion."
  [share-hash]
  [:discussion/share-hash :ret future?]
  (toggle-discussion-wordcloud share-hash false))
