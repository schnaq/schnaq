(ns schnaq.interface.views.hub.settings
  (:require [clojure.string :as string]
            [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [oops.core :refer [oget+]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.text.display-data :refer [fa labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.feed.overview :as feed]
            [schnaq.interface.views.hub.common :as hub-common]
            [schnaq.interface.views.hub.overview :as hubs]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user.image-upload :as image]))

(defn- logo-input [input-id]
  (let [hub @(rf/subscribe [:hub/current])
        name (:hub/keycloak-name hub)
        hub-logo (:hub/logo hub)
        temporary-logo (get-in hub [:logo-temporary :content])
        preview-image (or temporary-logo hub-logo)]
    [:div.d-flex.mr-4
     [:div.d-flex.avatar-image
      [hub-common/logo preview-image name 80]]
     [:div.mt-auto
      (if temporary-logo
        ;; delete temporary button
        [:button.btn.btn-primary.change-profile-pic-button
         {:on-click (fn [e] (js-wrap/prevent-default e)
                      (rf/dispatch [:hub.logo/reset hub]))}
         [:i.fas {:class (fa :cross)}]]
        ;; upload temporary button
        [:label.btn.btn-light.change-profile-pic-button
         [:i.fas {:class (fa :camera)}]
         [:input {:id input-id
                  :accept (string/join "," shared-config/allowed-mime-types)
                  :type "file"
                  :on-change (fn [event] (image/store-temporary-profile-picture
                                           event [:hubs name :logo-temporary]))
                  :hidden true}]])]]))

(rf/reg-event-db
  :hub.logo/reset
  (fn [db [_ {:hub/keys [name]}]]
    (update-in db [:hubs name] dissoc :logo-temporary)))


(defn- settings-body []
  (let [{:hub/keys [name]} @(rf/subscribe [:hub/current])
        input-id :change-hub-name-input
        logo-input-id :hub-logo-pic]
    [:<>
     [pages/settings-panel
      (labels :hub.settings/change-name)
      [:form
       {:on-submit (fn [e]
                     (let [new-hub-name (oget+ e [:target :elements input-id :value])]
                       (js-wrap/prevent-default e)
                       (rf/dispatch [:hub.logo/update])
                       (rf/dispatch [:hub.name/update new-hub-name])))}
       [:div.d-flex.flex-row
        [logo-input logo-input-id]
        [common/form-input {:id input-id
                            :default-value name
                            :css "font-150"}]]
       [:div.text-right.my-3
        [:button.btn.btn-lg.btn-outline-primary.rounded-2 {:type :submit}
         (labels :hub.settings/save)]]]]
     [pages/settings-panel
      (labels :hub.members.add.form/title)
      [:form
       {:on-submit (fn [e]
                     (let [new-member-mail (oget+ e [:target :elements :add-member-input :value])]
                       (js-wrap/prevent-default e)
                       (rf/dispatch [:hub.members/add new-member-mail])))}
       [:div.d-flex.flex-row
        [common/form-input {:id :add-member-input
                            :placeholder "contact@email.com"
                            :css "font-150"}]]
       [:div.text-right.my-3
        [:button.btn.btn-lg.btn-outline-primary.rounded-2 {:type :submit}
         (labels :hub.members.add.form/button)]]]]]))

(>defn- settings-view
  "Show the CRUD view for a hub."
  []
  [:ret vector?]
  (let [keycloak-name (get-in @(rf/subscribe [:navigation/current-route])
                              [:path-params :keycloak-name])]
    [pages/three-column-layout
     {:page/heading (gstring/format (labels :hub/heading) keycloak-name)}
     [feed/feed-navigation]
     [settings-body]
     [:<>
      [hubs/hub-panel]
      [hubs/member-list]]]))

(defn settings
  "Renders all schnaqs belonging to the hub."
  []
  [settings-view])

(rf/reg-event-fx
  :hub.members/add
  (fn [{:keys [db]} [_ new-member-mail]]
    (let [keycloak-name (get-in db [:current-route :path-params :keycloak-name])]
      {:fx [(http/xhrio-request
              db :post
              (gstring/format "/hub/%s/add-member" keycloak-name)
              [:hub.members.add/success]
              {:new-member-mail new-member-mail})]})))

(rf/reg-event-fx
  :hub.members.add/success
  (fn [_ [_ {:keys [status]}]]
    (let [[body-title body-text context]
          (case status
            :user-added [:hub.members.add.result.success/title
                         :hub.members.add.result.success/body
                         :success]
            :user-not-registered [:hub.members.add.result.error/title
                                  :hub.members.add.result.error/unregistered-user
                                  :warning]
            :error-adding-user [:hub.members.add.result.error/title
                                :hub.members.add.result.error/generic-error
                                :danger])]
      {:fx [[:dispatch
             [:notification/add
              #:notification{:title (labels body-title)
                             :body (labels body-text)
                             :context context}]]]})))

(rf/reg-event-fx
  :hub.name/update
  (fn [{:keys [db]} [_ new-hub-name]]
    (let [keycloak-name (get-in db [:current-route :path-params :keycloak-name])]
      {:fx [(http/xhrio-request
              db :put (gstring/format "/hub/%s/name" keycloak-name)
              [:hub.name/update-success]
              {:new-hub-name new-hub-name}
              [:ajax.error/as-notification])]})))

(rf/reg-event-fx
  :hub.name/update-success
  (fn [{:keys [db]} [_ {:keys [hub]}]]
    {:db (assoc-in db [:hubs (:hub/keycloak-name hub)] hub)
     :fx [[:dispatch
           [:notification/add
            #:notification{:title (labels :hub.settings.name/updated-title)
                           :body (labels :hub.settings.name/updated-body)
                           :context :success}]]]}))

(rf/reg-event-fx
  :hub.logo/update
  (fn [{:keys [db]}]
    (let [keycloak-name (get-in db [:current-route :path-params :keycloak-name])]
      (when-let [temporary-hub-logo-url (get-in db [:hubs keycloak-name :logo-temporary])]
        {:fx [(http/xhrio-request db :put (gstring/format "/hub/%s/logo" keycloak-name)
                                  [:hub.logo/update-success]
                                  {:image temporary-hub-logo-url}
                                  [:hub.logo/update-error])]}))))

(rf/reg-event-fx
  :hub.logo/update-success
  (fn [{:keys [db]} [_ {:keys [hub]}]]
    (let [keycloak-name (get-in db [:current-route :path-params :keycloak-name])]
      {:db (assoc-in db [:hubs keycloak-name :hub/logo] (:hub/logo hub))
       :fx [[:dispatch [:notification/add
                        #:notification{:title (labels :hub.settings.update-logo-title/success)
                                       :body (labels :hub.settings.update-logo-body/success)
                                       :context :success}]]]})))

(rf/reg-event-fx
  :hub.logo/update-error
  (fn [{:keys [db]} [_ {:keys [response]}]]
    (let [mime-types (string/join ", " shared-config/allowed-mime-types)
          error-message (case (:error response)
                          :scaling (labels :user.settings.profile-picture.errors/scaling)
                          :invalid-file-type (gstring/format (labels :user.settings.profile-picture.errors/invalid-file-type) mime-types)
                          (labels :user.settings.profile-picture.errors/default))]
      {:db (assoc-in db [:user :profile-picture :temporary] nil)
       :fx [[:dispatch [:notification/add
                        #:notification{:title (labels :user.settings.profile-picture-title/error)
                                       :body error-message
                                       :context :danger}]]]})))