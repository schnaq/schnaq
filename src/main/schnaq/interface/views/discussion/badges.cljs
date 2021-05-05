(ns schnaq.interface.views.discussion.badges
  (:require [ghostwheel.core :refer [>defn-]]
            [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.localstorage :as ls]
            [schnaq.interface.utils.time :as time]
            [schnaq.interface.views.modals.modal :as modal]
            [schnaq.user :as user]))

(>defn- build-author-list
  "Build a nicely formatted string of a html list containing the authors from a sequence."
  [users]
  [sequential? :ret string?]
  (str
    "<ul class=\"authors-list\">"
    (apply str (map #(str "<li>" % "</li>") users))
    "</ul>"))

(defn- delete-clicker
  "Give admin the ability to delete a statement."
  [statement edit-hash]
  (when-not (:statement/deleted? statement)
    [:span.badge.badge-pill.badge-transparent.badge-clickable
     {:tabIndex 30
      :on-click (fn [e] (js-wrap/stop-propagation e)
                  (when (js/confirm (labels :discussion.badges/delete-statement-confirmation))
                    (rf/dispatch [:discussion.delete/statement (:db/id statement) edit-hash])))
      :title (labels :discussion.badges/delete-statement)}
     [:i {:class (str "m-auto fas " (fa :trash))}]]))

(defn- anonymous-edit-modal
  "Show this modal to anonymous users trying to edit statements."
  []
  [modal/modal-template
   (labels :discussion.anonymous-edit.modal/title)
   [:<>
    [:p [:i {:class (str "m-auto fas fa-lg " (fa :shield))}] " " (labels :discussion.anonymous-edit.modal/explain)]
    [:p (labels :discussion.anonymous-edit.modal/persuade)]
    [:button.btn.btn-primary.mx-auto.d-block
     {:on-click #(rf/dispatch [:keycloak/login])}
     (labels :discussion.anonymous-edit.modal/cta)]]])

(defn- edit-button
  "Give the registered user the ability to edit their statement."
  [statement]
  (let [user-id @(rf/subscribe [:user/id])
        creation-secrets @(rf/subscribe [:schnaq.discussion.statements/creation-secrets])
        anonymous-owner (contains? creation-secrets (:db/id statement))
        on-click-fn (if anonymous-owner
                      #(rf/dispatch [:modal {:show? true
                                             :child [anonymous-edit-modal]}])
                      (fn []
                        (rf/dispatch [:statement.edit/activate-edit (:db/id statement)])
                        (rf/dispatch [:statement.edit/change-argument-type (:db/id statement)
                                      (:meta/argument-type statement)])))]
    (when (or anonymous-owner
              ; User is registered author
              (and (= user-id (:db/id (:statement/author statement)))
                   (not (:statement/deleted? statement))))
      [:span.badge.badge-pill.badge-transparent.badge-clickable
       {:tabIndex 40
        :on-click (fn [e]
                    (js-wrap/stop-propagation e)
                    (on-click-fn))
        :title (labels :discussion.badges/edit-statement)}
       [:i {:class (str "m-auto fas " (fa :edit))}] " " (labels :discussion.badges/edit-statement)])))

(defn extra-discussion-info-badges
  "Badges that display additional discussion info."
  [statement edit-hash]
  (let [popover-id (str "debater-popover-" (:db/id statement))
        old-statements-nums-map @(rf/subscribe [:visited/statement-nums])
        old-statement-num (get old-statements-nums-map (:db/id statement) 0)
        statement-num (get-in statement [:meta/sub-discussion-info :sub-statements] 0)
        new? (not (= old-statement-num statement-num))
        authors (conj (-> statement :meta/sub-discussion-info :authors)
                      (user/statement-author statement))
        pill-class {:class (str "m-auto fas " (fa :comment))}]
    [:p.mb-0
     [:span.badge.badge-pill.badge-transparent.badge-clickable.mr-2
      (if new?
        [:i.secondary-color pill-class]
        [:i pill-class])
      " " statement-num]
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
     [edit-button statement]
     (when edit-hash
       [delete-clicker statement edit-hash])]))

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
      [:p [:span.badge.badge-pill.badge-secondary-outline (labels :discussion.state/read-only-label)]])))

;; #### Subs ####

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
