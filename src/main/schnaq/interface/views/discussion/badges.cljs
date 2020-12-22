(ns schnaq.interface.views.discussion.badges
  (:require [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))

(>defn- build-author-list
  "Build a nicely formatted string of a html list containing the authors from a sequence."
  [authors]
  [sequential? :ret string?]
  (str
    "<ul class=\"authors-list\">"
    (apply str (map #(str "<li>" (:author/nickname %) "</li>") authors))
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
  (let [popover-id (str "debater-popover-" (:db/id statement))]
    [:p.mb-0
     [:span.badge.badge-pill.badge-transparent.badge-clickable.mr-2
      [:i {:class (str "m-auto fas " (fa :comment))}] " "
      (-> statement :meta/sub-discussion-info :sub-statements)]
     [:span.badge.badge-pill.badge-transparent.badge-clickable.mr-2
      {:id popover-id
       :data-toggle "popover"
       :data-trigger "focus"
       :tabIndex 20
       :on-click (fn [e] (js-wrap/stop-propagation e)
                   (js-wrap/popover (str "#" popover-id) "show"))
       :title (labels :discussion.badges/user-overview)
       :data-html true
       :data-content (build-author-list (get-in statement [:meta/sub-discussion-info :authors]))}
      [:i {:class (str "m-auto fas " (fa :user/group))}] " "
      (-> statement :meta/sub-discussion-info :authors count)]
     (when edit-hash
       [delete-clicker statement edit-hash])]))