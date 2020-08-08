(ns meetly.meeting.processors
  (:require [ghostwheel.core :refer [>defn]]
            [clojure.walk :as walk]
            [meetly.meeting.database :as db])
  (:import (clojure.lang PersistentArrayMap)))

(>defn with-votes
  "Enrich every statement map with its vote-counts."
  [data]
  [any? :ret any?]
  (walk/postwalk
    #(if (and (= PersistentArrayMap (type %)) (contains? % :statement/content))
       (assoc % :meta/upvotes (db/upvotes-for-statement (:db/id %))
                :meta/downvotes (db/downvotes-for-statement (:db/id %)))
       %)
    data))

(>defn with-canonical-usernames
  "Enriches a step-vector with canonical :user/nickname."
  [[step args]]
  [vector? :ret vector?]
  (let [username (:user/nickname args)
        new-args (assoc args :user/nickname (db/canonical-username username))]
    [step new-args]))