(ns schnaq.interface.views.admin.control-center
  (:require [oops.core :refer [oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
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
  [input-id dispatch-event placeholder-text button-text]
  [:form.pb-3
   {:on-submit (fn [e]
                 (js-wrap/prevent-default e)
                 (when (js/confirm (labels :admin.center.delete/confirmation))
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
   :admin.center.delete.schnaq/label :admin.center.delete.schnaq/button])

(defn- statements-deletion-form
  "Delete any schnaq."
  []
  [input-form-builder "statements-deletion"
   :admin.delete.user/statements
   :admin.center.delete.user.statements/label :admin.center.delete.user.statements/button])

(defn- all-schnaqs-deletion
  "Delete all schnaqs for a given user."
  []
  [input-form-builder "schnaqs-deletion"
   :admin.delete.user/schnaqs
   :admin.center.delete.user.schnaqs/label :admin.center.delete.user.schnaqs/button])

(defn- delete-user-identity
  "Delete a user's identity."
  []
  [input-form-builder "user-identity-deletion"
   :admin.delete.user/identity
   :admin.center.delete.user.identity/label :admin.center.delete.user.identity/button])

(defn- center-overview
  "The startpage of the admin center."
  []
  [pages/with-nav-and-header
   {:condition/needs-administrator? true
    :page/heading (labels :admin.center.start/heading)
    :page/subheading (labels :admin.center.start/subheading)}
   [:div.container
    [:h2 (labels :admin.center.delete/heading)]
    [:h4 (labels :admin.center.delete.schnaq/heading)]
    [schnaq-deletion-form]
    [:h4.pt-3 (labels :admin.center.delete.user/heading)]
    [statements-deletion-form]
    [all-schnaqs-deletion]
    [delete-user-identity]]])

;; -----------------------------------------------------------------------------

(defn center-overview-route
  []
  [center-overview])
