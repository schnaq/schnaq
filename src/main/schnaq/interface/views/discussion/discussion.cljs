(ns schnaq.interface.views.discussion.discussion
  (:require [ajax.core :as ajax]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.discussion.carousel :as carousel]
            [schnaq.interface.views.discussion.logic :as logic]
            [schnaq.interface.views.discussion.view-elements :as view]))

(defn- add-premise-form
  "Either support or attack or undercut with the users own premise."
  []
  (let [history @(rf/subscribe [:discussion-history])]
    [:form
     {:on-submit (fn [e]
                   (js-wrap/prevent-default e)
                   (logic/submit-new-premise (oget e [:target :elements])))}
     ;; support
     [view/radio-button "for-radio" "premise-choice" "for-radio" :discussion/add-premise-supporting true]
     ;; attack
     [view/radio-button "against-radio" "premise-choice" "against-radio" :discussion/add-premise-against false]
     ;; undercut
     ;; Only show undercut, when there are at least two items in the history (otherwise we can not undercut anything)
     (when (< 1 (count history))
       [view/radio-button "undercut-radio" "premise-choice" "undercut-radio" :discussion/add-undercut false])
     ;; input form
     [view/input-form]]))

(defn- discussion-base-page
  "Base discussion view containing a nav header, meeting title and content container."
  [meeting content]
  [:<>
   [base/meeting-header meeting]
   [:div.container.discussion-base
    [:div.discussion-view-rounded.shadow-straight
     content]]])

(defn- discussion-start-view
  "The first step after starting a discussion."
  []
  (let [current-meeting @(rf/subscribe [:meeting/selected])]
    [discussion-base-page current-meeting
     [:<>
      [view/agenda-header-with-back-arrow
       current-meeting
       (fn []
         (rf/dispatch [:navigation/navigate :routes.meeting/show
                       {:share-hash (:meeting/share-hash current-meeting)}])
         (rf/dispatch [:meeting/select-current current-meeting]))]
      [view/conclusions-list @(rf/subscribe [:discussion.conclusions/starting])
       (:meeting/share-hash current-meeting)]
      [view/input-field]]]))

(rf/reg-event-fx
  :discussion.query.conclusions/starting
  (fn [{:keys [db]} _]
    (let [{:keys [id share-hash]} (get-in db [:current-route :parameters :path])]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/conclusions/starting")
                          :format (ajax/transit-request-format)
                          :params {:share-hash share-hash
                                   :discussion-id id}
                          :response-format (ajax/transit-response-format)
                          :on-success [:discussion.query.conclusions/set-starting]
                          :on-failure [:ajax.error/to-console]}]]})))

(rf/reg-event-fx
  :discussion.query.conclusions/set-starting
  (fn [{:keys [db]} [_ {:keys [starting-conclusions]}]]
    {:db (assoc-in db [:discussion :conclusions :starting] starting-conclusions)
     :fx [[:dispatch [:votes.local/reset]]]}))

(rf/reg-sub
  :discussion.conclusions/starting
  (fn [db _]
    (get-in db [:discussion :conclusions :starting] [])))

(defn discussion-start-view-entrypoint []
  [discussion-start-view])

(defn- present-conclusion-view
  [add-form]
  (let [current-premises @(rf/subscribe [:discussion.premises/current])
        current-meeting @(rf/subscribe [:meeting/selected])]
    [discussion-base-page current-meeting
     [:<>
      [view/agenda-header-with-back-arrow current-meeting
       #(rf/dispatch [:discussion.history/time-travel 1])]
      [view/history-view (:meeting/share-hash current-meeting)]
      [view/input-footer add-form]
      [carousel/carousel-element current-premises (:meeting/share-hash current-meeting)]]]))

(defn selected-conclusion
  "The view after a user has selected any conclusion."
  []
  [present-conclusion-view [add-premise-form]])

(defn- rewind-history
  "Rewinds a history until the last time statement-id was current."
  [history statement-id]
  (loop [history history]
    (if (or (= (:db/id (last history)) statement-id)
            (empty? history))
      (vec history)
      (recur (butlast history)))))

(rf/reg-event-db
  :discussion.history/push
  ;; IMPORTANT: Since we do not control what happens at the browser back button, pushing anything
  ;; that is already present in the history, will rewind the history to said place.
  (fn [db [_ statement]]
    (let [all-entries (-> db :history :full-context)
          history-ids (into #{} (map :db/id all-entries))]
      (if (and statement (contains? history-ids (:db/id statement)))
        (assoc-in db [:history :full-context] (rewind-history all-entries (:db/id statement)))
        (update-in db [:history :full-context] conj statement)))))

(rf/reg-event-db
  :discussion.history/clear
  (fn [db _]
    (assoc-in db [:history :full-context] [])))

(rf/reg-event-fx
  :discussion.history/time-travel
  (fn [{:keys [db]} [_ times]]
    ;; Only continue when default value (nil - go back one step) is set or we go back more than 0 steps
    (when (or (nil? times) (< 0 times))
      (let [steps-back (or times 1)
            before-time-travel (get-in db [:history :full-context])
            keep-n (- (count before-time-travel) steps-back)
            after-time-travel (vec (take keep-n before-time-travel))
            {:keys [id share-hash]} (get-in db [:current-route :parameters :path])]
        (if (>= 0 keep-n)
          {:fx [[:dispatch [:discussion.history/clear]]
                [:dispatch [:navigation/navigate :routes.discussion/start {:id id :share-hash share-hash}]]]}
          {:db (assoc-in db [:history :full-context] after-time-travel)
           :fx [[:dispatch [:navigation/navigate :routes.discussion.select/statement
                            {:id id :share-hash share-hash :statement-id (:db/id (last after-time-travel))}]]]})))))

(rf/reg-event-fx
  :notification/new-content
  (fn [_ _]
    {:fx [[:dispatch [:notification/add
                      #:notification{:title (labels :discussion.notification/new-content-title)
                                     :body (labels :discussion.notification/new-content-body)
                                     :context :success}]]]}))

(rf/reg-sub
  :discussion-history
  (fn [db _]
    (get-in db [:history :full-context])))

(rf/reg-event-db
  :votes.local/reset
  (fn [db _]
    (assoc db :votes {:up {}
                      :down {}})))

(rf/reg-sub
  :local-votes
  (fn [db _]
    (get db :votes)))