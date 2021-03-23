(ns schnaq.interface.views.schnaq.visited
  "Handling visited schnaqs."
  (:require [ajax.core :as ajax]
            [cljs.spec.alpha :as s]
            [clojure.set :as cset]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn-]]
            [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.utils.localstorage :as ls]))

(def ^:private hash-separator ",")

(>defn- parse-visited-schnaqs-from-localstorage
  "Read previously visited meetings from localstorage."
  []
  [:ret (s/coll-of (s/or :filled string? :empty nil?))]
  ;; PARTIALLY DEPRECATED: Remove the meeting part after 2021-08-05
  ;; Every important user should have the new format then
  ;; PARTIALLY DEPRECATED, deleted after 2021-09-22: Remove old ls/get-item part and only use native local-storage
  (let [old-schnaq-string (set (remove empty?
                                       (string/split (ls/get-item :schnaqs/visited)
                                                     (re-pattern hash-separator))))
        schnaqs-visited (:schnaqs/visited local-storage)
        meeting-string (set (remove empty?
                                    (string/split (ls/get-item :meetings/visited)
                                                  (re-pattern hash-separator))))]
    (cset/union old-schnaq-string schnaqs-visited meeting-string)))

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
