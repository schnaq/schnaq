(ns schnaq.interface.views.discussion.badges
  (:require [goog.string :refer [format]]
            [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [schnaq.interface.components.common :as common]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.clipboard :as clipboard]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.modal :as modal]
            [schnaq.interface.views.notifications :refer [notify!]]
            [schnaq.links :as schnaq-links]
            [schnaq.user :refer [feature-limit usage-warning-level
                                 warning-level-class]]))

(defn- dropdown-dots
  "Three dot menu which triggers a dropdown."
  [dropdown-id]
  [:button.btn.btn-link.text-dark.m-0.p-0
   {:id dropdown-id
    :role "button" :data-bs-toggle "dropdown"
    :aria-haspopup "true" :aria-expanded "false"}
   [icon :dots]])

(defn- dropdown-menu
  "Build a dropdown menu with dots."
  [dropdown-id dropdown-items]
  [:div.dropdown.ms-2
   [dropdown-dots dropdown-id]
   [:div.dropdown-menu.dropdown-menu-end {:aria-labelledby dropdown-id}
    dropdown-items]])

;; -----------------------------------------------------------------------------

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
          registered-owner? (= user-id (:db/id (:statement/author statement)))]
      (or edit-hash anonymous-owner? registered-owner?))))

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
                  (.stopPropagation e)
                  (if edit-hash (admin-delete-fn) (user-delete-fn)))
      :title (labels :discussion.badges/delete-statement)}
     [icon :trash "my-auto me-2"] (labels :discussion.badges/delete-statement)]))

(defn- share-link-to-statement
  "Copies a link to the statement to the clipboard"
  [statement]
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])
        link (schnaq-links/get-link-to-statement share-hash (:db/id statement))]
    [:button.dropdown-item
     {:tabIndex 40
      :on-click (fn []
                  (clipboard/copy-to-clipboard! link)
                  (notify! (labels :schnaq/link-copied-heading)
                           (labels :schnaq/link-copied-success)
                           :info
                           false))
      :title (labels :discussion.badges/share-statement)}
     [icon :share "my-auto me-2"] (labels :discussion.badges/share-statement)]))

(defn- edit-dropdown-button
  "Edit button to trigger custom functionality."
  [on-click-fn]
  [:button.dropdown-item
   {:tabIndex 40
    :on-click (fn [e]
                (.stopPropagation e)
                (on-click-fn))
    :title (labels :discussion.badges/edit-statement)}
   [icon :edit "my-auto me-1"] (labels :discussion.badges/edit-statement)])

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

(defn- edit-discussion-dropdown-menu []
  (let [{:keys [db/id discussion/share-hash discussion/author]} @(rf/subscribe [:schnaq/selected])
        dropdown-id (str "drop-down-conclusion-card-" id)
        creation-secrets @(rf/subscribe [:schnaq.discussion/creation-secrets])
        user-id @(rf/subscribe [:user/id])
        anonymous-owner? (contains? creation-secrets share-hash)
        editable? (or anonymous-owner?
                      (= user-id (:db/id author)))]
    (when editable?
      [dropdown-menu dropdown-id [edit-dropdown-button-discussion id share-hash]])))

(defn- flag-dropdown-button-statement [statement]
  (let [confirmation-fn (fn [dispatch-fn] (when (js/confirm (labels :statement/flag-statement-confirmation))
                                            (dispatch-fn)))
        flag-statement-fn #(confirmation-fn (fn [] (rf/dispatch [:statement/flag (:db/id statement)])))]
    [:button.dropdown-item
     {:tabIndex 50
      :on-click (fn [e] (.stopPropagation e)
                  (flag-statement-fn))
      :title (labels :discussion.badges/flag-statement)}
     [icon :flag "my-auto me-2"] (labels :statement/flag-statement)]))

(defn- lock-unlock-statement-dropdown-button [statement]
  (let [to-lock? (not (:statement/locked? statement))
        label (labels (if to-lock? :discussion.badges/lock-statement :discussion.badges/unlock-statement))]
    [:button.dropdown-item
     {:tabIndex 55
      :on-click (fn [e] (.stopPropagation e)
                  (rf/dispatch [:statement.lock/toggle (:db/id statement) to-lock?]))
      :title label}
     [icon (if to-lock? :lock :lock/open) "my-auto me-2"] label]))

(rf/reg-event-fx
 :statement.lock/toggle
 (fn [{:keys [db]} [_ statement-id lock?]]
   {:db (assoc-in db [:schnaq :statements statement-id :statement/locked?] lock?)
    :fx [(http/xhrio-request db :post "/discussion/statement/lock/toggle"
                             [:no-op]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])
                              :edit-hash (get-in db [:schnaq :selected :discussion/edit-hash])
                              :statement-id statement-id
                              :lock? lock?})]}))

(defn- toggle-pin-statement-dropdown-button [statement]
  (let [to-pin? (not (:statement/pinned? statement))
        label (labels (if to-pin? :discussion.badges/pin-statement :discussion.badges/unpin-statement))]
    [:button.dropdown-item
     {:tabIndex 56
      :on-click (fn [e] (.stopPropagation e)
                  (rf/dispatch [:statement.pin/toggle (:db/id statement) to-pin?]))
      :title label}
     [icon :pin "my-auto me-2"] label]))

