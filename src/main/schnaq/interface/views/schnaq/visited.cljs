(ns schnaq.interface.views.schnaq.visited
  "Handling visited schnaqs."
  (:require [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]))

(rf/reg-event-db
 :schnaqs.visited/store-hashes-from-localstorage
 (fn [db _]
   (assoc-in db [:schnaqs :visited-hashes]
             (set (remove nil? (:schnaqs/visited local-storage))))))

(rf/reg-event-fx
 :schnaq.visited/to-localstorage
 (fn [_ [_ share-hash]]
   {:fx [(when share-hash
           [:localstorage/assoc
            [:schnaqs/visited
             (set (remove nil? (conj (:schnaqs/visited local-storage) share-hash)))]])
         [:dispatch [:schnaqs.visited/store-hashes-from-localstorage]]]}))

(rf/reg-event-fx
 :schnaq.visited/remove-from-localstorage!
 (fn [_ [_ share-hash]]
   {:fx [(when share-hash
           [:localstorage/assoc
            [:schnaqs/visited
             (set (remove #(= % share-hash) (:schnaqs/visited local-storage)))]])
         [:dispatch [:schnaqs.visited/store-hashes-from-localstorage]]]}))

(rf/reg-sub
 :schnaqs.visited/all
 (fn [db _]
   (get-in db [:schnaqs :visited])))

(rf/reg-event-db
 :schnaqs.visited/store-from-backend
 (fn [db [_ {:keys [schnaqs]}]]
   (assoc-in db [:schnaqs :visited] schnaqs)))

(rf/reg-event-db
 :schnaqs.visited/remove-from-app-db!
 (fn [db [_ share-hash]]
   (let [share-hash-str (str share-hash)]
     (-> db
         (update-in [:schnaqs :visited-hashes]
                    #(disj % share-hash-str))
         (update-in [:schnaqs :visited]
                    #(remove
                      (fn [schnaq] (= share-hash-str (:discussion/share-hash schnaq)))
                      %))))))

(rf/reg-event-fx
 :schnaqs.visited/remove!
 (fn [_ [_ share-hash]]
   {:fx [[:dispatch [:schnaqs.visited/remove-from-app-db! share-hash]]
         [:dispatch [:schnaq.visited/remove-from-localstorage! share-hash]]]}))

(rf/reg-sub
 :schnaqs.visited/filter
 (fn [db _]
   (get-in db [:schnaqs :filter])))

(rf/reg-event-fx
 :schnaqs.visited/load
 (fn [{:keys [db]} [_ filter]]
   (let [visited-hashes (get-in db [:schnaqs :visited-hashes])]
     (when-not (empty? visited-hashes)
       {:db (assoc-in db [:schnaqs :filter] filter)
        :fx [(http/xhrio-request
              db :post "/schnaqs/by-hashes"
              [:schnaqs.visited/store-from-backend]
              {:share-hashes visited-hashes
               :display-name (tools/current-display-name db)})]}))))

(rf/reg-event-fx
 :schnaqs.visited/merge-registered-users-visits
 ;; Takes the schnaqs the registered user has and merges them with the local ones.
 ;; This event should only be called, after the app is fully initialized (i.e. ls-schnaqs are already inside the db)
 (fn [{:keys [db]} [_ registered-visited-hashes]]
   (let [db-schnaqs (get-in db [:schnaqs :visited-hashes])
         merged-schnaqs (set (concat registered-visited-hashes db-schnaqs))
         route-name (navigation/canonical-route-name (get-in db [:current-route :data :name]))]
     {:db (assoc-in db [:schnaqs :visited-hashes] merged-schnaqs)
      :fx [[:localstorage/assoc [:schnaqs/visited merged-schnaqs]]
           ;; reload visited schnaqs when we are inside the visited-schnaqs view, otherwise this happens with the controller
           (when (= :routes.schnaqs/personal route-name)
             [:dispatch [:schnaqs.visited/load]])]})))
