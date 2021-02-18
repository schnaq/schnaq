(ns schnaq.interface.views.discussion.badges
  (:require [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.localstorage :as ls]))

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
        old-statements-nums-map @(rf/subscribe [:visited/load-statement-nums])
        old-statement-num (get old-statements-nums-map (str (:db/id statement)) 0)
        statement-num (get-in statement [:meta/sub-discussion-info :sub-statements] 0)
        new? (not (= old-statement-num statement-num))
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
       :data-content (build-author-list (get-in statement [:meta/sub-discussion-info :authors]))}
      [:i {:class (str "m-auto fas " (fa :user/group))}] " "
      (-> statement :meta/sub-discussion-info :authors count)]
     (when edit-hash
       [delete-clicker statement edit-hash])]))

(defn- static-info-badges
  "Badges that display schnaq info."
  [meta-info]
  (let [statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))]
    [:p.mb-0
     [:span.badge.badge-pill.badge-transparent.mr-2
      [:i {:class (str "m-auto fas " (fa :comment))}]
      " " statement-count]
     [:span.badge.badge-pill.badge-transparent.mr-2
      {:tabIndex 20
       :title (labels :discussion.badges/user-overview)}
      [:i {:class (str "m-auto fas " (fa :user/group))}] " " user-count]]))

(defn current-schnaq-info-badges
  "Badges that display info of the current schnaq."
  []
  (let [current-schnaq @(rf/subscribe [:schnaq/selected])]
    [static-info-badges (:meta-info current-schnaq)]))

(defn schnaq-info-badges
  "Badges that display info of a schnaq."
  [schnaq]
  [static-info-badges (get schnaq :meta-info)])

;; #### Subs ####

(rf/reg-event-db
  :schnaqs/store-meta-info-by-hash
  (fn [db [_ share-hash meta-info]]
    (assoc-in db [:schnaqs :meta-info share-hash] meta-info)))

(rf/reg-sub
  :schnaqs/get-meta-info-by-hash
  (fn [db [_ share-hash]]
    (get-in db [:schnaqs :meta-info (str share-hash)])))

(rf/reg-sub
  :current-schnaq/meta-info
  (fn [db [_]]
    (let [starting-conclusions (get-in db [:discussion :conclusions :starting])
          n-conclusions (count starting-conclusions)
          fn-get-meta-info (fn [starting] (-> starting :meta/sub-discussion-info))
          all-meta-infos (map fn-get-meta-info starting-conclusions)
          fn-add-values (fn [{statements-v1 :all-statements authors-v1 :authors}
                             {statements-v2 :sub-statements authors-v2 :authors}]
                          {:all-statements (+ statements-v1 statements-v2)
                           :authors (conj authors-v1 authors-v2)})
          schnaq-info (reduce fn-add-values
                              {:all-statements n-conclusions :authors #{}}
                              all-meta-infos)]
      schnaq-info)))

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
