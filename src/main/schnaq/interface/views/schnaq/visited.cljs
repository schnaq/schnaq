(ns schnaq.interface.views.schnaq.visited
  "Handling visited schnaqs."
  (:require [cljs.spec.alpha :as s]
            [clojure.set :as cset]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn-]]
            [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.localstorage :as ls]))

(def ^:private hash-separator ",")

(>defn- parse-visited-schnaqs-from-localstorage
  "Read previously visited meetings from localstorage."
  []
  [:ret (s/coll-of (s/or :filled string? :empty nil?))]
  ;; PARTIALLY DEPRECATED, deleted after 2021-09-22: Remove old ls/get-item part and only use native local-storage
  (let [old-schnaq-string (set (remove empty?
                                       (string/split (ls/get-item :schnaqs/visited)
                                                     (re-pattern hash-separator))))
        schnaqs-visited (set (remove empty? (:schnaqs/visited local-storage)))]
    (cset/union old-schnaq-string schnaqs-visited)))

(rf/reg-event-db
  :schnaqs.visited/store-hashes-from-localstorage
  (fn [db _]
    (assoc-in db [:schnaqs :visited-hashes] (parse-visited-schnaqs-from-localstorage))))

(rf/reg-event-fx
  :schnaq.visited/to-localstorage
  (fn [_ [_ share-hash]]
    {:fx [[:localstorage/assoc
           [:schnaqs/visited (conj (parse-visited-schnaqs-from-localstorage) share-hash)]]
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
  (fn [db [_ {:keys [schnaqs]}]]
    (assoc-in db [:schnaqs :visited] schnaqs)))

(rf/reg-event-fx
  :schnaqs.visited/load
  (fn [{:keys [db]} _]
    (let [visited-hashes (get-in db [:schnaqs :visited-hashes])]
      (when-not (empty? visited-hashes)
        {:fx [(http/xhrio-request
                db :get "/schnaqs/by-hashes"
                [:schnaqs.visited/store-from-backend]
                {:share-hashes visited-hashes})]}))))

(rf/reg-event-fx
  :schnaqs.public/load
  (fn [{:keys [db]} _]
    {:fx [(http/xhrio-request db :get "/schnaqs/public"
                              [:schnaqs.public/store-from-backend])]}))

(rf/reg-event-db
  :schnaqs.public/store-from-backend
  (fn [db [_ {:keys [schnaqs]}]]
    (assoc-in db [:schnaqs :public] schnaqs)))

(rf/reg-sub
  :schnaqs/public
  (fn [db _]
    (get-in db [:schnaqs :public])))

(rf/reg-event-fx
  :schnaqs.visited/merge-registered-users-visits
  ;; Takes the schnaqs the registered user has and merges them with the local ones.
  ;; This event should only be called, after the app is fully initialized (i.e. ls-schnaqs are already inside the db)
  (fn [{:keys [db]} [_ registered-visited-hashes]]
    (let [db-schnaqs (get-in db [:schnaqs :visited-hashes])
          merged-schnaqs (set (concat registered-visited-hashes db-schnaqs))]
      {:db (assoc-in db [:schnaqs :visited-hashes] merged-schnaqs)
       :fx [[:localstorage/assoc [:schnaqs/visited merged-schnaqs]]]})))
