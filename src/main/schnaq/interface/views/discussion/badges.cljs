(ns schnaq.interface.views.discussion.badges
  (:require [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.modal :as modal]))

(defn- anonymous-edit-modal
  "Show this modal to anonymous users trying to edit statements."
  []
  [modal/anonymous-modal
   :discussion.anonymous-edit.modal/title
   :discussion.anonymous-edit.modal/explain
   :discussion.anonymous-edit.modal/cta])

(defn- anonymous-delete-modal
  "Show this modal to anonymous users trying to delete statements."
  []
  [modal/anonymous-modal
   :discussion.anonymous-delete.modal/title
   :discussion.anonymous-delete.modal/explain
   :discussion.anonymous-delete.modal/cta])

(defn- deletable?
  "Checks if a statement can be deleted by the user."
  [statement edit-hash user-id creation-secrets]
  (when-not (:statement/deleted? statement)
    (let [anonymous-owner? (contains? creation-secrets (:db/id statement))
          registered-owner? (= user-id (:db/id (:statement/author statement)))
          administrator? @(rf/subscribe [:user/administrator?])]
      (or edit-hash anonymous-owner? registered-owner? administrator?))))

(defn- delete-dropdown-button
  "Give admin and author the ability to delete a statement."
  [statement edit-hash]
  (let [creation-secrets @(rf/subscribe [:schnaq.discussion.statements/creation-secrets])
        anonymous-owner? (contains? creation-secrets (:db/id statement))
        confirmation-fn (fn [dispatch-fn] (when (js/confirm (labels :discussion.badges/delete-statement-confirmation))
                                            (dispatch-fn)))
        admin-delete-fn #(confirmation-fn (fn [] (rf/dispatch [:discussion.delete/statement (:db/id statement) edit-hash])))
        user-delete-fn (if anonymous-owner?
                         #(rf/dispatch [:modal {:show? true :child [anonymous-delete-modal]}])
                         #(confirmation-fn (fn [] (rf/dispatch [:statement/delete (:db/id statement)]))))]
    [:button.dropdown-item
     {:tabIndex 60
      :on-click (fn [e]
                  (js-wrap/stop-propagation e)
                  (if edit-hash (admin-delete-fn) (user-delete-fn)))
      :title (labels :discussion.badges/delete-statement)}
     [icon :trash "my-auto mr-1"] " " (labels :discussion.badges/delete-statement)]))

(defn- edit-dropdown-button
  "Edit button to trigger custom functionality."
  [on-click-fn]
  [:button.dropdown-item
   {:tabIndex 40
    :on-click (fn [e]
                (js-wrap/stop-propagation e)
                (on-click-fn))
    :title (labels :discussion.badges/edit-statement)}
   [icon :edit "my-auto"] " " (labels :discussion.badges/edit-statement)])

(defn- edit-dropdown-button-statement
  "Give the registered user the ability to edit their statement."
  [statement]
  (let [creation-secrets @(rf/subscribe [:schnaq.discussion.statements/creation-secrets])
        anonymous-owner? (contains? creation-secrets (:db/id statement))
        on-click-fn (if anonymous-owner?
                      #(rf/dispatch [:modal {:show? true
                                             :child [anonymous-edit-modal]}])
                      (fn []
                        (rf/dispatch [:statement.edit/activate-edit (:db/id statement)])
                        (rf/dispatch [:statement.edit/change-statement-type (:db/id statement)
                                      (:statement/type statement)])))]
    [edit-dropdown-button on-click-fn]))

(defn- edit-dropdown-button-discussion
  "Give the registered user the ability to edit their discussion title."
  [discussion-id share-hash]
  (let [creation-secrets @(rf/subscribe [:schnaq.discussion/creation-secrets])
        anonymous-owner? (contains? creation-secrets share-hash)
        on-click-fn (if anonymous-owner?
                      #(rf/dispatch [:modal {:show? true
                                             :child [anonymous-edit-modal]}])
                      (fn []
                        (rf/dispatch [:statement.edit/activate-edit discussion-id])))]
    [edit-dropdown-button on-click-fn]))

