(ns schnaq.interface.views.discussion.badges
  (:require [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.localstorage :as ls]
            [schnaq.interface.utils.time :as time]))

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

(defn extra-discussion-info-badges
  "Badges that display additional discussion info."
  [statement edit-hash]
  (let [popover-id (str "debater-popover-" (:db/id statement))
        locale @(rf/subscribe [:current-locale])
        old-statements-nums-map @(rf/subscribe [:visited/load-statement-nums])
        old-statement-num (get old-statements-nums-map (str (:db/id statement)) 0)
        statement-num (inc (get-in statement [:meta/sub-discussion-info :sub-statements] 0))
        new? (not (= (inc old-statement-num) statement-num))
        authors (conj (-> statement :meta/sub-discussion-info :authors)
                      (-> statement :statement/author :user/nickname))
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
     (when edit-hash
       [delete-clicker statement edit-hash])
     [:small.text-muted [time/timestamp-with-tooltip (:db/txInstant statement) locale]]]))

(defn static-info-badges
  "Badges that display schnaq info."
  [schnaq]
  (let [meta-info (:meta-info schnaq)
        statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))]
    [:p.mb-0
     [:span.badge.badge-pill.badge-transparent.mr-2
      [:i {:class (str "m-auto fas " (fa :comment))}]
      " " statement-count]
     [:span.badge.badge-pill.badge-transparent.mr-2
      {:tabIndex 20
       :title (labels :discussion.badges/user-overview)}
      [:i {:class (str "m-auto fas " (fa :user/group))}] " " user-count]
     [:small.text-muted [time/timestamp-with-tooltip (:db/txInstant schnaq) :de]]]))


;; #### Subs ####

(rf/reg-sub
  :visited/load-statement-nums
  (fn [db [_]]
    (let [string-value-hash-map (get-in db [:visited :statement-nums])
          int-values (map (fn [[key value]] {key (js/parseInt value)}) string-value-hash-map)]
      (into {} int-values))))

(rf/reg-event-db
  :visited.save-statement-nums/store-hashes-from-localstorage
  (fn [db _]
    (assoc-in db [:visited :statement-nums]
              (ls/parse-hash-map-string (ls/get-item :discussion/statement-nums)))))

(rf/reg-event-fx
  :visited.statement-nums/to-localstorage
  (fn [_ [_]]
    (let [statements-nums-map @(rf/subscribe [:visited/statement-nums])]
      {:fx [[:localstorage/write
             [:discussion/statement-nums
              (ls/add-hash-map-and-build-map-from-localstorage statements-nums-map :visited/statement-nums)]]
            [:dispatch [:visited.save-statement-nums/store-hashes-from-localstorage]]]})))

(rf/reg-sub
  :visited/statement-nums
  (fn [db _]
    (get-in db [:visited :statement-nums])))

(rf/reg-event-db
  :visited/set-visited-statements
  (fn [db [_ statement]]
    (assoc-in db [:visited :statement-nums (str (:db/id statement))]
              (str (get-in statement [:meta/sub-discussion-info :sub-statements] 0)))))
