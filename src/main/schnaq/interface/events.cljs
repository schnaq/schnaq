(ns schnaq.interface.events
  (:require [goog.string :as gstring]
            [goog.userAgent :as gagent]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.auth :as auth]
            [schnaq.interface.routes :as routes]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.localstorage :refer [from-localstorage]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.links :as links]))

;; Note: this lives in the common namespace to prevent circles through the routes import
(rf/reg-event-fx
 :hub.schnaqs/add
 (fn [{:keys [db]} [_ form]]
   (let [schnaq-input (oget form :schnaq-add-input :value)
         share-hash (or (-> (routes/parse-route schnaq-input) :path-params :share-hash)
                        schnaq-input)
         keycloak-name (get-in db [:current-route :path-params :keycloak-name])]
     {:fx [(http/xhrio-request db :post (gstring/format "/hub/%s/add" keycloak-name)
                               [:hub.schnaqs/add-success form]
                               {:share-hash share-hash}
                               [:hub.schnaqs/add-failure])]})))

(rf/reg-event-fx
 :load/last-added-schnaq
 (fn [_ _]
   (let [share-hash (from-localstorage :schnaq.last-added/share-hash)
         edit-hash (from-localstorage :schnaq.last-added/edit-hash)]
     (when (and (not (nil? edit-hash)) (not (nil? share-hash)))
       {:fx [[:dispatch [:schnaq/load-by-hash-as-admin share-hash edit-hash]]]}))))

