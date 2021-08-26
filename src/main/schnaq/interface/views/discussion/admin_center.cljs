(ns schnaq.interface.views.discussion.admin-center
  (:require [clojure.string :as string]
            [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [hodgepodge.core :refer [local-storage]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [schnaq.interface.config :as config]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.utils.clipboard :as clipboard]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.localstorage :as ls]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.header-image :as header-image]
            [schnaq.interface.views.notifications :refer [notify!]]
            [schnaq.interface.views.pages :as pages]
            [schnaq.links :as links]))

(defn- copy-link-form
  "A form that displays the link the user can copy. Form is read-only."
  [create-link-fn id-extra]
  (reagent/create-class
    {:component-did-mount
     (fn [_]
       (js-wrap/tooltip (str "#meeting-link-form-" id-extra)))
     :component-will-unmount
     (fn [_]
       (js-wrap/tooltip (str "#meeting-link-form-" id-extra) "disable")
       (js-wrap/tooltip (str "#meeting-link-form-" id-extra) "dispose"))
     :reagent-render
     (fn []
       (let [display-content (create-link-fn (-> @(rf/subscribe [:navigation/current-route])
                                                 :path-params :share-hash))
             meeting-link-id (str "meeting-link" id-extra)]
         [:div.pb-4
          [:form.form.create-meeting-form.d-flex
           {:id (str "meeting-link-form-" id-extra)
            :on-click (fn [e]
                        (js-wrap/prevent-default e)
                        (clipboard/copy-to-clipboard! display-content)
                        (notify! (labels :schnaq/link-copied-heading)
                                 (labels :schnaq/link-copied-success)
                                 :info
                                 false))
            :data-toggle "tooltip"
            :data-placement "bottom"
            :title (labels :schnaq/copy-link-tooltip)}
           [:input.form-control.form-round.copy-link-form.clickable-no-hover
            {:id meeting-link-id
             :type "text"
             :value display-content
             :readOnly true}]
           [:label.clickable-no-hover.align-right.ml-4.d-flex.justify-content-center {:for meeting-link-id}
            [:div {:class (str "m-auto far fa-lg " (fa :copy))}]]]]))}))

(defn- img-text
  "Create one icon in a grid"
  [path-to-img heading]
  [:<>
   [:img {:src path-to-img}]
   [:h5 heading]])

(defn- educate-element []
  [:div.row.mb-3
   [:div.col-12.col-md-6.share-link-icons
    [img-text (img-path :schnaqqifant/share)
     (labels :schnaq/educate-on-link-text)]]
   [:div.col-12.col-md-6.share-link-icons
    [img-text (img-path :schnaqqifant/talk)
     (labels :schnaq/educate-on-link-text-subtitle)]]])

;; -----------------------------------------------------------------------------

(>defn- invite-participants-form
  "A form which allows the sending of the invitation-link to several participants via E-Mail."
  []
  [:ret :re-frame/component]
  (let [input-id "participant-email-addresses"]
    [:<>
     [:h4.mt-4 (labels :schnaq.admin/send-invites-heading)]
     [:form.form.text-left.mb-5
      {:on-submit (fn [e]
                    (js-wrap/prevent-default e)
                    (rf/dispatch [:discussion.admin/send-email-invites
                                  (oget e [:target :elements])]))}
      [:div.form-group
       [:label.m-1 {:for input-id} (labels :schnaq.admin/addresses-label)]
       [:textarea.form-control.m-1.rounded-3
        {:id input-id
         :name "participant-addresses" :wrap "soft" :rows 3
         :auto-complete "off"
         :required true
         :placeholder (labels :schnaq.admin/addresses-placeholder)}]
       [:small.form-text.text-muted.float-right
        (labels :schnaq.admin/addresses-privacy)]]
      [:button.btn.btn-outline-primary
       (labels :schnaq.admin/send-invites-button-text)]]]))

(rf/reg-event-fx
  :discussion.admin/send-admin-center-link
  (fn [{:keys [db]} [_ form]]
    (let [current-route (:current-route db)
          {:keys [share-hash edit-hash]} (:path-params current-route)]
      {:fx [(http/xhrio-request db :post "/emails/send-admin-center-link" [:discussion.admin/send-email-success form]
                                {:recipient (oget form ["admin-center-recipient" :value])
                                 :share-hash share-hash
                                 :edit-hash edit-hash
                                 :admin-center (links/get-admin-link share-hash edit-hash)}
                                [:ajax.error/as-notification])]})))

