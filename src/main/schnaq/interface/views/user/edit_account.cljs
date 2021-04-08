(ns schnaq.interface.views.user.edit-account
  (:require [oops.core :refer [oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.config :as config]
            [schnaq.interface.text.display-data :refer [fa labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.hub.common :as hub]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user.image-upload :as image]
            [schnaq.interface.views.user.settings :as settings]))

(defn- avatar-input [input-id]
  (let [user @(rf/subscribe [:user/current])]
    (let [profile-picture (get-in user [:profile-picture :display])
          temporary-picture (get-in user [:profile-picture :temporary :content])
          preview-image (or temporary-picture profile-picture)]
      [:div.d-flex.mr-4
       [:div.d-flex.avatar-image
        [common/avatar #:user.registered{:profile-picture preview-image
                                         :display-name (get-in user [:names :display])} 80]]
       [:div.mt-auto
        (if temporary-picture
          ;; delete temporary button
          [:button.btn.btn-dark.change-profile-pic-button
           {:on-click (fn [e] (js-wrap/prevent-default e)
                        (rf/dispatch [:user.picture/reset]))}
           [:i.fas.mr-1 {:class (fa :cross)}]]
          ;; upload temporary button
          [:label.btn.btn-light.change-profile-pic-button
           [:i.fas.mr-1 {:class (fa :camera)}]
           [:input {:id input-id
                    :accept "image/x-png,image/jpeg,image/*"
                    :type "file"
                    :on-change (fn [event] (image/store-temporary-profile-picture event))
                    :hidden true}]])]])))

(defn- change-user-info []
  (let [display-name @(rf/subscribe [:user/display-name])
        input-id :user-display-name
        pic-input-id :user-profile-pic]
    [:<>
     (labels :user.settings/change-name)
     [:form.my-4
      {:on-submit (fn [e]
                    (let [new-display-name (oget+ e [:target :elements input-id :value])
                          new-profile-picture (oget+ e [:target :elements pic-input-id :value])]
                      (js-wrap/prevent-default e)
                      (when new-profile-picture

                        (rf/dispatch [:user.picture/update new-profile-picture]))
                      (rf/dispatch [:user.name/update new-display-name])))}
      [:div.d-flex.flex-row
       [avatar-input pic-input-id]
       [common/form-input {:id input-id
                           :default-value display-name
                           :css "font-150"}]]
      [:div.row.pt-5
       [:div.col.text-left.my-3
        [:a.btn.btn-lg.btn-outline-secondary.rounded-2 {:href config/keycloak-profile-page}
         (labels :user.keycloak-settings)]]
       [:div.col.text-right.my-3
        [:button.btn.btn-lg.btn-outline-primary.rounded-2 {:type :submit}
         (labels :user.settings.button/change-account-information)]]]]]))

(defn- show-hubs []
  (let [hubs @(rf/subscribe [:hubs/all])]
    [:<>
     (labels :user.settings.hubs/show)
     (if (empty? hubs)
       (labels :user.settings.hubs/empty)
       [hub/hub-list hubs])]))

(defn- content []
  [pages/settings-panel
   (labels :user.settings/header)
   [:<>
    [change-user-info]
    [:hr.my-5]
    [show-hubs]]])

(defn view []
  [settings/user-view
   :user/edit-account
   [content]])


;; ----------------------------------------------------------------------------

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
    (assoc-in db [:user :names :display]
              (:user.registered/display-name updated-user))))

(rf/reg-event-fx
  :user.picture/update
  (fn [{:keys [db]} _]
    (when-let [new-profile-picture-url (get-in db [:user :profile-picture :temporary])]
      {:fx [(http/xhrio-request db :put "/user/picture"
                                [:user.profile-picture/update-success]
                                {:image new-profile-picture-url}
                                [:ajax.error/as-notification])]})))

(rf/reg-event-db
  :user.picture/reset
  (fn [db _]
    (update-in db [:user :profile-picture] dissoc :temporary)))

(rf/reg-event-db
  :user.profile-picture/update-success
  (fn [db [_ {:keys [updated-user]}]]
    (assoc-in db [:user :profile-picture :display]
              (:user.registered/profile-picture updated-user))))

