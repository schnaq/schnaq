(ns schnaq.interface.views.hub.overview
  (:require [com.fulcrologic.guardrails.core :refer [>defn-]]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.auth :as auth]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.feed.overview :as feed]
            [schnaq.interface.views.hub.common :as hub-common]
            [schnaq.interface.views.pages :as pages]))

(defn- add-schnaq-to-hub-form
  "Add a new schnaq to hub."
  []
  [:form.pb-3
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (rf/dispatch [:hub.schnaqs/add (oget e [:target :elements])]))}
   [:label.form-label.small (labels :hub.add.schnaq.input/label)]
   [:div.input-group
    [:input.form-control {:name "schnaq-add-input"
                          :required true
                          :placeholder (labels :hub.add.schnaq.input/placeholder)}]
    [:button.btn.btn-primary {:type "submit"}
     [icon :plus "m-auto"]]]])

(defn hub-panel
  "Small overview for the hub."
  []
  (let [hub @(rf/subscribe [:hub/current])]
    [:section.panel-white
     [hub-common/single-hub hub]
     [:div.mx-2
      [add-schnaq-to-hub-form]]
     [:div.text-center
      [:a.btn.btn-outline-dark.btn-rounded-2
       {:href (navigation/href :routes.hub/edit {:keycloak-name (:hub/keycloak-name hub)})}
       [icon :cog "me-1"]
       (labels :hub/settings)]]
     [:hr]
     [feed/sidebar-info-links]]))

(defn member-list
  "Lists all members of a hub."
  []
  (let [members @(rf/subscribe [:hub.current/members])]
    (when (seq members)
      [:section.panel-white.mt-4
       [:h5.text-center.pb-2 (labels :hub.members/heading)]
       [:ul.list-group
        (for [member members]
          [:li.list-group-item.border-0.py-0 {:key (:db/id member)}
           [common/inline-avatar member 30]])]])))

(defn sidebar-right []
  [:<>
   [hub-panel]
   [member-list]])

(>defn- hub-index
  "Shows the page for an overview of schnaqs for a hub. Takes a keycloak-name which
  uniquely refers to a hub."
  []
  [:ret vector?]
  (let [keycloak-name (get-in @(rf/subscribe [:navigation/current-route])
                              [:path-params :keycloak-name])]
    [pages/three-column-layout
     {:page/heading (gstring/format (labels :hub/heading) keycloak-name)
      :page/title (str keycloak-name)
      :condition/needs-authentication? true}
     [feed/feed-navigation]
     [feed/schnaq-list-view [:hubs/schnaqs keycloak-name] true]
     [sidebar-right]]))

(defn hub-overview
  "Renders all schnaqs belonging to the hub."
  []
  [hub-index])

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :hub/load
 (fn [{:keys [db]} [_ keycloak-name]]
   (when (auth/user-authenticated? db)
     {:fx [(http/xhrio-request db :get (str "/hub/" keycloak-name) [:hub.load/success keycloak-name])]})))

(rf/reg-event-fx
 :hubs.personal/load
 (fn [{:keys [db]}]
   (when (auth/user-authenticated? db)
     {:fx [(http/xhrio-request db :get "/hubs/personal" [:hubs.load/success])]})))

(rf/reg-event-db
 :hubs.load/success
 (fn [db [_ {:keys [hubs]}]]
   (when-not (empty? hubs)
     (let [formatted-hubs
           (into {} (map #(vector (:hub/keycloak-name %) %) hubs))]
       (assoc db :hubs formatted-hubs)))))

(rf/reg-event-db
 :hub.load/success
 (fn [db [_ keycloak-name response]]
   (-> db
       (assoc-in [:hubs keycloak-name] (:hub response))
       ;; We do not reuse the :hubs namespace, because the query of all hubs overwrites it when a race condition hits.
       (assoc-in [:hub-members keycloak-name] (:hub-members response)))))

(rf/reg-sub
 :hubs/schnaqs
 (fn [db [_ keycloak-name]]
   (get-in db [:hubs keycloak-name :hub/schnaqs] [])))

(rf/reg-sub
 :hubs/all
 (fn [db] (:hubs db)))

(rf/reg-sub
 :hub/current
 (fn [db]
   (let [keycloak-name (get-in db [:current-route :path-params :keycloak-name])]
     (get-in db [:hubs keycloak-name]))))

(rf/reg-event-db
 :hub/select!
 (fn [db [_ keycloak-name]]
   (if keycloak-name
     (assoc db :hub-selected keycloak-name)
     (dissoc db :hub-selected))))

(rf/reg-sub
 :hub/selected
 (fn [db]
   (:hub-selected db)))

(rf/reg-sub
 :hub.current/members
 (fn [db _]
   (let [keycloak-name (get-in db [:current-route :path-params :keycloak-name])]
     (get-in db [:hub-members keycloak-name] []))))

(rf/reg-event-fx
 :hub.schnaqs/add-success
 (fn [{:keys [db]} [_ form response]]
   (let [hub (:hub response)]
     {:db (assoc-in db [:hubs (:hub/keycloak-name hub)] hub)
      :fx [[:dispatch [:notification/add
                       #:notification{:title (labels :hub.add.schnaq.success/title)
                                      :body (labels :hub.add.schnaq.success/body)
                                      :context :success}]]
           [:form/clear form]]})))

(rf/reg-event-fx
 :hub.schnaqs/add-failure
 (fn [_ _]
   {:fx [[:dispatch [:notification/add
                     #:notification{:title (labels :hub.add.schnaq.error/title)
                                    :body (labels :hub.add.schnaq.error/body)
                                    :context :danger}]]]}))

(rf/reg-event-fx
 :hub.remove/schnaq
 (fn [{:keys [db]} [_ share-hash]]
   (let [keycloak-name (get-in db [:current-route :path-params :keycloak-name])]
     {:fx [(http/xhrio-request db :delete (gstring/format "/hub/%s/remove" keycloak-name)
                               [:hub.remove.schnaq/success]
                               {:share-hash share-hash}
                               [:hub.remove.schnaq/failure])]})))

(rf/reg-event-fx
 :hub.remove.schnaq/success
 (fn [{:keys [db]} [_ response]]
   (let [hub (:hub response)]
     {:db (assoc-in db [:hubs (:hub/keycloak-name hub)] hub)
      :fx [[:dispatch [:notification/add
                       #:notification{:title (labels :hub.remove.schnaq.success/title)
                                      :body (labels :hub.remove.schnaq.success/body)
                                      :context :success}]]]})))

(rf/reg-event-fx
 :hub.remove.schnaq/failure
 (fn [_ _]
   {:fx [[:dispatch [:notification/add
                     #:notification{:title (labels :hub.remove.schnaq.error/title)
                                    :body (labels :hub.remove.schnaq.error/body)
                                    :context :danger}]]]}))
