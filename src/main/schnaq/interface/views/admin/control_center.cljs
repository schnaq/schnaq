(ns schnaq.interface.views.admin.control-center
  (:require [cljs.pprint :refer [pprint]]
            [oops.core :refer [oget+]]
            [re-frame.core :as rf]
            [schnaq.database.specs :as specs]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.pages :as pages]))

(rf/reg-event-fx
 :admin.delete/schnaq
 (fn [{:keys [db]} [_ share-hash]]
   {:fx [(http/xhrio-request db :delete "/admin/schnaq/delete" [:no-op]
                             {:share-hash share-hash} [:ajax.error/as-notification])]}))

(rf/reg-event-fx
 :admin.delete.user/statements
 (fn [{:keys [db]} [_ keycloak-id]]
   {:fx [(http/xhrio-request db :delete "/admin/user/statements" [:no-op]
                             {:keycloak-id keycloak-id} [:ajax.error/as-notification])]}))

(rf/reg-event-fx
 :admin.delete.user/schnaqs
 (fn [{:keys [db]} [_ keycloak-id]]
   {:fx [(http/xhrio-request db :delete "/admin/user/schnaqs" [:no-op]
                             {:keycloak-id keycloak-id} [:ajax.error/as-notification])]}))

(rf/reg-event-fx
 :admin.delete.user/identity
 (fn [{:keys [db]} [_ keycloak-id]]
   {:fx [(http/xhrio-request db :delete "/admin/user/identity" [:no-op]
                             {:keycloak-id keycloak-id} [:ajax.error/as-notification])]}))

;; -----------------------------------------------------------------------------

(defn- input-form-builder
  "Build an input form for database manipulation."
  [input-id dispatch-event placeholder-text button-text confirmation-text]
  [:form.pb-3
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (if confirmation-text
                   (when (js/confirm (labels confirmation-text))
                     (rf/dispatch [dispatch-event
                                   (oget+ e [:target :elements input-id :value])]))
                   (rf/dispatch [dispatch-event
                                 (oget+ e [:target :elements input-id :value])])))}
   [:div.input-group
    [:div.form-floating
     [:input.form-control
      {:id input-id
       :name input-id
       :placeholder (labels placeholder-text)
       :defaultValue "d6d8a351-2074-46ff-aa9b-9c57ab6c6a18"
       :required true}]
     [:label {:for input-id} (labels placeholder-text)]]
    [:button.input-group-text (labels button-text)]]])

(defn- schnaq-deletion-form
  "Delete any schnaq."
  []
  [input-form-builder "share-hash-deletion"
   :admin.delete/schnaq
   :admin.center.delete.schnaq/label :admin.center.delete.schnaq/button
   :admin.center.delete/confirmation])

(defn- statements-deletion-form
  "Delete any schnaq."
  []
  [input-form-builder "statements-deletion"
   :admin.delete.user/statements
   :common/keycloak-id :admin.center.delete.user.statements/button
   :admin.center.delete/confirmation])

(defn- all-schnaqs-deletion
  "Delete all schnaqs for a given user."
  []
  [input-form-builder "schnaqs-deletion"
   :admin.delete.user/schnaqs
   :common/keycloak-id :admin.center.delete.user.schnaqs/button
   :admin.center.delete/confirmation])

(defn- delete-user-identity
  "Delete a user's identity."
  []
  [input-form-builder "user-identity-deletion"
   :admin.delete.user/identity
   :common/keycloak-id :admin.center.delete.user.identity/button
   :admin.center.delete/confirmation])

;; -----------------------------------------------------------------------------

(defn- deletion-section
  "Admin section to delete entities."
  []
  [:<>
   [:h2 (labels :admin.center.delete/heading)]
   [:section.row
    [:div.col-md-6.col-12
     [:h4 (labels :admin.center.delete.schnaq/heading)]
     [schnaq-deletion-form]]
    [:div.col-md-6.col-12
     [:h4 (labels :admin.center.delete.user/heading)]
     [statements-deletion-form]
     [all-schnaqs-deletion]
     [delete-user-identity]]]])

;; -----------------------------------------------------------------------------

(defn- load-user
  "Form to load and display user."
  []
  (let [user @(rf/subscribe [:admin/user])]
    [:div.row
     [:div.col-12.col-md-6
      [input-form-builder "user-load"
       :admin/load-user
       :common/keycloak-id :admin.center.user.load/button]]
     [:div.col-12.col-md-6
      [:pre {:style {:max-height "200px"}}
       [:code
        (with-out-str (pprint user))]]]]))

(defn- change-role []
  (let [selection-id "select-role-id"]
    [:form {:on-submit (fn [e]
                         (.preventDefault e)
                         (when-let [selection (oget+ e [:target :elements selection-id :value])]
                           (when (not-empty selection)
                             (let [selection (keyword "role" selection)]
                               (prn selection)))))}
     [:label "Füge Rolle zu User hinzu"
      [:div.input-group
       [:select.form-control {:id selection-id}
        [:option {:value ""} "---"]
        (for [role specs/user-roles]
          [:option {:key (str "role-selector-" role)
                    :value role} role])]
       [:button.input-group-text {:type :submit} "foo"]]]]))

(defn- user-management
  "Show user management section."
  []
  [:section
   [:h2 "User Management"]
   [load-user]
   [change-role]])

(rf/reg-event-fx
 :admin/load-user
 (fn [{:keys [db]} [_ keycloak-id]]
   {:fx [(http/xhrio-request db :get "/admin/user"
                             [:admin.load-user/success]
                             {:keycloak-id keycloak-id})]}))

(rf/reg-event-db
 :admin.load-user/success
 (fn [db [_ {:keys [user]}]]
   (assoc-in db [:admin :user] user)))

(rf/reg-sub
 :admin/user
 (fn [db]
   (get-in db [:admin :user])))

;; -----------------------------------------------------------------------------

(defn- center-overview
  "The startpage of the admin center."
  []
  [pages/with-nav-and-header
   {:condition/needs-administrator? true
    :page/heading (labels :admin.center.start/heading)
    :page/subheading (labels :admin.center.start/subheading)
    :page/vertical-header? true}
   [:div.container
    [user-management]
    [deletion-section]]])

;; -----------------------------------------------------------------------------

(defn center-overview-route
  []
  [center-overview])
