(ns schnaq.interface.views.discussion.moderation-center
  (:require [com.fulcrologic.guardrails.core :refer [>defn-]]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.buttons :as button]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.config :as config]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.header-image :as header-image]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user.themes :as themes]
            [schnaq.links :as links]))

(defn- img-text
  "Create one icon in a grid"
  [path-to-img alt-key heading]
  [:<>
   [:img {:src path-to-img
          :alt (labels alt-key)}]
   [:h5 heading]])

(rf/reg-event-fx
 :discussion.moderation/promote-user-to-moderator
 (fn [{:keys [db]} [_ form]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:fx [(http/xhrio-request db :post "/moderation/promote-user"
                               [:discussion.moderation/send-email-success form]
                               {:recipient (oget form ["moderation-center-recipient" :value])
                                :share-hash share-hash
                                :admin-center (links/get-moderator-center-link share-hash)}
                               [:ajax.error/as-notification])]})))

(rf/reg-event-fx
 :discussion.moderation/make-read-only
 (fn [{:keys [db]} _]
   (let [{:discussion/keys [share-hash]} (get-in db [:schnaq :selected])]
     {:fx [(http/xhrio-request db :put "/discussion/manage/make-read-only"
                               [:discussion.moderation/make-read-only-success]
                               {:share-hash share-hash}
                               [:ajax.error/as-notification])]})))

