(ns schnaq.database.wordcloud
  (:require
   [clojure.spec.alpha :as s]
   [com.fulcrologic.guardrails.core :refer [=> >defn ?]]
   [schnaq.database.main :as db :refer [transact query]]
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

(>defn local-wordclouds
  "Returns all local wordclouds belonging to a discussion."
  [share-hash]
  [:discussion/share-hash => (s/coll-of ::specs/wordcloud)]
  (query '[:find [(pull ?wordcloud pattern) ...]
           :in $ ?share-hash pattern
           :where [?discussion :discussion/share-hash ?share-hash]
           [?wordcloud :wordcloud.local/discussion ?discussion]]
         share-hash patterns/local-wordcloud))

(>defn add-word-to-wordcloud
  "Adds a word to the wordcloud. If word already exists, increase count."
  [wordcloud-id word]
  [:db/id ::specs/non-blank-string => ::specs/non-blank-string]
  (let [[_ word-count :as existing-word-tuple]
        (query '[:find ?tuple .
                 :in $ ?wordcloud ?word
                 :where [?wordcloud :wordcloud.local/words ?tuple]
                 [(untuple ?tuple) [?word _]]]
               wordcloud-id word)]
    (if existing-word-tuple
      ;; Increment the words count
      (transact [[:db/retract wordcloud-id :wordcloud.local/words existing-word-tuple]
                 [:db/add wordcloud-id :wordcloud.local/words [word (inc word-count)]]])
      ;; Create the new word
      (transact [[:db/add wordcloud-id :wordcloud.local/words [word 1]]]))
    word))
