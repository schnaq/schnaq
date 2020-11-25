(ns schnaq.interface.views.meeting.visited
  "Handling visited meetings."
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.utils.localstorage :as ls]
            [ajax.core :as ajax]))

(def ^:private hash-separator ",")

(>defn- parse-visited-meetings-from-localstorage
  "Read previously visited meetings from localstorage."
  []
  [:ret (s/coll-of (s/or :filled string? :empty nil?))]
  (remove empty?
          (string/split (ls/get-item :meetings/visited)
                        (re-pattern hash-separator))))

(>defn- build-visited-meetings-from-localstorage
  "Builds collection of visited meetings, based on previously stored hashes from
  the localstorage."
  [share-hash]
  [string? :ret (s/coll-of string?)]
  (let [meetings-visited (parse-visited-meetings-from-localstorage)
        meetings-visited-with-new-hash (conj meetings-visited share-hash)
        join-hashes (partial string/join hash-separator)]
    (if-not (some #{share-hash} meetings-visited)
      (join-hashes meetings-visited-with-new-hash)
      (join-hashes meetings-visited))))

(rf/reg-event-db
  :meetings.visited/store-hashes-from-localstorage
  (fn [db _]
    (assoc-in db [:meetings :visited-hashes]
              (parse-visited-meetings-from-localstorage))))

(rf/reg-event-fx
  :meeting.visited/to-localstorage
  (fn [_ [_ share-hash]]
    {:fx [[:localstorage/write
           [:meetings/visited
            (build-visited-meetings-from-localstorage share-hash)]]
          [:dispatch [:meetings.visited/store-hashes-from-localstorage]]]}))

(rf/reg-sub
  :meetings.visited/all-hashes
  (fn [db _]
    (get-in db [:meetings :visited-hashes])))

(rf/reg-sub
  :meetings.visited/all
  (fn [db _]
    (get-in db [:meetings :visited])))

(rf/reg-event-db
  :meeting.visited/store-from-backend
  (fn [db [_ {:keys [meetings]}]]
    (assoc-in db [:meetings :visited] meetings)))

(rf/reg-event-fx
  :meetings.visited/load
  (fn [{:keys [db]} _]
    (let [visited-hashes (get-in db [:meetings :visited-hashes])]
      (when-not (empty? visited-hashes)
        {:fx [[:http-xhrio {:method :get
                            :uri (str (:rest-backend config) "/meetings/by-hashes")
                            :params {:share-hashes visited-hashes}
                            :format (ajax/transit-request-format)
                            :response-format (ajax/transit-response-format)
                            :on-success [:meeting.visited/store-from-backend]
                            :on-failure [:ajax.error/as-notification]}]]}))))