(rf/reg-event-fx
 :statement.pin/toggle
 (fn [{:keys [db]} [_ statement-id pin?]]
   {:db (assoc-in db [:schnaq :statements statement-id :statement/pinned?] pin?)
    :fx [(http/xhrio-request db :post "/discussion/statement/pin/toggle"
                             [:no-op]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])
                              :edit-hash (get-in db [:schnaq :selected :discussion/edit-hash])
                              :statement-id statement-id
                              :pin? pin?})]}))

(rf/reg-event-fx
 :statement/flag
 (fn [{:keys [db]} [_ statement-id]]
   (when-let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
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

(defn edit-statement-dropdown-menu
  "Dropdown menu for statements containing edit report and deletion."
  [{:keys [db/id] :as statement}]
  (let [dropdown-id (str "drop-down-conclusion-card-" id)
        current-edit-hash @(rf/subscribe [:schnaq.current/admin-access])
        admin? @(rf/subscribe [:user/administrator?])
        user-id @(rf/subscribe [:user/id])
        creation-secrets @(rf/subscribe [:schnaq.discussion.statements/creation-secrets])
        read-only? @(rf/subscribe [:schnaq.selected/read-only?])
        deletable? (deletable? statement current-edit-hash user-id creation-secrets)
        editable? (editable? statement user-id creation-secrets)]
    [dropdown-menu dropdown-id
     [:<>
      [share-link-to-statement statement]
      [flag-dropdown-button-statement statement]
      (when (and current-edit-hash @(rf/subscribe [:user/authenticated?]))
        [lock-unlock-statement-dropdown-button statement])
      (when (and current-edit-hash @(rf/subscribe [:user/pro?]))
        [toggle-pin-statement-dropdown-button statement])
      (when-not read-only?
        [:<>
         (when editable?
           [edit-dropdown-button-statement statement])
         (when (or admin? deletable?)
           [delete-dropdown-button statement current-edit-hash])])]]))

(defn show-number-of-replies [statement]
  (let [old-statements-nums-map @(rf/subscribe [:visited/statement-nums])
        share-hash @(rf/subscribe [:schnaq/share-hash])
        old-statement-num (get old-statements-nums-map (:db/id statement) 0)
        statement-num (:meta/sub-statement-count statement 0)
        new? (not (= old-statement-num statement-num))]
    [:a.badge.rounded-pill.badge-transparent.badge-clickable
     {:href (navigation/href :routes.schnaq.select/statement {:share-hash share-hash
                                                              :statement-id (:db/id statement)})
      :role :button}
     [:div.d-flex.flex-wrap.align-items-center
      (if new?
        [icon :comment/alt "m-auto text-secondary me-1"]
        [icon :comment/alt "m-auto me-1"])
      statement-num " "
      (if (= 1 statement-num)
        (labels :statement.badges/more-post)
        (labels :statement.badges/more-posts))]]))

(defn comments-info-badge
  "Badge that display the comment count."
  []
  (let [schnaq @(rf/subscribe [:schnaq/selected])
        meta-info (:meta-info schnaq)
        statement-count (:all-statements meta-info)]
    [:span.small.me-2
     [icon :comment/alt "m-auto"]
     " " statement-count
     " " (labels :discussion.badges/posts)]))

(defn static-info-badges
  "Badges that display schnaq info."
  []
  (let [schnaq @(rf/subscribe [:schnaq/selected])
        meta-info (:meta-info schnaq)
        statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))]
    [:p.mb-0
     [:span.badge.rounded-pill.badge-transparent.me-2
      [icon :comment/alt "m-auto"]
      " " statement-count]
     [:span.badge.rounded-pill.badge-transparent.me-2
      {:tabIndex 20
       :title (labels :discussion.badges/user-overview)}
      [icon :user/group "m-auto"] " " user-count]]))

(defn- number-of-remaining-posts
  "Calculate and highlight the number of remaining posts in this schnaq."
  []
  (let [author @(rf/subscribe [:schnaq/author])
        schnaq @(rf/subscribe [:schnaq/selected])
        statement-count (get-in schnaq [:meta-info :all-statements])
        warning-class (warning-level-class (usage-warning-level author :posts-per-schnaq statement-count))
        limit (feature-limit author :posts-per-schnaq)]
    [:span.badge.rounded-pill.badge-transparent.me-2 {:class warning-class}
     [icon :comment/alt "m-auto me-1"]
     (if limit
       [tooltip/text (labels :feature.limit.posts/alert-tooltip)
        [:span (format "%d %s %d %s" statement-count (labels :discussion.badges/posts-of) limit (labels :discussion.badges/posts-alt))]] ;; WIP
       [:span (format "%d %s" statement-count (labels :discussion.badges/posts))])]))

(defn static-info-badges-discussion
  "Badges that display schnaq info."
  []
  [:div.d-flex.flex-row.mb-0
   [number-of-remaining-posts]
   [edit-discussion-dropdown-menu]])

(defn read-only-badge
  "Badge that appears only if the passed schnaq is set to read-only"
  []
  (let [schnaq @(rf/subscribe [:schnaq/selected])]
    (when (some #{:discussion.state/read-only} (:discussion/states schnaq))
      [:small
       [common/outlined-pill (labels :discussion.state/read-only-label) :secondary]])))

(defn archived-badge
  "Badge that appears only if the passed schnaq is set to read-only"
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    (when @(rf/subscribe [:schnaq.visited/archived? share-hash])
      [:small
       [common/outlined-pill (labels :schnaq.options/archived) :success]])))

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :statement/delete
 (fn [{:keys [db]} [_ statement-id]]
   (when-let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
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
