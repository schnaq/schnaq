(ns schnaq.interface.views.meeting.visited
  "Handling visited meetings."
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn-]]
            [schnaq.interface.utils.localstorage :as ls]
            [re-frame.core :as rf]))

(>defn- parse-visited-meetings-from-localstorage
  "Read previously visited meetings from localstorage."
  []
  [:ret (s/coll-of (s/or :filled string? :empty nil?))]
  (remove empty?
          (string/split (ls/get-item :meetings/visited)
                        #",")))

(>defn- build-visited-meetings-from-localstorage
  "Builds collection of visited meetings, based on previously stored hashes from
  the localstorage."
  [share-hash]
  [string? :ret (s/coll-of string?)]
  (let [meetings-visited (parse-visited-meetings-from-localstorage)]
    (when-not (some #{share-hash} meetings-visited)
      (string/join "," (conj meetings-visited share-hash)))))

(rf/reg-event-db
  :meeting.visited/store-hashes-from-localstorage
  (fn [db _]
    (assoc-in db [:meetings :visited-hashs]
              (parse-visited-meetings-from-localstorage))))

(rf/reg-event-fx
  :meeting.visited/to-localstorage
  (fn [_ [_ share-hash]]
    {:fx [[:localstorage/write :meetings/visited (build-visited-meetings-from-localstorage share-hash)]]}))