(defn- editable?
  "Checks if a statement can be edited"
  [statement user-id creation-secrets]
  (let [anonymous-owner? (contains? creation-secrets (:db/id statement))]
    (and (not (:statement/deleted? statement))
         (or anonymous-owner?
             (= user-id (:db/id (:statement/author statement)))))))

(defn- edit-discussion-dropdown-menu [{:keys [db/id
                                              discussion/share-hash
                                              discussion/author]}]
  (let [dropdown-id (str "drop-down-conclusion-card-" id)
        creation-secrets @(rf/subscribe [:schnaq.discussion/creation-secrets])
        user-id @(rf/subscribe [:user/id])
        anonymous-owner? (contains? creation-secrets share-hash)
        editable? (or anonymous-owner?
                      (= user-id (:db/id author)))]
    (when editable?
      [:div.dropdown.ml-2
       [:div.dropdown-toggle.m-0.p-0
        {:id dropdown-id
         :href "#" :role "button" :data-toggle "dropdown"
         :aria-haspopup "true" :aria-expanded "false"}
        [icon :dots]]
       [:div.dropdown-menu.dropdown-menu-right {:aria-labelledby dropdown-id}
        (when editable?
          [:dropdown-item
           [edit-dropdown-button-discussion id share-hash]])]])))

(defn- flag-dropdown-button-statement [statement]
  (let [confirmation-fn (fn [dispatch-fn] (when (js/confirm (labels :statement/flag-statement-confirmation))
                                            (dispatch-fn)))
        flag-statement-fn #(confirmation-fn (fn [] (rf/dispatch [:statement/flag (:db/id statement)])))]
    [:button.dropdown-item
     {:tabIndex 50
      :on-click (fn [e] (js-wrap/stop-propagation e)
                  (flag-statement-fn))
      :title (labels :discussion.badges/edit-statement)}
     [icon :flag "my-auto mr-1"] " " (labels :statement/flag-statement)]))

(rf/reg-event-fx
 :statement/flag
 (fn [{:keys [db]} [_ statement-id]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:fx [(http/xhrio-request db :post "/discussion/statement/flag"
                               [:discussion.admin/flag-statement-success statement-id]
                               {:statement-id statement-id
                                :share-hash share-hash}
                               [:ajax.error/as-notification])]})))

(rf/reg-event-fx
 ;; Success event when flagging a post
 :discussion.admin/flag-statement-success
 (fn [_ [_ _statement-id]]
   {:fx [[:dispatch [:notification/add
                     #:notification{:title (labels :statement.notifications/statement-flagged-title)
                                    :body (labels :statement.notifications/statement-flagged-body)
                                    :context :success}]]]}))

(defn- statement-dropdown-menu [dropdown-id dropdown-items]
  [:div.dropdown.ml-2
   [:div.dropdown-toggle.m-0.p-0
    {:id dropdown-id
     :href "#" :role "button" :data-toggle "dropdown"
     :aria-haspopup "true" :aria-expanded "false"}
    [icon :dots]]
   [:div.dropdown-menu.dropdown-menu-right {:aria-labelledby dropdown-id}
    dropdown-items]])

(defn edit-statement-dropdown-menu
  "Dropdown menu for statements containing edit report and deletion."
  [{:keys [db/id] :as statement}]
  (let [dropdown-id (str "drop-down-conclusion-card-" id)
        current-edit-hash @(rf/subscribe [:schnaq.current/admin-access])
        user-id @(rf/subscribe [:user/id])
        creation-secrets @(rf/subscribe [:schnaq.discussion.statements/creation-secrets])
        deletable? (deletable? statement current-edit-hash user-id creation-secrets)
        editable? (editable? statement user-id creation-secrets)]
    [statement-dropdown-menu dropdown-id
     [:<>
      (when editable?
        [:dropdown-item
         [edit-dropdown-button-statement statement]])
      [:dropdown-item
       [flag-dropdown-button-statement statement]]
      (when deletable?
        [:dropdown-item
         [delete-dropdown-button statement current-edit-hash]])]]))

