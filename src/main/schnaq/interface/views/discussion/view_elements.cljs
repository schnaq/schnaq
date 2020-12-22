(ns schnaq.interface.views.discussion.view-elements
  (:require [ajax.core :as ajax]
            [ghostwheel.core :refer [>defn-]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels fa img-path]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))

(rf/reg-event-fx
  :discussion.add.statement/starting
  (fn [{:keys [db]} [_ form]]
    (let [{:keys [id share-hash]} (get-in db [:current-route :parameters :path])
          nickname (get-in db [:user :name] "Anonymous")
          statement-text (oget form [:statement-text :value])]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/statements/starting/add")
                          :format (ajax/transit-request-format)
                          :params {:statement statement-text
                                   :nickname nickname
                                   :share-hash share-hash
                                   :discussion-id id}
                          :response-format (ajax/transit-response-format)
                          :on-success [:discussion.add.statement/starting-success form]
                          :on-failure [:ajax.error/as-notification]}]]})))

(rf/reg-event-fx
  :discussion.add.statement/starting-success
  (fn [_ [_ form new-starting-statements]]
    {:fx [[:dispatch [:notification/add
                      #:notification{:title (labels :discussion.notification/new-content-title)
                                     :body (labels :discussion.notification/new-content-body)
                                     :context :success}]]
          [:dispatch [:discussion.query.conclusions/set-starting new-starting-statements]]
          [:form/clear form]]}))

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

(rf/reg-event-fx
  :discussion.query.statement/by-id
  (fn [{:keys [db]} _]
    (let [{:keys [id share-hash statement-id]} (get-in db [:current-route :parameters :path])]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/statement/info")
                          :format (ajax/transit-request-format)
                          :params {:statement-id statement-id
                                   :share-hash share-hash
                                   :discussion-id id}
                          :response-format (ajax/transit-response-format)
                          :on-success [:discussion.query.statement/by-id-success]
                          :on-failure [:ajax.error/as-notification]}]]})))

(rf/reg-event-fx
  :discussion.query.statement/by-id-success
  (fn [{:keys [db]} [_ {:keys [conclusion premises undercuts]}]]
    {:db (->
           (assoc-in db [:discussion :conclusions :selected] conclusion)
           (assoc-in [:discussion :premises :current] (concat premises undercuts)))
     :fx [[:dispatch [:discussion.history/push conclusion]]]}))

(rf/reg-event-fx
  :discussion.select/conclusion
  (fn [{:keys [db]} [_ conclusion]]
    (let [{:keys [id share-hash]} (get-in db [:current-route :parameters :path])]
      {:db (assoc-in db [:discussion :conclusions :selected] conclusion)
       :fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/statements/for-conclusion")
                          :format (ajax/transit-request-format)
                          :params {:selected-statement conclusion
                                   :share-hash share-hash
                                   :discussion-id id}
                          :response-format (ajax/transit-response-format)
                          :on-success [:discussion.premises/set-current]
                          :on-failure [:ajax.error/as-notification]}]]})))

(rf/reg-event-db
  :discussion.premises/set-current
  (fn [db [_ {:keys [premises undercuts]}]]
    (assoc-in db [:discussion :premises :current] (concat premises undercuts))))

(rf/reg-sub
  :discussion.premises/current
  (fn [db _]
    (get-in db [:discussion :premises :current] [])))

(rf/reg-sub
  :discussion.conclusions/selected
  (fn [db _]
    (get-in db [:discussion :conclusions :selected])))

(rf/reg-event-fx
  :discussion/toggle-upvote
  (fn [{:keys [db]} [_ {:keys [db/id] :as statement}]]
    {:fx [[:http-xhrio {:method :post
                        :uri (str (:rest-backend config) "/votes/up/toggle")
                        :format (ajax/transit-request-format)
                        :params {:statement-id id
                                 :nickname (get-in db [:user :name] "Anonymous")
                                 :meeting-hash (-> db :meeting :selected :meeting/share-hash)}
                        :response-format (ajax/transit-response-format)
                        :on-success [:upvote-success statement]
                        :on-failure [:ajax.error/as-notification]}]]}))

(rf/reg-event-fx
  :discussion/toggle-downvote
  (fn [{:keys [db]} [_ {:keys [db/id] :as statement}]]
    {:fx [[:http-xhrio {:method :post
                        :uri (str (:rest-backend config) "/votes/down/toggle")
                        :format (ajax/transit-request-format)
                        :params {:statement-id id
                                 :nickname (get-in db [:user :name] "Anonymous")
                                 :meeting-hash (-> db :meeting :selected :meeting/share-hash)}
                        :response-format (ajax/transit-response-format)
                        :on-success [:downvote-success statement]
                        :on-failure [:ajax.error/as-notification]}]]}))

(rf/reg-event-db
  :upvote-success
  (fn [db [_ {:keys [db/id]} {:keys [operation]}]]
    (case operation
      :added (update-in db [:votes :up id] inc)
      :removed (update-in db [:votes :up id] dec)
      :switched (update-in
                  (update-in db [:votes :up id] inc)
                  [:votes :down id] dec))))

(rf/reg-event-db
  :downvote-success
  (fn [db [_ {:keys [db/id]} {:keys [operation]}]]
    (case operation
      :added (update-in db [:votes :down id] inc)
      :removed (update-in db [:votes :down id] dec)
      :switched (update-in
                  (update-in db [:votes :down id] inc)
                  [:votes :up id] dec))))
