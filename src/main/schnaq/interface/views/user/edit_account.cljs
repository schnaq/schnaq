(ns schnaq.interface.views.user.edit-account
  (:require [clojure.string :as string]
            [oops.core :refer [oget+]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.config :as config]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.files :as files]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.hub.common :as hub-common]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user.settings :as settings]
            [schnaq.interface.views.user.subscription :as user-subscription]))

(defn- avatar-input [input-id]
  (let [user @(rf/subscribe [:user/current])
        profile-picture (get-in user [:profile-picture :display])
        temporary-picture (get-in user [:profile-picture :temporary :content])
        preview-image (or temporary-picture profile-picture)]
    [:div.d-flex.me-4
     [:div.d-flex.avatar-image
      [common/avatar :size 80 :user #:user.registered{:profile-picture preview-image
                                                      :display-name (get-in user [:names :display])}]]
     [:div.mt-auto
      (if temporary-picture
        ;; delete temporary button
        [:button.btn.btn-primary.change-profile-pic-button
         {:on-click (fn [e] (.preventDefault e)
                      (rf/dispatch [:user.picture/reset]))}
         [icon :cross]]
        ;; upload temporary button
        [:label.form-label.btn.btn-light.change-profile-pic-button
         [icon :camera]
         [:input {:id input-id
                  :accept (string/join "," shared-config/allowed-mime-types-images)
                  :type "file"
                  :on-change (fn [event] (files/store-temporary-file
                                          event [:user :profile-picture :temporary]))
                  :hidden true}]])]]))

(defn- change-user-info []
  (let [display-name @(rf/subscribe [:user/display-name])
        input-id :user-display-name
        pic-input-id :user-profile-pic]
    [:<>
     (labels :user.settings/change-name)
     [:form.my-4
      {:on-submit (fn [e]
                    (let [new-display-name (oget+ e [:target :elements input-id :value])]
                      (.preventDefault e)
                      (rf/dispatch [:user.picture/update])
                      (rf/dispatch [:user.name/update new-display-name])))}
      [:div.d-flex.flex-row
       [avatar-input pic-input-id]
       [common/form-input {:id input-id
                           :default-value display-name
                           :css "font-150"}]]
      [:div.row.pt-5
       [:div.col.text-start.my-3
        [:a.btn.btn-lg.btn-outline-secondary {:href config/keycloak-profile-page}
         (labels :user/profile-settings)]]
       [:div.col.text-end.my-3
        [:button.btn.btn-lg.btn-outline-primary {:type :submit}
         (labels :user.settings.button/change-account-information)]]]]]))

(defn- content []
  [pages/settings-panel
   (labels :user.settings/header)
   [:<>
    [change-user-info]
    [user-subscription/stripe-management]
    [hub-common/list-hubs-with-heading]]])

(defn view []
  [settings/user-view
   :user/edit-account
   [content]])

;; ----------------------------------------------------------------------------

(rf/reg-event-db
 :user.name/store
 (fn [db [_ username]]
   (-> db
       (assoc-in [:user :names :display] username)
       (assoc-in [:user :entity :user.registered/display-name] username))))

(rf/reg-event-fx
 :user.name/update
 (fn [{:keys [db]} [_ new-display-name]]
   {:fx [(http/xhrio-request db :put "/user/name"
                             [:user.name/update-success]
                             {:display-name new-display-name}
                             [:ajax.error/as-notification])
         [:dispatch [:user.name/store new-display-name]]]}))

(rf/reg-event-fx
 :user.name/update-success
 (fn [_ [_ {:keys [updated-user]}]]
   (let [username (:user.registered/display-name updated-user)]
     {:fx [[:dispatch [:user.name/store username]]]})))

(rf/reg-event-fx
 :user.picture/update
 (fn [{:keys [db]} _]
   (when-let [new-profile-picture (get-in db [:user :profile-picture :temporary])]
     {:fx [(http/xhrio-request db :put "/user/picture"
                               [:user.profile-picture/update-success]
                               {:image new-profile-picture}
                               [:file.store/error])]})))

(rf/reg-event-db
 :user.picture/reset
 (fn [db _]
   (update-in db [:user :profile-picture] dissoc :temporary)))

(rf/reg-event-fx
 :user.profile-picture/update-success
 (fn [{:keys [db]} [_ {:keys [updated-user]}]]
   {:db (assoc-in db [:user :profile-picture :display]
                  (:user.registered/profile-picture updated-user))
    :fx [[:dispatch [:notification/add
                     #:notification{:title (labels :user.settings.profile-picture-title/success)
                                    :body (labels :user.settings.profile-picture-body/success)
                                    :context :success}]]]}))
