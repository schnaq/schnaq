(ns schnaq.interface.views.admin.control-center
  (:require [cljs.pprint :refer [pprint]]
            [cljs.spec.alpha :as s]
            [clojure.test.check.properties]
            [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.localstorage :refer [from-localstorage]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.pages :as pages]
            [schnaq.shared-toolbelt :as shared-tools]))

(rf/reg-event-fx
 :admin.delete/schnaq
 (fn [{:keys [db]} [_ share-hash]]
   {:fx [(http/xhrio-request db :delete "/admin/schnaq/delete" [:no-op]
                             {:share-hash share-hash} [:ajax.error/as-notification])]}))

(rf/reg-event-fx
 :schnaq/remove!
 (fn [{:keys [db]} [_ share-hash]]
   {:fx [(http/xhrio-request db :delete "/schnaq"
                             [:schnaq.remove!/success share-hash]
                             {:share-hash share-hash}
                             [:ajax.error/as-notification])]}))

(rf/reg-event-fx
 :schnaq.remove!/success
 (fn [{:keys [db]} [_ share-hash]]
   {:db (-> db
            (update-in [:schnaqs :visited] (fn [schnaq-list]
                                             (remove #(= share-hash (:discussion/share-hash %)) schnaq-list)))
            (update-in [:user :meta :total-schnaqs] dec))
    :fx [(when-let [last-added-hash (from-localstorage :schnaq.last-added/share-hash)]
           (when (= last-added-hash share-hash)
             [:localstorage/dissoc :schnaq.last-added/share-hash]))]}))

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
       :admin.user/load
       :common/keycloak-id :admin.center.user.load/button]]
     (when user
       [:div.col-12.col-md-6
        [:pre {:style {:max-height "200px"}}
         [:code
          (with-out-str (pprint user))]]])]))

(defn- input-field
  "Generate an input field based on a model's field."
  [field]
  (let [user-value @(rf/subscribe [:admin/user field])
        keycloak-id @(rf/subscribe [:admin/user :user.registered/keycloak-id])
        fqname (shared-tools/namespaced-keyword->string field)
        id (str "user-input-" fqname)]
    [:div.col-12.col-md-4.pt-2
     [:label.small {:for id} fqname]
     [:div.input-group
      [:input.form-control {:id id
                            :name fqname
                            :placeholder user-value}]
      [:button.btn.btn-outline-danger
       {:type :button
        :on-click (fn [_]
                    (when (js/confirm (labels :admin.center.user.delete/confirmation))
                      (rf/dispatch [:admin.user.delete/attribute keycloak-id (keyword fqname)])))}
       [icon :times]]]
     [:small.text-muted (str (s/form field))]]))

(defn- user-form
  "Generate a form to update a user."
  []
  (when-let [keycloak-id @(rf/subscribe [:admin/user :user.registered/keycloak-id])]
    [:form {:on-submit (fn [e]
                         (.preventDefault e)
                         (let [form (oget e [:target :elements])
                               user (toolbelt/form->coerced-map form)]
                           (rf/dispatch [:admin.user/update (assoc user :user.registered/keycloak-id keycloak-id)])
                           (rf/dispatch [:form/should-clear form])))}
     [:div.row.pt-3
      [input-field :user.registered/display-name]
      [input-field :user.registered/first-name]
      [input-field :user.registered/last-name]
      [input-field :user.registered/email]
      [input-field :user.registered/profile-picture]]
     [:div.row
      [:p.lead.pt-3.mb-0 "Features"]
      [input-field :user.registered.features/concurrent-users]
      [input-field :user.registered.features/total-schnaqs]
      [input-field :user.registered.features/posts-per-schnaq]]
     [:div.row
      [:p.lead.pt-3.mb-0 "Stripe"]
      [input-field :user.registered.subscription/stripe-customer-id]
      [input-field :user.registered.subscription/stripe-id]]
     [:button.btn.btn-primary.mt-3 {:type :submit}
      (labels :admin.center.user.save/button)]]))

(defn- user-management
  "Show user management section."
  []
  [:section.pb-5
   [:h2 (labels :admin.center.user/headline)]
   [:p.lead (labels :admin.center.user/subheadline)]
   [load-user]
   [user-form]])

(rf/reg-event-fx
 :admin.user/update
 (fn [{:keys [db]} [_ user]]
   {:fx [(http/xhrio-request db :put "/admin/user"
                             [:admin.user.load/success]
                             {:user user}
                             [:ajax.error/as-notification])]}))

(rf/reg-event-fx
 :admin.user.delete/attribute
 (fn [{:keys [db]} [_ keycloak-id attribute]]
   {:db (update-in db [:admin :user] dissoc attribute)
    :fx [(http/xhrio-request db :delete "/admin/user"
                             [:admin.user/load keycloak-id]
                             {:keycloak-id keycloak-id
                              :attribute attribute}
                             [:ajax.error/as-notification])]}))

(rf/reg-event-fx
 :admin.user.delete/role
 (fn [{:keys [db]} [_ keycloak-id role]]
   {:db (update-in db [:admin :user :user.registered/roles] #(disj % role))
    :fx [(http/xhrio-request db :delete "/admin/user"
                             [:admin.user/load keycloak-id]
                             {:keycloak-id keycloak-id
                              :attribute :user.registered/roles
                              :value role}
                             [:ajax.error/as-notification])]}))

(rf/reg-event-fx
 :admin.user/load
 (fn [{:keys [db]} [_ keycloak-id]]
   {:fx [(http/xhrio-request db :get "/admin/user"
                             [:admin.user.load/success]
                             {:keycloak-id keycloak-id}
                             [:ajax.error/as-notification])]}))

(rf/reg-event-db
 :admin.user.load/success
 (fn [db [_ {:keys [user]}]]
   (assoc-in db [:admin :user] user)))

(rf/reg-sub
 :admin/user
 (fn [db [_ field]]
   (if field
     (get-in db [:admin :user field])
     (get-in db [:admin :user]))))

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
