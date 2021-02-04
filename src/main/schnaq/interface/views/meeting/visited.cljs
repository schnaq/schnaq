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

(>defn- parse-visited-schnaqs-from-localstorage
  "Read previously visited meetings from localstorage."
  []
  [:ret (s/coll-of (s/or :filled string? :empty nil?))]
  (remove empty?
          (string/split (ls/get-item :schnaqs/visited)
                        (re-pattern hash-separator))))

(>defn- build-visited-schnaqs-from-localstorage
  "Builds collection of visited meetings, based on previously stored hashes from
  the localstorage."
  [share-hash]
  [string? :ret (s/coll-of string?)]
  (let [schnaqs-visited (parse-visited-schnaqs-from-localstorage)
        schnaqs-visited-with-new-hash (conj schnaqs-visited share-hash)
        join-hashes (partial string/join hash-separator)]
    (if-not (some #{share-hash} schnaqs-visited)
      (join-hashes schnaqs-visited-with-new-hash)
      (join-hashes schnaqs-visited))))

(rf/reg-event-db
  :schnaqs.visited/store-hashes-from-localstorage
  (fn [db _]
    (assoc-in db [:schnaqs :visited-hashes]
              (parse-visited-schnaqs-from-localstorage))))

(rf/reg-event-fx
  :schnaq.visited/to-localstorage
  (fn [_ [_ share-hash]]
    {:fx [[:localstorage/write
           [:schnaqs/visited
            (build-visited-schnaqs-from-localstorage share-hash)]]
          [:dispatch [:schnaqs.visited/store-hashes-from-localstorage]]]}))

(rf/reg-sub
  :schnaqs.visited/all-hashes
  (fn [db _]
    (get-in db [:schnaqs :visited-hashes])))

(rf/reg-sub
  :schnaqs.visited/all
  (fn [db _]
    (get-in db [:schnaqs :visited])))

(rf/reg-event-db
  :schnaqs.visited/store-from-backend
  (fn [db [_ {:keys [discussions]}]]
    (assoc-in db [:schnaqs :visited] discussions)))

(rf/reg-event-fx
  :schnaqs.visited/load
  (fn [{:keys [db]} _]
    (let [visited-hashes (get-in db [:schnaqs :visited-hashes])]
      (when-not (empty? visited-hashes)
        {:fx [[:http-xhrio {:method :get
                            :uri (str (:rest-backend config) "/schnaqs/by-hashes")
                            :params {:share-hashes visited-hashes}
                            :format (ajax/transit-request-format)
                            :response-format (ajax/transit-response-format)
                            :on-success [:schnaqs.visited/store-from-backend]
                            :on-failure [:ajax.error/to-console]}]]}))))

(rf/reg-event-fx
  :schnaqs.public/load
  (fn [_ _]
    {:fx [[:http-xhrio {:method :get
                        :uri (str (:rest-backend config) "/schnaqs/public")
                        :params {}
                        :format (ajax/transit-request-format)
                        :response-format (ajax/transit-response-format)
                        :on-success [:schnaqs.public/store-from-backend]
                        :on-failure [:ajax.error/to-console]}]]}))

(rf/reg-event-db
  :schnaqs.public/store-from-backend
  (fn [db [_ {:keys [discussions]}]]
    (assoc-in db [:schnaqs :public] discussions)))

(rf/reg-sub
  :schnaqs/public
  (fn [db _]
    (get-in db [:schnaqs :public])))