(rf/reg-event-fx
  :discussion.admin/delete-statements
  (fn [{:keys [db]} [_ form]]
    (let [raw-statements (oget form ["statement-ids" :value])
          statement-ids (map #(js/parseInt %) (string/split raw-statements #"\s+"))
          current-route (:current-route db)
          {:keys [share-hash edit-hash]} (:path-params current-route)]
      {:fx [(http/xhrio-request db :delete "/discussion/statements/delete"
                                [:discussion.admin/delete-statements-success form]
                                {:statement-ids statement-ids
                                 :share-hash share-hash
                                 :edit-hash edit-hash}
                                [:ajax.error/as-notification])]})))

(rf/reg-event-fx
  :discussion.admin/make-read-only
  (fn [{:keys [db]} _]
    (let [current-route (:current-route db)
          {:keys [share-hash edit-hash]} (:path-params current-route)]
      {:fx [(http/xhrio-request db :put "/discussion/manage/make-read-only" [:discussion.admin/make-read-only-success]
                                {:share-hash share-hash
                                 :edit-hash edit-hash}
                                [:ajax.error/as-notification])]})))

(rf/reg-event-db
  :discussion.admin/make-read-only-success
  (fn [db _]
    (update-in db [:schnaq :selected :discussion/states]
               #(distinct (conj % :discussion.state/read-only)))))

(rf/reg-event-fx
  :discussion.admin/make-writeable
  (fn [{:keys [db]} _]
    (let [current-route (:current-route db)
          {:keys [share-hash edit-hash]} (:path-params current-route)]
      {:fx [(http/xhrio-request db :put "/discussion/manage/make-writeable" [:discussion.admin/make-writeable-success]
                                {:share-hash share-hash
                                 :edit-hash edit-hash}
                                [:ajax.error/as-notification])]})))

(rf/reg-event-db
  :discussion.admin/make-writeable-success
  (fn [db _]
    (update-in db [:schnaq :selected :discussion/states]
               #(-> % set (disj :discussion.state/read-only) vec))))

(rf/reg-event-fx
  ;; Deletion success from admin center
  :discussion.admin/delete-statements-success
  (fn [_ [_ form _return]]
    {:fx [[:dispatch [:notification/add
                      #:notification{:title (labels :schnaq.admin.notifications/statements-deleted-title)
                                     :body (labels :schnaq.admin.notifications/statements-deleted-lead)
                                     :context :success}]]
          [:form/clear form]]}))

(rf/reg-event-fx
  :discussion.delete/statement
  (fn [{:keys [db]} [_ statement-id edit-hash]]
    (let [share-hash (get-in db [:current-route :path-params :share-hash])]
      {:fx [(http/xhrio-request db :delete "/discussion/statements/delete"
                                [:discussion.admin/delete-statement-success statement-id]
                                {:statement-ids [statement-id]
                                 :share-hash share-hash
                                 :edit-hash edit-hash}
                                [:ajax.error/as-notification])]})))

(rf/reg-event-fx
  ;; Success event of deletion live in discussion - not from admin panel
  :discussion.admin/delete-statement-success
  (fn [_ [_ statement-id return]]
    {:fx [[:dispatch [:notification/add
                      #:notification{:title (labels :schnaq.admin.notifications/statements-deleted-title)
                                     :body (labels :schnaq.admin.notifications/statements-deleted-lead)
                                     :context :success}]]
          [:dispatch [:discussion.delete/purge-stores statement-id return]]]}))

(rf/reg-event-db
  ;; Delete a statement-id from conclusions-list, history and carousels
  :discussion.delete/purge-stores
  (fn [db [_ statement-id return-value]]
    (let [mark-fn (fn [coll]
                    (mapv #(if (= statement-id (:db/id %))
                             (assoc % :statement/content config/deleted-statement-text)
                             %)
                          coll))
          delete-fn (fn [coll] (remove #(= statement-id (:db/id %)) coll))
          mark-starting-fn #(if (= (:db/id %) statement-id)
                              (assoc % :statement/content config/deleted-statement-text)
                              %)
          method (or (:method return-value) (first (:methods return-value)))]
      (if (= :deleted method)
        (-> db
            (update-in [:discussion :conclusion :selected] mark-starting-fn)
            (update-in [:discussion :premises :current] delete-fn))
        (-> db
            (update-in [:discussion :conclusion :selected] mark-starting-fn)
            (update-in [:discussion :premises :current] mark-fn)
            (update-in [:history :full-context] mark-fn))))))

(rf/reg-event-fx
  :discussion.admin/send-email-invites
  (fn [{:keys [db]} [_ form]]
    (let [raw-emails (oget form ["participant-addresses" :value])
          recipients (string/split raw-emails #"\s+")
          current-route (:current-route db)
          {:keys [share-hash edit-hash]} (:path-params current-route)]
      {:fx [(http/xhrio-request db :post "/emails/send-invites" [:discussion.admin/send-email-success form]
                                {:recipients recipients
                                 :share-hash share-hash
                                 :edit-hash edit-hash
                                 :share-link (links/get-share-link share-hash)}
                                [:ajax.error/as-notification])]})))

(rf/reg-event-fx
  :discussion.admin/send-email-success
  (fn [_ [_ form {:keys [failed-sendings]}]]
    {:fx [[:dispatch [:notification/add
                      #:notification{:title (labels :schnaq.admin.notifications/emails-successfully-sent-title)
                                     :body (labels :schnaq.admin.notifications/emails-successfully-sent-body-text)
                                     :context :success}]]
          [:form/clear form]
          (when (seq failed-sendings)
            [:dispatch [:notification/add
                        #:notification{:title (labels :schnaq.admin.notifications/sending-failed-title)
                                       :body [:<>
                                              (labels :schnaq.admin.notifications/sending-failed-lead)
                                              [:ul
                                               (for [failed-sending failed-sendings]
                                                 [:li {:key failed-sending} failed-sending])]]
                                       :context :warning
                                       :stay-visible? true}]])]}))

(defn- send-admin-center-link
  "Send admin link via mail to the creator."
  []
  [:section
   [:p.lead (labels :schnaq.admin.edit.link/primer)]
   [:section.row.mb-3
    ;; elephant admin
    [:div.col-md-6
     [:div.share-link-icons
      [img-text (img-path :schnaqqifant/admin)
       (labels :schnaq.admin.edit.link/admin)]]]
    ;; elephant edit
    [:div.col-md-6.share-link-icons
     [img-text (img-path :schnaqqifant/erase)
      (labels :schnaq.admin.edit.link/admin-privileges)]]]
   ;; admin mail input
   (let [input-id "admin-link-mail-address"]
     [:form.form.text-left.mb-5
      {:on-submit (fn [e]
                    (js-wrap/prevent-default e)
                    (rf/dispatch [:discussion.admin/send-admin-center-link
                                  (oget e [:target :elements])]))}
      [:div.form-group
       [:label {:for input-id} (labels :schnaq.admin.edit.link.form/label)]
       [:input.form-control.m-1.rounded-3
        {:id input-id
         :name "admin-center-recipient"
         :auto-complete "off"
         :required true
         :placeholder (labels :schnaq.admin.edit.link.form/placeholder)}]
       [:small.form-text.text-muted.float-right
        (labels :schnaq.admin/addresses-privacy)]]
      [:button.btn.btn-outline-primary
       (labels :schnaq.admin.edit.link.form/submit-button)]])])

(defn- enable-discussion-read-only
  "A Checkbox that makes the current discussion read-only or writeable."
  []
  (let [schnaq-read-only? @(rf/subscribe [:schnaq.selected/read-only?])
        dispatch (if schnaq-read-only? :discussion.admin/make-writeable
                                       :discussion.admin/make-read-only)
        checked (if schnaq-read-only? "checked" "")]
    [:div.text-left
     [:div.mb-2
      [:input.big-checkbox
       {:type :checkbox
        :id :enable-read-only?
        :checked checked
        :on-change (fn [e] (js-wrap/prevent-default e)
                     (rf/dispatch [dispatch]))}]
      [:label.form-check-label.display-6.pl-1 {:for :enable-read-only?}
       (labels :schnaq.admin.configurations.read-only/checkbox)]]
     [:span (labels :schnaq.admin.configurations.read-only/explanation)]]))

(defn- disable-pro-con []
  (let [pro-con-disabled? @(rf/subscribe [:schnaq.selected/pro-con?])
        checked (if pro-con-disabled? "checked" "")]
    [:div.text-left
     [:div.mb-2
      [:input.big-checkbox
       {:type :checkbox
        :id :disable-pro-con-checkbox?
        :checked checked
        :on-change
        (fn [e]
          (js-wrap/prevent-default e)
          (rf/dispatch [:schnaq.admin/disable-pro-con (not pro-con-disabled?)]))}]
      [:label.form-check-label.display-6.pl-1 {:for :disable-pro-con-checkbox?}
       (labels :schnaq.admin.configurations.disable-pro-con/label)]]
     [:span (labels :schnaq.admin.configurations.disable-pro-con/explanation)]]))

(rf/reg-sub
  :schnaq.selected/pro-con?
  (fn [_ _]
    (rf/subscribe [:schnaq/selected]))
  (fn [selected-schnaq _ _]
    (not (nil? (some #{:discussion.state/disable-pro-con} (:discussion/states selected-schnaq))))))

(rf/reg-event-fx
  :schnaq.admin/disable-pro-con
  (fn [{:keys [db]} [_ disable-pro-con?]]
    (let [current-route (:current-route db)
          {:keys [share-hash edit-hash]} (:path-params current-route)]
      {:fx [(http/xhrio-request db :put "/discussion/manage/disable-pro-con"
                                [:schnaq.admin/disable-pro-con-success disable-pro-con?]
                                {:disable-pro-con? disable-pro-con?
                                 :share-hash share-hash
                                 :edit-hash edit-hash}
                                [:ajax.error/as-notification])]})))

(rf/reg-event-db
  :schnaq.admin/disable-pro-con-success
  (fn [db [_ disable-pro-con?]]
    (if disable-pro-con?
      (update-in db [:schnaq :selected :discussion/states]
                 #(distinct (conj % :discussion.state/disable-pro-con)))
      (update-in db [:schnaq :selected :discussion/states]
                 #(-> % set (disj :discussion.state/disable-pro-con) vec)))))

(>defn- administrate-discussion
  "A form which allows removing single statements from the discussion."
  []
  [:ret :re-frame/component]
  [:<>
   [header-image/image-url-input]
   [:div.text-left.my-5
    [:h4.mt-4 (labels :schnaq.admin.configurations/heading)]]
   [:div.row
    [:div.col [enable-discussion-read-only]]
    [:div.col [disable-pro-con]]]])

(defn- invite-participants-tabs
  "Share link and invite via mail in a tabbed view."
  []
  [common/tab-builder
   "invite-participants"
   ;; participants access via link
   {:link (labels :schnaq.admin.invite/via-link)
    :view [:<>
           [educate-element]
           [copy-link-form links/get-share-link "share-hash"]]}
   ;; participants access via mail
   {:link (labels :schnaq.admin.invite/via-mail)
    :view [invite-participants-form]}
   ;; admin access via mail
   {:link (labels :schnaq.admin.edit.link/header)
    :view [send-admin-center-link]}
   ;; manage discussion / delete posts
   {:link (labels :schnaq.admin.edit/administrate)
    :view [administrate-discussion]}])

;; -----------------------------------------------------------------------------

(defn- admin-center
  "This view is presented to the user after they have created a new meeting."
  []
  (let [{:discussion/keys [share-hash title]} @(rf/subscribe [:schnaq/last-added])]
    ;; display admin center
    [pages/with-discussion-header
     {:page/heading (labels :schnaq.admin/heading)
      :page/subheading (gstring/format (labels :schnaq.admin/subheading) title)}
     [:div.container.px-3.px-md-5.py-3.text-center
      [invite-participants-tabs]
      [:div.pb-5.mt-3]
      ;; stop image and hint to copy the link
      [:div.single-image [:img {:src (img-path :schnaqqifant/stop)}]]
      [:h4.mb-4 (labels :schnaqs/continue-with-schnaq-after-creation)]
      [:button.btn.button-primary.btn-lg.center-block.mb-5
       {:role "button"
        :on-click #(rf/dispatch [:navigation/navigate :routes.schnaq/start {:share-hash share-hash}])}
       (labels :schnaqs/continue-to-schnaq-button)]]]))

(defn admin-center-view []
  [admin-center])

;; #### Events ####

(rf/reg-sub
  :schnaqs/load-admin-access
  (fn [db [_]]
    (get-in db [:schnaqs :admin-access])))

(rf/reg-event-fx
  :schnaqs.save-admin-access/to-localstorage-and-db
  (fn [{:keys [db]} [_ share-hash edit-hash]]
    ;; PARTIALLY DEPRECATED, deleted after 2021-09-22: Remove the old ls/add-key-value-… stuff and only load native structure
    (let [deprecated-map (ls/add-key-value-and-build-map-from-localstorage :schnaqs/admin-access)
          admin-access-map (assoc (:schnaqs/admin-access local-storage) share-hash edit-hash)
          merged-access (merge deprecated-map admin-access-map)]
      {:db (assoc-in db [:schnaqs :admin-access] merged-access)
       :fx [[:localstorage/assoc [:schnaqs/admin-access merged-access]]
            (when (= 1 (count admin-access-map))
              [:dispatch [:celebrate/first-schnaq-created]])]})))

(rf/reg-event-db
  ;; PARTIALLY DEPRECATED FROM 2021-09-22: Remove the second or clause
  :schnaqs.save-admin-access/store-hashes-from-localstorage
  (fn [db _]
    (assoc-in db [:schnaqs :admin-access] (or (:schnaqs/admin-access local-storage)
                                              (ls/parse-hash-map-string (ls/get-item :schnaqs/admin-access))))))