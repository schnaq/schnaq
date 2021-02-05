(ns schnaq.interface.views.discussion.conclusion-card
  (:require [ajax.core :as ajax]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.logic :as logic]
            [schnaq.interface.views.user :as user]))

(rf/reg-sub
  :local-votes
  (fn [db _]
    (get db :votes)))

(rf/reg-event-fx
  :discussion/toggle-upvote
  (fn [{:keys [db]} [_ {:keys [db/id] :as statement}]]
    {:fx [[:http-xhrio {:method :post
                        :uri (str (:rest-backend config) "/votes/up/toggle")
                        :format (ajax/transit-request-format)
                        :params {:statement-id id
                                 :nickname (get-in db [:user :name] "Anonymous")
                                 :share--hash (-> db :schnaq :selected :discussion/share-hash)}
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
                                 :share-hash (-> db :schnaq :selected :discussion/share-hash)}
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

(defn up-down-vote-breaking
  "Add panel for up and down votes."
  [statement]
  (let [votes @(rf/subscribe [:local-votes])]
    [:div.float-right
     [:div.d-flex
      [:div.vote-box.up-vote
       {:on-click (fn [e]
                    (js-wrap/stop-propagation e)
                    (rf/dispatch [:discussion/toggle-upvote statement]))}
       [:h6 [:i {:class (str "m-auto fas fa-lg " (fa :arrow-up))}]]]
      [:h6.ml-1.pt-1 (logic/calculate-votes statement :upvotes votes)]]
     [:div.d-flex
      [:div.vote-box.down-vote.align-bottom
       {:on-click (fn [e]
                    (js-wrap/stop-propagation e)
                    (rf/dispatch [:discussion/toggle-downvote statement]))}
       [:h6 [:i {:class (str "m-auto fas fa-lg " (fa :arrow-down))}]]]
      [:h6.ml-1.pt-1 (logic/calculate-votes statement :downvotes votes)]]]))

(defn- statement-card
  [edit-hash {:keys [statement/content] :as statement} attitude]
  (let [fa-label (logic/attitude->symbol attitude)]
    [:div.card.card-rounded.clickable.shadow-straight-light
     {:class (str "statement-card-" (name attitude))}
     [:div.d-flex.flex-row
      [:div.m-auto
       [:i.card-view-type {:class (str "fas " (fa fa-label))}]]
      [:div.card-view.card-body.py-0.pb-1
       [:div.d-flex.pt-3
        [:div.mr-auto [:p content]]
        [:div.pb-2 [up-down-vote-breaking statement]]]
       [:div.d-flex
        [:div.mr-auto [badges/extra-discussion-info-badges statement edit-hash]]
        [:div.float-right.pb-1
         [user/user-info (-> statement :statement/author :user/nickname) 32]]]]]]))

(defn conclusion-cards-list
  "Displays a list of conclusions."
  [conclusions share-hash]
  (let [path-params (:path-params @(rf/subscribe [:navigation/current-route]))
        admin-access-map @(rf/subscribe [:schnaqs/load-admin-access])
        edit-hash (get admin-access-map share-hash)]
    [:div.card-columns.card-columns-discussion.py-3
     (for [conclusion conclusions]
       [:div {:key (:db/id conclusion)
              :on-click (fn [_e]
                          (let [selection (.toString (.getSelection js/window))]
                            (when (zero? (count selection))
                              (rf/dispatch [:discussion.select/conclusion conclusion])
                              (rf/dispatch [:discussion.history/push conclusion])
                              (rf/dispatch [:navigation/navigate :routes.schnaq.select/statement
                                            (assoc path-params :statement-id (:db/id conclusion))]))))}
        [statement-card edit-hash conclusion (logic/arg-type->attitude (:meta/argument-type conclusion))]])]))

(rf/reg-event-fx
  :discussion.select/conclusion
  (fn [{:keys [db]} [_ conclusion]]
    (let [share-hash (get-in db [:current-route :parameters :path :share-hash])]
      {:db (assoc-in db [:discussion :conclusions :selected] conclusion)
       :fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/statements/for-conclusion")
                          :format (ajax/transit-request-format)
                          :params {:selected-statement conclusion
                                   :share-hash share-hash}
                          :response-format (ajax/transit-response-format)
                          :on-success [:discussion.premises/set-current]
                          :on-failure [:ajax.error/as-notification]}]]})))

(rf/reg-event-db
  :discussion.premises/set-current
  (fn [db [_ {:keys [premises undercuts]}]]
    (assoc-in db [:discussion :premises :current] (concat premises undercuts))))