(rf/reg-event-db
 :discussion.moderation/make-read-only-success
 (fn [db _]
   (update-in db [:schnaq :selected :discussion/states]
              #(distinct (conj % :discussion.state/read-only)))))

(rf/reg-event-fx
 :discussion.moderation/make-writeable
 (fn [{:keys [db]} _]
   (let [{:discussion/keys [share-hash]} (get-in db [:schnaq :selected])]
     {:fx [(http/xhrio-request db :put "/discussion/manage/make-writeable"
                               [:discussion.moderation/make-writeable-success]
                               {:share-hash share-hash}
                               [:ajax.error/as-notification])]})))

(rf/reg-event-db
 :discussion.moderation/make-writeable-success
 (fn [db _]
   (update-in db [:schnaq :selected :discussion/states]
              #(-> % set (disj :discussion.state/read-only) vec))))

(rf/reg-event-fx
 ;; Success event of deletion live in discussion - not from admin panel
 :discussion.moderation/delete-statement-success
 (fn [_ [_ statement-id return]]
   {:fx [[:dispatch [:notification/add
                     #:notification{:title (labels :schnaq.moderation.notifications/statements-deleted-title)
                                    :body (labels :schnaq.moderation.notifications/statements-deleted-lead)
                                    :context :success}]]
         [:dispatch [:discussion.delete/purge-stores statement-id return]]]}))

(rf/reg-event-db
 ;; Delete a statement-id from conclusions-list and history
 :discussion.delete/purge-stores
 (fn [db [_ statement-id return-value]]
   (let [;; User deleted their own post
         method (:method return-value)
         ;; Admin deleted multiple statements
         deleted-statements (:deleted-statements return-value)
         history (get-in db [:history :full-context])
         parent-id (get-in db [:schnaq :statements statement-id :statement/parent :db/id])
         update-history-parent-fn #(cond-> %
                                     parent-id
                                     (update-in [:schnaq :statements parent-id :meta/sub-statement-count] dec)
                                     (= statement-id (last history))
                                     (update-in [:history :full-context] (comp vec butlast)))]
     (if deleted-statements
       (update-history-parent-fn (update-in db [:schnaq :statements] #(apply dissoc % deleted-statements)))
       (if (= :deleted method)
         (update-history-parent-fn (update-in db [:schnaq :statements] dissoc statement-id))
         (assoc-in db [:schnaq :statements statement-id :statement/content] config/deleted-statement-text))))))

(rf/reg-event-fx
 :discussion.moderation/send-email-success
 (fn [_ [_ form {:keys [failed-sendings]}]]
   {:fx [[:dispatch [:notification/add
                     #:notification{:title (labels :schnaq.moderation.notifications/emails-successfully-sent-title)
                                    :body (labels :schnaq.moderation.notifications/emails-successfully-sent-body-text)
                                    :context :success}]]
         [:form/clear form]
         (when (seq failed-sendings)
           [:dispatch [:notification/add
                       #:notification{:title (labels :schnaq.moderation.notifications/sending-failed-title)
                                      :body [:<>
                                             (labels :schnaq.moderation.notifications/sending-failed-lead)
                                             [:ul
                                              (for [failed-sending failed-sendings]
                                                [:li {:key failed-sending} failed-sending])]]
                                      :context :warning
                                      :stay-visible? true}]])]}))

(defn- send-moderation-center-link
  "Send moderation center link via mail to the creator."
  []
  [:section
   [:p.lead (labels :schnaq.admin.edit.link/primer)]
   [:section.row.mb-3
    ;; elephant admin
    [:div.col-md-6
     [:div.share-link-icons
      [img-text (img-path :schnaqqifant/admin) :schnaqqifant/admin-alt-text
       (labels :schnaq.moderation.edit.link/admin)]]]
    ;; elephant edit
    [:div.col-md-6.share-link-icons
     [img-text (img-path :schnaqqifant/erase) :schnaqqifant/erase-alt-text
      (labels :schnaq.moderation.edit.link/admin-privileges)]]]
   ;; admin mail input
   (let [input-id "moderation-link-mail-address"]
     [:form.form.text-start.mb-5
      {:on-submit (fn [e]
                    (.preventDefault e)
                    (rf/dispatch [:discussion.moderation/promote-user-to-moderator
                                  (oget e [:target :elements])]))}
      [:div.mb-3
       [:label.form-label {:for input-id} (labels :schnaq.moderation.edit.link.form/label)]
       [:input.form-control.m-1.rounded-3
        {:id input-id
         :name "moderation-center-recipient"
         :auto-complete "off"
         :required true
         :placeholder (labels :schnaq.moderation.edit.link.form/placeholder)}]
       [:small.form-text.text-muted.float-end
        (labels :schnaq.moderation/addresses-privacy)]]
      [:button.btn.btn-outline-primary
       (labels :schnaq.moderation.edit.link.form/submit-button)]])])

(defn- enable-discussion-read-only
  "A Checkbox that makes the current discussion read-only or writeable."
  []
  (let [schnaq-read-only? @(rf/subscribe [:schnaq.selected/read-only?])
        dispatch (if schnaq-read-only?
                   :discussion.moderation/make-writeable
                   :discussion.moderation/make-read-only)
        pro-user? @(rf/subscribe [:user/pro?])]
    [:div {:class (when-not pro-user? "text-muted")}
     [:input.big-checkbox
      {:type :checkbox
       :id :enable-read-only?
       :disabled (not pro-user?)
       :checked schnaq-read-only?
       :on-change (fn [e] (.preventDefault e)
                    (rf/dispatch [dispatch]))}]
     [:label.form-check-label.h5.ps-1 {:for :enable-read-only?}
      (labels :schnaq.moderation.configurations.read-only/checkbox)]
     [:p (labels :schnaq.moderation.configurations.read-only/explanation)]]))

(defn- disable-pro-con []
  (let [pro-con-disabled? @(rf/subscribe [:schnaq.selected/pro-con?])
        pro-user? @(rf/subscribe [:user/pro?])]
    [:div {:class (when-not pro-user? "text-muted")}
     [:input.big-checkbox
      {:type :checkbox
       :id :disable-pro-con-checkbox?
       :disabled (not pro-user?)
       :checked pro-con-disabled?
       :on-change
       (fn [e]
         (.preventDefault e)
         (rf/dispatch [:schnaq.moderation/disable-pro-con (not pro-con-disabled?)]))}]
     [:label.form-check-label.h5.ps-1 {:for :disable-pro-con-checkbox?}
      (labels :schnaq.moderation.configurations.disable-pro-con/label)]
     [:p (labels :schnaq.moderation.configurations.disable-pro-con/explanation)]]))

(defn- only-moderators-mark-setting []
  (let [mods-mark-only? @(rf/subscribe [:schnaq.selected.qa/mods-mark-only?])
        pro-user? @(rf/subscribe [:user/pro?])]
    [:div {:class (when-not pro-user? "text-muted")}
     [:input.big-checkbox
      {:type :checkbox
       :disabled (not pro-user?)
       :id :only-moderators-mark-checkbox
       :checked mods-mark-only?
       :on-change
       (fn [e]
         (.preventDefault e)
         (rf/dispatch [:schnaq.moderation.qa/mods-mark-only! (not mods-mark-only?)]))}]
     [:label.form-check-label.h5.ps-1 {:for :only-moderators-mark-checkbox}
      (labels :schnaq.moderation.configurations.mods-mark-only/label)]
     [:p (labels :schnaq.moderation.configurations.mods-mark-only/explanation)]]))

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :schnaq.moderation/add-state
 (fn [{:keys [db]} [_ state]]
   (let [{:keys [share-hash]} (get-in db [:current-route :path-params])]
     {:db (update-in db [:schnaq :selected :discussion/states] conj state)
      :fx [(http/xhrio-request db :put "/discussion/manage/state"
                               [:ajax.error/to-console]
                               {:state state
                                :share-hash share-hash}
                               [:ajax.error/as-notification])]})))
(rf/reg-event-fx
 :schnaq.moderation/delete-state
 (fn [{:keys [db]} [_ state]]
   (let [{:keys [share-hash]} (get-in db [:current-route :path-params])]
     {:db (update-in db [:schnaq :selected :discussion/states] disj state)
      :fx [(http/xhrio-request db :delete "/discussion/manage/state"
                               [:ajax.error/to-console]
                               {:state state
                                :share-hash share-hash}
                               [:ajax.error/as-notification])]})))

(rf/reg-sub
 :schnaq.selected/pro-con?
 :<- [:schnaq/selected]
 (fn [selected-schnaq _ _]
   (not (nil? (some #{:discussion.state/disable-pro-con} (:discussion/states selected-schnaq))))))

(rf/reg-event-fx
 :schnaq.moderation/disable-pro-con
 (fn [{:keys [db]} [_ disable-pro-con?]]
   (let [{:keys [share-hash]} (get-in db [:current-route :path-params])]
     {:fx [(http/xhrio-request db :put "/discussion/manage/disable-pro-con"
                               [:schnaq.moderation/disable-pro-con-success disable-pro-con?]
                               {:disable-pro-con? disable-pro-con?
                                :share-hash share-hash}
                               [:ajax.error/as-notification])]})))

(rf/reg-event-db
 :schnaq.moderation/disable-pro-con-success
 (fn [db [_ disable-pro-con?]]
   (if disable-pro-con?
     (update-in db [:schnaq :selected :discussion/states]
                #(distinct (conj % :discussion.state/disable-pro-con)))
     (update-in db [:schnaq :selected :discussion/states]
                #(-> % set (disj :discussion.state/disable-pro-con) vec)))))

(rf/reg-sub
 :schnaq.selected.qa/mods-mark-only?
 :<- [:schnaq/selected]
 (fn [selected-schnaq _ _]
   (not (nil? (some #{:discussion.state.qa/mark-as-moderators-only} (:discussion/states selected-schnaq))))))

(rf/reg-event-fx
 :schnaq.moderation.qa/mods-mark-only!
 (fn [{:keys [db]} [_ mods-mark-only?]]
   {:fx [(http/xhrio-request db :put "/discussion/manage/mods-mark-only"
                             [:schnaq.moderation.qa/mods-mark-only-success mods-mark-only?]
                             {:mods-mark-only? mods-mark-only?
                              :share-hash (get-in db [:schnaq :selected :discussion/share-hash])}
                             [:ajax.error/as-notification])]}))

(rf/reg-event-db
 :schnaq.moderation.qa/mods-mark-only-success
 (fn [db [_ mods-mark-only?]]
   (if mods-mark-only?
     (update-in db [:schnaq :selected :discussion/states]
                #(distinct (conj % :discussion.state.qa/mark-as-moderators-only)))
     (update-in db [:schnaq :selected :discussion/states]
                #(-> % set (disj :discussion.state.qa/mark-as-moderators-only) vec)))))

(rf/reg-event-fx
 :schnaq.moderation.focus/entity
 (fn [{:keys [db]} [_ entity-id]]
   {:fx [(http/xhrio-request db :put "/discussion/manage/focus" [:schnaq.moderation.focus.entity/success]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])
                              :entity-id entity-id})]}))

(rf/reg-event-fx
 :schnaq.moderation.focus.entity/success
 (fn [_ _]
   {:fx [[:dispatch [:notification/add
                     #:notification{:title (labels :schnaq.admin.focus.notification/title)
                                    :body (labels :schnaq.admin.focus.notification/body)
                                    :context :success}]]]}))

;; -----------------------------------------------------------------------------

(defn- discussion-settings
  "List all possible discussion settings."
  []
  [:<>
   [:h4 (labels :schnaq.moderation.configurations/heading)]
   [only-moderators-mark-setting]
   [enable-discussion-read-only]
   [disable-pro-con]])

(>defn- moderate-discussion
  "Settings for the discussion."
  []
  [:ret :re-frame/component]
  (if @(rf/subscribe [:user/pro?])
    [:<>
     [themes/assign-theme-to-schnaq]
     [:hr.my-5]
     [discussion-settings]
     [:hr.my-5]
     [header-image/image-url-input]]
    [:div.pt-1
     [:p.h4 [icon :lock] " " (labels :schnaq.moderation.configurations.mods-mark-only/beta)]
     [button/upgrade]
     [:div.border.border-danger.p-3.mt-4
      [discussion-settings]
      [:div.pt-4
       [header-image/image-url-input]]]]))

(defn- moderation-tabs
  "Share link and invite via mail in a tabbed view."
  []
  [common/tab-builder
   "invite-participants"
   ;; Manage discussion settings
   {:link (labels :schnaq.moderation.edit/administrate)
    :view [moderate-discussion]}
   ;; admin access via mail
   {:link (labels :schnaq.moderation.edit.link/header)
    :view [:div.text-center [send-moderation-center-link]]}])

;; -----------------------------------------------------------------------------

(defn- moderation-center
  "This view is presented to the user after they have created a new meeting."
  []
  (let [{:discussion/keys [share-hash title]} @(rf/subscribe [:schnaq/selected])]
    ;; display admin center
    [pages/with-discussion-header
     {:page/heading (labels :schnaq.moderation/heading)
      :page/subheading (gstring/format (labels :schnaq.moderation/subheading) title)
      :condition/needs-moderator? true}
     [:div.container.px-3.px-md-5.py-3
      [moderation-tabs]
      [:div.text-center
       [:div.pb-5.mt-3]
       ;; stop image and hint to copy the link
       [:div.single-image [:img {:src (img-path :schnaqqifant/stop)
                                 :alt (labels :schnaqqifant/stop-alt-text)}]]
       [:h4.mb-4 (labels :schnaqs/continue-with-schnaq-after-creation)]
       [:a.btn.btn-primary.btn-lg.center-block.mb-5
        {:role "button"
         :href (navigation/href :routes.schnaq/start {:share-hash share-hash})}
        (labels :schnaqs/continue-to-schnaq-button)]]]]))

(defn moderation-center-view []
  [moderation-center])
