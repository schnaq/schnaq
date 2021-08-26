(ns schnaq.interface.views.discussion.badges
  (:require [ghostwheel.core :refer [>defn-]]
            [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.localstorage :as ls]
            [schnaq.interface.utils.time :as time]
            [schnaq.interface.views.discussion.common :as dcommon]
            [schnaq.interface.views.modal :as modal]
            [schnaq.user :as user]
            [schnaq.config.shared :as shared-config]))

(>defn- build-author-list
  "Build a nicely formatted string of a html list containing the authors from a sequence."
  [users]
  [sequential? :ret string?]
  (str
    "<ul class=\"authors-list\">"
    (apply str (map #(str "<li>" % "</li>") users))
    "</ul>"))

(defn- anonymous-modal
  "Basic modal which is presented to anonymous users trying to alter statements."
  [header-label shield-label info-label]
  [modal/modal-template
   (labels header-label)
   [:<>
    [:p [:i {:class (str "m-auto fas fa-lg " (fa :shield))}] " " (labels shield-label)]
    [:p (labels :discussion.anonymous-edit.modal/persuade)]
    [:button.btn.btn-primary.mx-auto.d-block
     {:on-click #(rf/dispatch [:keycloak/login])}
     (labels info-label)]]])

(defn- anonymous-labels-modal
  "Explain to anonymous users that they need to log in to set and remove labels."
  []
  ;; TODO set the right labels
  (anonymous-modal :discussion.anonymous-labels.modal/title
                   :discussion.anonymous-labels.modal/explain
                   :discussion.anonymous-labels.modal/cta))

(defn- anonymous-edit-modal
  "Show this modal to anonymous users trying to edit statements."
  []
  (anonymous-modal :discussion.anonymous-edit.modal/title
                   :discussion.anonymous-edit.modal/explain
                   :discussion.anonymous-edit.modal/cta))


(defn- anonymous-delete-modal
  "Show this modal to anonymous users trying to delete statements."
  []
  (anonymous-modal :discussion.anonymous-delete.modal/title
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

(defn- edit-labels-button
  "Give the registered user the ability to add or remove labels to a statement."
  ;; TODO labelize
  [statement]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])
        on-click-fn (if authenticated?
                      #(rf/dispatch [:modal {:show? true
                                             :child [anonymous-labels-modal]}])
                      (fn []
                        ;; TODO add label function here
                        ))]
    (if true
      (let [dropdown-id (str "label-" (:db/id statement))]
        #_[:div.btn-group
           [:button.btn.btn-danger.dropdown-toggle {:type "button" :data-toggle "dropdown" :aria-haspopup "true" :aria-expanded "false"} "Action"]
           [:div.dropdown-menu.dropdown-menu-right
            [:a.dropdown-item {:href "#"} "Action"]
            [:a.dropdown-item {:href "#"} "Another action"]
            [:a.dropdown-item {:href "#"} "Something else here"]
            [:div.dropdown-divider]
            [:a.dropdown-item {:href "#"} "Separated link"]
            [:a.dropdown-item {:href "#"} "Separated link"]
            [:a.dropdown-item {:href "#"} "Separated link"]
            [:a.dropdown-item {:href "#"} "Separated link"]]]
        [:div.dropdown.pr-2
         [:a.dropdown-toggle.m-0.p-0
          {:id dropdown-id
           :href "#" :role "button" :data-toggle "dropdown"
           :aria-haspopup "true" :aria-expanded "false"}
          [:i {:class (fa :tag)}]]
         [:div.dropdown-menu.dropdown-menu-right {:aria-labelledby dropdown-id}
          (for [label (conj shared-config/allowed-labels "A" "b" "c" "d")]
            [:button.dropdown-item
             {:key (str "label-" (:db/id statement) "-" label)}
             label])
          #_[:dropdown-item
             [edit-dropdown-button statement]]]]
        #_[:div.dropdown
           [:a.dropdown-toggle.m-0.p-0
            {:id label-dropdown-id
             :href "#" :role "button" :data-toggle "dropdown"
             :aria-haspopup "true" :aria-expanded "true"}
            [:i {:class (str "fas " (fa :tag))}]]
           [:div.dropdown-menu {:aria-labelledby label-dropdown-id}
            (for [label shared-config/allowed-labels]
              [:dropdown-item
               {:key (str "label-" (:db/id statement) "-" label)}
               [:button.dropdown-item
                label]])]])
      #_[:button.dropdown-item
         {:tabIndex 30
          :on-click (fn [e]
                      (js-wrap/stop-propagation e)
                      (on-click-fn))
          :title "Labels"}
         [:i {:class (str "m-auto " (fa :tag))}] " Labels"])))

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

(defn extra-discussion-info-badges
  "Badges that display additional discussion info."
  [statement edit-hash]
  (let [popover-id (str "debater-popover-" (:db/id statement))
        old-statements-nums-map @(rf/subscribe [:visited/statement-nums])
        path-parameters (:path-params @(rf/subscribe [:navigation/current-route]))
        old-statement-num (get old-statements-nums-map (:db/id statement) 0)
        statement-num (get-in statement [:meta/sub-discussion-info :sub-statements] 0)
        new? (not (= old-statement-num statement-num))
        authors (conj (-> statement :meta/sub-discussion-info :authors)
                      (user/statement-author statement))
        pill-class {:class (str "m-auto fas " (fa :comment))}]
    [:div.d-flex.flex-row
     [:span.badge.badge-pill.badge-transparent.badge-clickable.mr-2
      {:on-click (dcommon/navigate-to-statement-on-click statement path-parameters)}
      (if new?
        [:i.secondary-color pill-class]
        [:i pill-class])
      " " statement-num
      " " (labels :discussion.badges/posts)]
     [:span.badge.badge-pill.badge-transparent.badge-clickable.mr-2
      {:id popover-id
       :data-toggle "popover"
       :data-trigger "focus"
       :tabIndex 20
       :on-click (fn [e] (js-wrap/stop-propagation e)
                   (js-wrap/popover (str "#" popover-id) "show"))
       :title (labels :discussion.badges/user-overview)
       :data-html true
       :data-content (build-author-list authors)}
      [:i {:class (str "m-auto fas " (fa :user/group))}] " "
      (count authors)]
     [edit-labels-button statement]
     [edit-statement-dropdown-menu statement edit-hash]]))

(defn static-info-badges
  "Badges that display schnaq info."
  [schnaq]
  (let [meta-info (:meta-info schnaq)
        statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))
        locale @(rf/subscribe [:current-locale])]
    [:p.mb-0
     [:span.badge.badge-pill.badge-transparent.mr-2
      [:i {:class (str "m-auto fas " (fa :comment))}]
      " " statement-count]
     [:span.badge.badge-pill.badge-transparent.mr-2
      {:tabIndex 20
       :title (labels :discussion.badges/user-overview)}
      [:i {:class (str "m-auto fas " (fa :user/group))}] " " user-count]
     [:small.text-muted [time/timestamp-with-tooltip (:discussion/created-at schnaq) locale]]]))

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
    (let [share-hash (get-in db [:current-route :path-params :share-hash])]
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
