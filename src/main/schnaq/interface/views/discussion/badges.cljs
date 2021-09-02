(ns schnaq.interface.views.discussion.badges
  (:require [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.localstorage :as ls]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.discussion.labels :as statement-labels]
            [schnaq.interface.views.modal :as modal]
            [schnaq.user :as user]))

(defn- anonymous-edit-modal
  "Show this modal to anonymous users trying to edit statements."
  []
  (modal/anonymous-modal :discussion.anonymous-edit.modal/title
                         :discussion.anonymous-edit.modal/explain
                         :discussion.anonymous-edit.modal/cta))

(defn- anonymous-delete-modal
  "Show this modal to anonymous users trying to delete statements."
  []
  (modal/anonymous-modal :discussion.anonymous-delete.modal/title
                         :discussion.anonymous-delete.modal/explain
                         :discussion.anonymous-delete.modal/cta))

(defn- delete-dropdown-button
  "Give admin and author the ability to delete a statement."
  [statement edit-hash]
  (let [creation-secrets @(rf/subscribe [:schnaq.discussion.statements/creation-secrets])
        anonymous-owner? (contains? creation-secrets (:db/id statement))
        confirmation-fn (fn [dispatch-fn] (when (js/confirm (labels :discussion.badges/delete-statement-confirmation))
                                            (dispatch-fn)))
        admin-delete-fn #(confirmation-fn (fn [] (rf/dispatch [:discussion.delete/statement (:db/id statement) edit-hash])))
        user-delete-fn (if anonymous-owner? #(rf/dispatch [:modal {:show? true :child [anonymous-delete-modal]}])
                                            #(confirmation-fn (fn [] (rf/dispatch [:statement/delete (:db/id statement)]))))]
    [:button.dropdown-item
     {:tabIndex 50
      :on-click (fn [e]
                  (js-wrap/stop-propagation e)
                  (if edit-hash (admin-delete-fn)
                                (user-delete-fn)))
      :title (labels :discussion.badges/delete-statement)}
     [:i {:class (str "m-auto fas " (fa :trash))}] " " (labels :discussion.badges/delete-statement)]))

(defn- edit-dropdown-button
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
    [:button.dropdown-item
     {:tabIndex 40
      :on-click (fn [e]
                  (js-wrap/stop-propagation e)
                  (on-click-fn))
      :title (labels :discussion.badges/edit-statement)}
     [:i {:class (str "m-auto fas " (fa :edit))}] " " (labels :discussion.badges/edit-statement)]))

(defn- is-deletable?
  "Checks if a statement can be deleted"
  [statement edit-hash]
  (when-not (:statement/deleted? statement)
    (let [user-id @(rf/subscribe [:user/id])
          creation-secrets @(rf/subscribe [:schnaq.discussion.statements/creation-secrets])
          anonymous-owner? (contains? creation-secrets (:db/id statement))
          registered-owner? (= user-id (:db/id (:statement/author statement)))]
      (or edit-hash anonymous-owner? registered-owner?))))

(defn- is-editable?
  "Checks if a statement can be edited"
  [statement]
  (let [user-id @(rf/subscribe [:user/id])
        creation-secrets @(rf/subscribe [:schnaq.discussion.statements/creation-secrets])
        anonymous-owner? (contains? creation-secrets (:db/id statement))]
    (and (not (:statement/deleted? statement))
         (or anonymous-owner?
             (= user-id (:db/id (:statement/author statement)))))))

(defn- edit-statement-dropdown-menu [{:keys [db/id] :as statement} edit-hash]
  (let [dropdown-id (str "drop-down-conclusion-card-" id)
        deletable? (is-deletable? statement edit-hash)
        editable? (is-editable? statement)]
    (when (or deletable? editable?)
      [:div.dropdown
       [:a.dropdown-toggle.m-0.p-0
        {:id dropdown-id
         :href "#" :role "button" :data-toggle "dropdown"
         :aria-haspopup "true" :aria-expanded "false"}
        [:i {:class (str "fas " (fa :dots))}]]
       [:div.dropdown-menu.dropdown-menu-right {:aria-labelledby dropdown-id}
        (when editable?
          [:dropdown-item
           [edit-dropdown-button statement]])
        (when deletable?
          [:dropdown-item
           [delete-dropdown-button statement edit-hash]])]])))

(defn- author-list
  "A list of author-names participating in a subdiscussion."
  [authors]
  [:<>
   [:h5 (labels :discussion.badges/user-overview)]
   [:hr]
   [:ul.list-unstyled.text-left
    (for [author (sort > (remove nil? authors))]
      [:li {:key author} author])]])

(defn- authors-badge
  "A badge listing the people participating in the discussion."
  [authors]
  [:span.badge.badge-pill.badge-transparent.badge-clickable.mr-2
   [tooltip/html
    [author-list authors]
    [:div [:i {:class (str "m-auto fas " (fa :user/group))}] " " (count authors)]
    {:animation "scale"
     :trigger "click"
     :appendTo js-wrap/document-body}]])

(defn extra-discussion-info-badges
  "Badges that display additional discussion info."
  [statement edit-hash]
  (let [old-statements-nums-map @(rf/subscribe [:visited/statement-nums])
        path-parameters (:path-params @(rf/subscribe [:navigation/current-route]))
        old-statement-num (get old-statements-nums-map (:db/id statement) 0)
        statement-num (get-in statement [:meta/sub-discussion-info :sub-statements] 0)
        new? (not (= old-statement-num statement-num))
        authors (conj (-> statement :meta/sub-discussion-info :authors)
                      (user/statement-author statement))
        pill-class {:class (str "m-auto fas " (fa :comments))}]
    [:div.d-flex.flex-row
     [:a.badge.badge-pill.badge-transparent.badge-clickable.mr-2
      {:href (rfe/href :routes.schnaq.select/statement (assoc path-parameters :statement-id (:db/id statement)))
       :role :button}
      (if new?
        [:i.secondary-color pill-class]
        [:i pill-class])
      " " statement-num
      " " (labels :discussion.badges/posts)]
     [authors-badge authors]
     [statement-labels/edit-labels-button statement]
     [edit-statement-dropdown-menu statement edit-hash]]))

(defn static-info-badges
  "Badges that display schnaq info."
  [schnaq]
  (let [meta-info (:meta-info schnaq)
        statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))]
    [:p.mb-0
     [:span.badge.badge-pill.badge-transparent.mr-2
      [:i {:class (str "m-auto fas " (fa :comments))}]
      " " statement-count]
     [:span.badge.badge-pill.badge-transparent.mr-2
      {:tabIndex 20
       :title (labels :discussion.badges/user-overview)}
      [:i {:class (str "m-auto fas " (fa :user/group))}] " " user-count]]))

(defn read-only-badge
  "Badge that appears only if the passed schnaq is set to read-only"
  [schnaq]
  (let [read-only? (some #{:discussion.state/read-only} (:discussion/states schnaq))]
    (when read-only?
      [:span.badge.badge-pill.badge-secondary-outline (labels :discussion.state/read-only-label)])))


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
          ;; DEPRECATED, deleted after 2021-09-22: Remove deprecated-map and use the map directly after merging
          deprecated-map (->> (ls/get-item :discussion/statement-nums)
                              ls/parse-hash-map-string
                              (map #(vector (js/parseInt (first %)) (js/parseInt (second %))))
                              (into {}))
          current-visited-nums (:discussion/statement-nums local-storage)
          merged-visited-nums (merge deprecated-map current-visited-nums statements-nums-map)]
      {:fx [[:localstorage/assoc [:discussion/statement-nums merged-visited-nums]]
            [:dispatch [:visited.save-statement-nums/store-hashes-from-localstorage]]]})))

(rf/reg-event-db
  :visited/set-visited-statements
  (fn [db [_ statement]]
    (assoc-in db [:visited :statement-nums (:db/id statement)]
              (get-in statement [:meta/sub-discussion-info :sub-statements] 0))))