(defn extra-discussion-info-badges
  "Badges that display additional discussion info."
  ([statement]
   (extra-discussion-info-badges statement false))
  ([statement with-edit-dropdown?]
   (let [old-statements-nums-map @(rf/subscribe [:visited/statement-nums])
         share-hash @(rf/subscribe [:schnaq/share-hash])
         old-statement-num (get old-statements-nums-map (:db/id statement) 0)
         statement-num (:meta/sub-statement-count statement 0)
         new? (not (= old-statement-num statement-num))]
     [:div.d-flex.flex-row.align-items-center
      [:a.badge.badge-pill.badge-transparent.badge-clickable.ml-3
       {:href (rfe/href :routes.schnaq.select/statement {:share-hash share-hash
                                                         :statement-id (:db/id statement)})
        :role :button}
       (if new?
         [icon :comments "m-auto text-secondary"]
         [icon :comments "m-auto"])
       " " statement-num]
      (when with-edit-dropdown?
        [:div.ml-2
         [edit-statement-dropdown-menu statement]])])))

(defn comments-info-badge
  "Badge that display the comment count."
  [schnaq]
  (let [meta-info (:meta-info schnaq)
        statement-count (:all-statements meta-info)]
    [:p.mb-0
     [:span.small.mr-2
      [icon :comment/alt "m-auto"]
      " " statement-count
      " " (labels :discussion.badges/posts)]]))

(defn static-info-badges
  "Badges that display schnaq info."
  [schnaq]
  (let [meta-info (:meta-info schnaq)
        statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))]
    [:p.mb-0
     [:span.badge.badge-pill.badge-transparent.mr-2
      [icon :comments "m-auto"]
      " " statement-count]
     [:span.badge.badge-pill.badge-transparent.mr-2
      {:tabIndex 20
       :title (labels :discussion.badges/user-overview)}
      [icon :user/group "m-auto"] " " user-count]]))

(defn static-info-badges-discussion
  "Badges that display schnaq info."
  [schnaq]
  (let [meta-info (:meta-info schnaq)
        statement-count (:all-statements meta-info)]
    [:div.d-flex.flex-row.mb-0
     [:span.badge.badge-pill.badge-transparent.mr-2
      [icon :comments "m-auto"]
      " " statement-count]
     [edit-discussion-dropdown-menu schnaq]]))

(defn read-only-badge
  "Badge that appears only if the passed schnaq is set to read-only"
  [schnaq]
  (let [read-only? (some #{:discussion.state/read-only} (:discussion/states schnaq))]
    (when read-only?
      [:div.small.my-auto.text-secondary (labels :discussion.state/read-only-label)])))

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :statement/delete
 (fn [{:keys [db]} [_ statement-id]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:fx [(http/xhrio-request db :delete "/discussion/statement/delete"
                               [:discussion.admin/delete-statement-success statement-id]
                               {:statement-id statement-id
                                :share-hash share-hash}
                               [:ajax.error/as-notification])]})))

(rf/reg-sub
 :visited/statement-nums
 (fn [db [_]]
   (get-in db [:visited :statement-nums])))

(rf/reg-event-db
 :visited.save-statement-nums/store-hashes-from-localstorage
 (fn [db _]
   (assoc-in db [:visited :statement-nums] (:discussion/statement-nums local-storage))))

(rf/reg-event-fx
 :visited.statement-nums/to-localstorage
 (fn [{:keys [db]} [_]]
   (let [statements-nums-map (get-in db [:visited :statement-nums])
         current-visited-nums (:discussion/statement-nums local-storage)
         merged-visited-nums (merge current-visited-nums statements-nums-map)]
     {:fx [[:localstorage/assoc [:discussion/statement-nums merged-visited-nums]]
           [:dispatch [:visited.save-statement-nums/store-hashes-from-localstorage]]]})))

(rf/reg-event-db
 :visited/set-visited-statements
 (fn [db [_ statement]]
   (assoc-in db [:visited :statement-nums (:db/id statement)]
             (:meta/sub-statement-count statement 0))))