(rf/reg-event-fx
 :re-frame-10x/hide-on-mobile
 (fn [_ _]
   (when (and (not shared-config/production?) gagent/MOBILE)
     {:fx [[:localstorage/assoc
            ['day8.re-frame-10x.show-panel false]]]})))

(rf/reg-event-fx
 :initialize/schnaq
 (fn [_ _]
   {:fx [[:dispatch [:localstorage/availability]] ;; WIP vllt unnötig
         [:dispatch [:username/from-localstorage]]
         [:dispatch [:user/init-device-id]]
         [:dispatch [:user.currency/from-localstorage]]
         [:dispatch [:user.tours/from-localstorage]]
         [:dispatch [:re-frame-10x/hide-on-mobile]]
         [:dispatch [:keycloak/init]]
         [:dispatch [:visited.save-statement-nums/store-hashes-from-localstorage]]
         [:dispatch [:visited.save-statement-ids/store-hashes-from-localstorage]]
         [:dispatch [:schnaqs.save-admin-access/store-hashes-from-localstorage]]
         [:dispatch [:schnaqs.visited/from-localstorage]]
         [:dispatch [:schnaqs.archived/from-localstorage]]
         [:dispatch [:schnaq.discussion-secrets/load-from-localstorage]]
         [:dispatch [:load/last-added-schnaq]]
         [:dispatch [:schnaq.polls/load-past-votes]]
         [:dispatch [:schnaq.votes/load-from-localstorage]]]}))

(rf/reg-event-fx
 :form/should-clear
 (fn [_ [_ form-elements]]
   {:fx [[:form/clear form-elements]]}))

(rf/reg-fx
 :form/clear
 (fn [form-elements]
   (toolbelt/reset-form-fields! form-elements)))

(rf/reg-event-fx
 :body.class/add
 (fn [_ [_ class]]
   {:fx [[:body.class/add! class]]}))

(rf/reg-fx
 :body.class/add!
 (fn [class]
   (.add (.. js/document -body -classList) class)))

(rf/reg-event-fx
 :body.class/remove
 (fn [_ [_ class]]
   {:fx [[:body.class/remove! class]]}))

(rf/reg-fx
 :body.class/remove!
 (fn [class]
   (.remove (.. js/document -body -classList) class)))

(rf/reg-event-fx
 :schnaq/select-current-from-backend
 (fn [_ [_ {:keys [schnaq]}]]
   {:fx [[:dispatch [:loading/toggle [:schnaq? false]]]
         [:dispatch [:schnaq/select-current schnaq]]
         [:dispatch [:theme.apply/from-discussion]]
         [:dispatch [:schnaq.wordcloud/from-current-premises]]]}))

(rf/reg-event-fx
 :schnaq/select-current
 (fn [{:keys [db]} [_ {:discussion/keys [share-hash edit-hash] :as schnaq}]]
   (let [admin-access-map (get-in db [:schnaqs :admin-access] {})
         updated-access-map (if edit-hash (assoc admin-access-map share-hash edit-hash) admin-access-map)
         edit-hash-localstorage (or edit-hash (get admin-access-map share-hash))]
     {:db (cond-> db
            true (assoc-in [:schnaq :selected] schnaq)
            edit-hash (assoc-in [:schnaqs :admin-access] updated-access-map)
            edit-hash-localstorage (assoc-in [:schnaq :selected :discussion/edit-hash] edit-hash-localstorage))
      :fx [[:dispatch [:schnaq.visited/to-localstorage share-hash]]
           [:localstorage/assoc [:schnaqs/admin-access updated-access-map]]]})))

(rf/reg-sub
 :schnaq/selected
 (fn [db _]
   (get-in db [:schnaq :selected])))

(rf/reg-sub
 :schnaq/author
 :<- [:schnaq/selected]
 (fn [schnaq]
   (:discussion/author schnaq)))

(rf/reg-sub
 :schnaq/share-hash
 :<- [:schnaq/selected]
 (fn [selected-schnaq _ _]
   (:discussion/share-hash selected-schnaq)))

(rf/reg-sub
 :schnaq/edit-hash
 :<- [:schnaq/selected]
 (fn [selected-schnaq _ _]
   (:discussion/edit-hash selected-schnaq)))

(rf/reg-sub
 :schnaq/share-link
 :<- [:schnaq/selected]
 (fn [selected-schnaq]
   (links/get-share-link (:discussion/share-hash selected-schnaq))))

(rf/reg-sub
 :schnaq.selected/statement-number
 :<- [:schnaq/selected]
 (fn [selected-schnaq _ _]
   (-> selected-schnaq :meta-info :all-statements)))

(rf/reg-sub
 :schnaq.selected/access-code
 :<- [:schnaq/selected]
 (fn [selected-schnaq _ _]
   (get-in selected-schnaq [:discussion/access :discussion.access/code])))

(rf/reg-sub
 :schnaq.selected/read-only?
 :<- [:schnaq/selected]
 (fn [selected-schnaq _ _]
   (not (nil? (some #{:discussion.state/read-only} (:discussion/states selected-schnaq))))))

(rf/reg-sub
 :schnaq.selected/theme
 :<- [:schnaq/selected]
 (fn [selected-schnaq _ _]
   (:discussion/theme selected-schnaq)))

(rf/reg-event-db
 :schnaq/share-hash
 (fn [db [_ share-hash]]
   (assoc-in db [:schnaq :selected :discussion/share-hash] share-hash)))

(rf/reg-event-db
 :schnaq.selected/dissoc
 ;; Remove currently selected schnaq
 (fn [db]
   (update db :schnaq dissoc :selected)))

(rf/reg-event-fx
 :schnaq/load-by-share-hash
 ;; Explicit api-url is optional
 (fn [{:keys [db]} [_ share-hash api-url]]
   (let [request (partial http/xhrio-request db :get "/schnaq/by-hash" [:schnaq/select-current-from-backend]
                          {:share-hash share-hash
                           :display-name (toolbelt/current-display-name db)})]
     {:db (assoc-in db [:schnaq :selected :discussion/share-hash] share-hash)
      :fx [[:dispatch [:loading/toggle [:schnaq? true]]]
           (if api-url
             (request [:ajax.error/to-console] api-url)
             (request))]})))

(rf/reg-event-fx
 :discussion.statements/mark-all-as-seen
 ;; Mark all statements in a schnaq as read when accessing the schnaq.
 (fn [{:keys [db]} [_ share-hash]]
   (when (auth/user-authenticated? db)
     {:fx [(http/xhrio-request db :put "/discussion/statements/mark-all-as-seen"
                               [:no-op]
                               {:share-hash share-hash})]})))

(rf/reg-event-fx
 :schnaq/add-visited!
 (fn [{:keys [db]} [_ share-hash]]
   (when (auth/user-authenticated? db)
     {:fx [(http/xhrio-request db :put "/schnaq/add-visited"
                               [:no-op]
                               {:share-hash share-hash})]})))

(rf/reg-event-fx
 :schnaq/remove-visited!
 (fn [{:keys [db]} [_ share-hash]]
   (when (auth/user-authenticated? db)
     {:fx [(http/xhrio-request db :delete "/schnaq/remove-visited"
                               [:no-op]
                               {:share-hash share-hash})]})))

(rf/reg-event-fx
 :schnaq/check-admin-credentials
 (fn [{:keys [db]} [_ share-hash edit-hash]]
   {:fx [(http/xhrio-request db :post "/credentials/validate" [:schnaq/check-admin-credentials-success]
                             {:share-hash share-hash
                              :edit-hash edit-hash}
                             [:ajax.error/as-notification])]}))

(rf/reg-event-fx
 ;; Response tells whether the user is allowed to see the view. (Actions are still checked by
 ;; the backend every time)
 :schnaq/check-admin-credentials-success
 (fn [_ [_ {:keys [valid-credentials?]}]]
   (when-not valid-credentials?
     {:fx [[:dispatch [:navigation/navigate :routes/forbidden-page]]]})))

(rf/reg-event-db
 :schnaq/save-as-last-added
 (fn [db [_ {:keys [schnaq]}]]
   (assoc-in db [:schnaq :last-added] schnaq)))

(rf/reg-sub
 :schnaq/last-added
 (fn [db _]
   (get-in db [:schnaq :last-added])))

(rf/reg-event-fx
 :schnaq/load-by-hash-as-admin
 (fn [{:keys [db]} [_ share-hash edit-hash]]
   {:fx [(http/xhrio-request db :post "/schnaq/by-hash-as-admin" [:schnaq/save-as-last-added]
                             {:share-hash share-hash
                              :edit-hash edit-hash})]}))
