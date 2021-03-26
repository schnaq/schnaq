(ns schnaq.interface.views.user.edit-account
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :as config]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user.elements :as elements]))

(defn name-input
  "The input form for the display name."
  []
  (let [display-name @(rf/subscribe [:user/display-name])]
    [:input#user-display-name.form-control.form-title.form-border-bottom.mb-2
     {:type "text"
      :autoComplete "off"
      :defaultValue display-name
      :required true}]))

(defn- change-user-info []
  (let [display-name (rf/subscribe [:user/display-name])]
    [:div.manage-account-content.shadow-straight-light
     [:h4.text-gray-600.mb-5 (labels :user.settings/change-name)]
     [:form
      {:on-submit (fn [e]
                    (let [display-name (oget e [:target :elements :user-display-name :value])]
                      (js-wrap/prevent-default e)
                      (rf/dispatch [:user.name/update display-name])))}
      [:div.d-flex.flex-row
       [:div.mr-4 [common/avatar display-name 50]]
       [name-input]]
      [:div.row.pt-5
       [:div.col.text-left.my-3
        [:a.btn.button-secondary {:href config/keycloak-profile-page}
         (labels :user.keycloak-settings)]]
       [:div.col.text-right.my-3
        [:button.btn.button-primary
         (labels :user.settings.button/change-account-information)]]]]]))

(defn- content []
  [pages/with-nav
   {:page/heading (labels :user/edit-account)}
   [elements/user-view [change-user-info]]])

(defn view []
  [content])

;; subs ;;

(rf/reg-sub
  :user/data
  (fn [db]
    (get-in db [:user])))

(rf/reg-sub
  :user/groups
  (fn [db]
    (get-in db [:user :groups])))

(rf/reg-event-fx
  :user.name/update
  (fn [{:keys [db]} [_ new-display-name]]
    {:fx [(http/xhrio-request db :put "/user/name"
                              [:user.name/update-success]
                              {:display-name new-display-name}
                              [:ajax.error/as-notification])]}))

(rf/reg-event-db
  :user.name/update-success
  (fn [db [_ {:keys [updated-user]}]]
    (assoc-in db [:user :names :display] (get updated-user :user.registered/display-name))))