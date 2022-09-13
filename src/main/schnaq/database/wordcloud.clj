(ns schnaq.database.wordcloud
  (:require [com.fulcrologic.guardrails.core :refer [=> >defn ?]]
            [schnaq.database.main :as db :refer [transact]]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs]))

(>defn wordcloud-by-share-hash
  "Return wordcloud by a discussion's share-hash."
  [share-hash]
  [:discussion/share-hash => (? :discussion/wordcloud)]
  (db/query '[:find (pull ?wordcloud pattern) .
              :in $ ?share-hash pattern
              :where [?discussion :discussion/share-hash ?share-hash]
              [?discussion :discussion/wordcloud ?wordcloud]]
            share-hash patterns/wordcloud))

(>defn toggle-wordcloud-visibility
  "Toggle visibility of wordcloud."
  [share-hash]
  [:discussion/share-hash => (? map?)]
  (if-let [{:keys [db/id wordcloud/visible?]} (wordcloud-by-share-hash share-hash)]
    @(transact [[:db/add id :wordcloud/visible? (not visible?)]])
    @(transact [{:discussion/share-hash share-hash
                 :discussion/wordcloud {:wordcloud/visible? true}}])))

(>defn create-local-wordcloud
  "Create a local wordcloud activation."
  [share-hash title]
  [:discussion/share-hash ::specs/non-blank-string => ::specs/wordcloud]
  (let [temp-id "temp"]
    (db/transact-and-pull-temp [{:db/id temp-id
                                 :wordcloud.local/title title
                                 :wordcloud.local/discussion [:discussion/share-hash share-hash]}]
                               temp-id
                               patterns/local-wordcloud)))
