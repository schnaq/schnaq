(ns schnaq.interface.views.schnaq.visited
  "Handling visited schnaqs."
  (:require [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [schnaq.interface.auth :as auth]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]))

(rf/reg-event-db
 :schnaqs.visited/from-localstorage
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
         [:dispatch [:schnaqs.visited/from-localstorage]]]}))

(rf/reg-event-fx
 :schnaq.visited/remove-from-localstorage!
 (fn [_ [_ share-hash]]
   {:fx [(when share-hash
           [:localstorage/assoc
            [:schnaqs/visited
             (set (remove #(= % share-hash) (:schnaqs/visited local-storage)))]])
         [:dispatch [:schnaqs.visited/from-localstorage]]]}))

;; -----------------------------------------------------------------------------

(rf/reg-event-db
 :schnaqs.archived/from-localstorage
 (fn [db]
   (assoc-in db [:schnaqs :archived-hashes]
             (set (remove nil? (:schnaqs/archived local-storage))))))

(rf/reg-event-fx
 :schnaq.archived/to-localstorage
 (fn [_ [_ share-hash]]
   {:fx [(when share-hash
           [:localstorage/assoc
            [:schnaqs/archived
             (set (remove nil? (conj (:schnaqs/archived local-storage) share-hash)))]])
         [:dispatch [:schnaqs.archived/from-localstorage]]]}))

(rf/reg-event-fx
 :schnaq.archived/remove-from-localstorage!
 (fn [_ [_ share-hash]]
   {:fx [(when share-hash
           [:localstorage/assoc
            [:schnaqs/archived
             (set (remove #(= % share-hash) (:schnaqs/archived local-storage)))]])
         [:dispatch [:schnaqs.archived/from-localstorage]]]}))

;; -----------------------------------------------------------------------------

(rf/reg-sub
 :schnaqs.visited/all
 :<- [:schnaqs.visited/filter]
 :<- [:schnaq/visited]
 :<- [:schnaq.visited/archived-hashes]
 :<- [:user/id]
 (fn [[schnaq-filter visited-schnaqs archived-hashes user-id]]
   (let [archived? (fn [schnaq] (contains? archived-hashes (:discussion/share-hash schnaq)))
         from-current-user? (fn [schnaq] (= user-id (-> schnaq :discussion/author :db/id)))]
     (case schnaq-filter
       :created-by-user (filter from-current-user? visited-schnaqs)
       :archived-by-user (filter archived? visited-schnaqs)
       (filter #(not (archived? %)) visited-schnaqs)))))

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
   {:fx [[:dispatch [:schnaq/remove-visited! share-hash]]
         [:dispatch [:schnaqs.visited/remove-from-app-db! share-hash]]
         [:dispatch [:schnaq.visited/remove-from-localstorage! share-hash]]]}))

(rf/reg-sub
 :schnaqs.visited/filter
 (fn [db _]
   (get-in db [:schnaqs :filter])))

(rf/reg-event-fx
 :schnaqs.visited/load
 (fn [{:keys [db]}]
   (let [visited-hashes (get-in db [:schnaqs :visited-hashes])
         schnaq-filter (keyword (get-in db [:current-route :parameters :query :filter]))]
     (when-not (empty? visited-hashes)
       {:db (if schnaq-filter
              (assoc-in db [:schnaqs :filter] schnaq-filter)
              (update db :schnaqs dissoc :filter))
        :fx [(http/xhrio-request
              db :post "/schnaqs/by-hashes"
              [:schnaqs.visited/store-from-backend]
              {:share-hashes visited-hashes
               :display-name (tools/current-display-name db)})]}))))

(rf/reg-event-fx
 :schnaqs.archived-and-visited/to-localstorage
 ;; Takes the schnaqs the registered user has and merges them with the local ones.
 ;; This event should only be called, after the app is fully initialized (i.e. ls-schnaqs are already inside the db)
 (fn [{:keys [db]} [_ visited-hashes archived-hashes]]
   (let [db-hashes-visited (get-in db [:schnaqs :visited-hashes])
         merged-visited-hashes (set (concat visited-hashes db-hashes-visited))
         db-hashes-archived (get-in db [:schnaqs :archived-hashes])
         merged-archived-hashes (set (concat archived-hashes db-hashes-archived))
         route-name (navigation/canonical-route-name (get-in db [:current-route :data :name]))]
     {:db (-> db
              (assoc-in [:schnaqs :visited-hashes] merged-visited-hashes)
              (assoc-in [:schnaqs :archived-hashes] merged-archived-hashes))
      :fx [[:localstorage/assoc [:schnaqs/visited merged-visited-hashes]]
           [:localstorage/assoc [:schnaqs/archived merged-archived-hashes]]
           ;; reload visited schnaqs when we are inside the visited-schnaqs view, otherwise this happens with the controller
           (when (= :routes.schnaqs/personal route-name)
             [:dispatch [:schnaqs.visited/load]])]})))

(rf/reg-event-fx
 :schnaqs.visited/archive!
 (fn [{:keys [db]} [_ share-hash]]
   {:db (update-in db [:schnaqs :archived-hashes] #(set (conj % share-hash)))
    :fx [[:dispatch [:schnaq.archived/to-localstorage share-hash]]
         (when (auth/user-authenticated? db)
           (http/xhrio-request
            db :put "/schnaq/archive"
            [:no-op]
            {:share-hash share-hash}))]}))

(rf/reg-event-fx
 :schnaqs.visited/unarchive!
 (fn [{:keys [db]} [_ share-hash]]
   {:db (update-in db [:schnaqs :archived-hashes] #(disj % share-hash))
    :fx [[:dispatch [:schnaq.archived/remove-from-localstorage! share-hash]]
         (when (auth/user-authenticated? db)
           (http/xhrio-request
            db :delete "/schnaq/archive"
            [:no-op]
            {:share-hash share-hash}))]}))

(rf/reg-sub
 :schnaq.visited/archived-hashes
 (fn [db]
   (get-in db [:schnaqs :archived-hashes] #{})))

(rf/reg-sub
 :schnaq/visited
 (fn [db]
   (get-in db [:schnaqs :visited] #{})))

(rf/reg-sub
 :schnaq.visited/archived?
 :<- [:schnaq.visited/archived-hashes]
 (fn [archived-hashes [_ share-hash]]
   (contains? archived-hashes share-hash)))
