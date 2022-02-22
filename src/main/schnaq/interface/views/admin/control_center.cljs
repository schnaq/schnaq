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

(defn- input-form-builder
  "Build an input form for database manipulation."
  [input-id dispatch-event placeholder-text button-text]
  [:form
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

(defn- center-overview
  "The startpage of the admin center."
  []
  [pages/with-nav-and-header
   {:condition/needs-administrator? true
    :page/title (labels :admin.center.start/title)
    :page/heading (labels :admin.center.start/heading)
    :page/subheading (labels :admin.center.start/subheading)}
   [:div.container
    [:h2 (labels :admin.center.delete/heading)]
    [:h4 (labels :admin.center.delete.schnaq/heading)]
    [schnaq-deletion-form]
    [:h4.pt-3 (labels :admin.center.delete.user/heading)]
    [statements-deletion-form]]])

;; -----------------------------------------------------------------------------

(defn center-overview-route
  []
  [center-overview])
