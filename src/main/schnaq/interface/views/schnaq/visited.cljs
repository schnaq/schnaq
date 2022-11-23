(ns schnaq.interface.views.schnaq.visited
  "Handling visited schnaqs."
  (:require [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.auth :as auth]
            [schnaq.interface.config :as config]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.localstorage :refer [from-localstorage]]
            [schnaq.interface.utils.toolbelt :as tools]))

(rf/reg-event-db
 :schnaqs.visited/from-localstorage
 (fn [db _]
   (when-let [visited-schnaqs (from-localstorage :schnaqs/visited)]
     (assoc-in db [:schnaqs :visited-hashes]
               (set (remove nil? visited-schnaqs))))))

(rf/reg-event-fx
 :schnaq.visited/to-localstorage
 (fn [_ [_ share-hash]]
   (when share-hash
     (let [visited-share-hashes (or (from-localstorage :schnaqs/visited) #{})]
       {:fx [[:localstorage/assoc
              [:schnaqs/visited
               (set (remove nil? (conj visited-share-hashes share-hash)))]]
             [:dispatch [:schnaqs.visited/from-localstorage]]]}))))

(rf/reg-event-fx
 :schnaq.visited/remove-from-localstorage!
 (fn [_ [_ share-hash]]
   (when-let [visited-schnaqs (from-localstorage :schnaqs/visited)]
     {:fx [(when share-hash
             [:localstorage/assoc
              [:schnaqs/visited
               (set (remove #(= % share-hash) visited-schnaqs))]])
           [:dispatch [:schnaqs.visited/from-localstorage]]]})))

;; -----------------------------------------------------------------------------

(rf/reg-event-db
 :schnaqs.archived/from-localstorage
 (fn [db]
   (when-let [archived-schnaqs (from-localstorage :schnaqs/archived)]
     (assoc-in db [:schnaqs :archived-hashes]
               (set (remove nil? archived-schnaqs))))))

(rf/reg-event-fx
 :schnaq.archived/to-localstorage
 (fn [_ [_ share-hash]]
   (when-let [archived-schnaqs (from-localstorage :schnaqs/archived)]
     {:fx [(when share-hash
             [:localstorage/assoc
              [:schnaqs/archived
               (set (remove nil? (conj archived-schnaqs share-hash)))]])
           [:dispatch [:schnaqs.archived/from-localstorage]]]})))

(rf/reg-event-fx
 :schnaq.archived/remove-from-localstorage!
 (fn [_ [_ share-hash]]
   (when-let [archived-schnaqs (from-localstorage :schnaqs/archived)]
     {:fx [(when share-hash
             [:localstorage/assoc
              [:schnaqs/archived
               (set (remove #(= % share-hash) archived-schnaqs))]])
           [:dispatch [:schnaqs.archived/from-localstorage]]]})))

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

(rf/reg-event-fx
 :schnaqs.visited/store-from-backend
 (fn [{:keys [db]} [_ {:keys [schnaqs]}]]
   {:db (assoc-in db [:schnaqs :visited] schnaqs)
    :fx [[:dispatch [:loading/toggle [:schnaqs? false]]]]}))

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
 ;; Load visited schnaqs for the user. Always adds FAQ-schnaq in production.
 (fn [{:keys [db]}]
   (let [visited-hashes (get-in db [:schnaqs :visited-hashes])
         visited-hashes-with-faq (conj visited-hashes config/faq-share-hash)
         share-hashes (if shared-config/production? visited-hashes-with-faq visited-hashes)
         schnaq-filter (keyword (get-in db [:current-route :parameters :query :filter]))]
     (when-not (empty? share-hashes)
       {:db (if schnaq-filter
              (assoc-in db [:schnaqs :filter] schnaq-filter)
              (update db :schnaqs dissoc :filter))
        :fx [(http/xhrio-request
              db :post "/schnaqs/by-hashes"
              [:schnaqs.visited/store-from-backend]
              {:share-hashes share-hashes
               :display-name (tools/current-display-name db)})
             [:dispatch [:loading/toggle [:schnaqs? true]]]]}))))

(rf/reg-event-fx
 :schnaqs.archived-and-visited/to-localstorage
 ;; Takes the schnaqs the registered user has and merges them with the local ones.
 ;; This event should only be called, after the app is fully initialized (i.e. ls-schnaqs are already inside the db)
 (fn [{:keys [db]} [_ visited-hashes archived-hashes]]
   (let [db-hashes-visited (get-in db [:schnaqs :visited-hashes])
         merged-visited-hashes (set (concat visited-hashes db-hashes-visited))
         db-hashes-archived (get-in db [:schnaqs :archived-hashes])
         merged-archived-hashes (set (concat archived-hashes db-hashes-archived))]
     {:db (-> db
              (assoc-in [:schnaqs :visited-hashes] merged-visited-hashes)
              (assoc-in [:schnaqs :archived-hashes] merged-archived-hashes))
      :fx [[:localstorage/assoc [:schnaqs/visited merged-visited-hashes]]
           [:localstorage/assoc [:schnaqs/archived merged-archived-hashes]]]})))

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
