(ns schnaq.interface.views.discussion.conclusion-card
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.config :refer [default-anonymous-display-name]]
            [schnaq.interface.text.display-data :refer [fa labels img-path]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.logic :as logic]
            [schnaq.interface.views.user :as user]))

(defn- call-to-contribute
  "If no contributions are available, add a call to action to engage the users."
  []
  [common/delayed-fade-in
   [:article.w-75.mx-auto.pt-5
    [:div.alert.alert-primary.text-center.row
     [:div.col-sm-8.col-12
      [:p.lead.pt-3 (labels :call-to-contribute/lead)]
      [:p (labels :call-to-contribute/body)]
      [:p (labels :how-to/ask-question)
       [:a {:href (reitfe/href :routes/how-to)}
        (labels :how-to/answer-question)]]]
     [:div.col-sm-4.col-12.p-3.p-md-0
      [:img.w-75 {:src (img-path :schnaqqifant.300w/talk)}]]]]])


;; -----------------------------------------------------------------------------

(defn up-down-vote-breaking
  "Add panel for up and down votes."
  [statement]
  (let [votes @(rf/subscribe [:local-votes])]
    [:div.float-right
     [:div.d-flex
      [:div.px-2
       {:on-click (fn [e]
                    (js-wrap/stop-propagation e)
                    (rf/dispatch [:discussion/toggle-upvote statement]))}
       [:div.vote-box.up-vote
        [:i {:class (str "m-auto fas " (fa :arrow-up))}]]]]
     [:h6.d-flex.p-2.m-0 (logic/calculate-votes statement votes)]
     [:div.d-flex
      [:div.px-2
       {:on-click (fn [e]
                    (js-wrap/stop-propagation e)
                    (rf/dispatch [:discussion/toggle-downvote statement]))}
       [:div.vote-box.down-vote.align-bottom
        [:i {:class (str "m-auto fas " (fa :arrow-down))}]]]]]))

(defn- up-down-vote
  "Add inline panel for up and down votes."
  [statement]
  (let [votes @(rf/subscribe [:local-votes])]
    [:div.d-flex {:on-click (fn [e] (js-wrap/stop-propagation e))}
     [:div.px-2
      {:on-click (fn [e]
                   (js-wrap/stop-propagation e)
                   (rf/dispatch [:discussion/toggle-upvote statement]))}
      [:div.vote-box.up-vote [:i.vote-arrow {:class (str "m-auto fas " (fa :arrow-up))}]]]
     [:h6.m-0 (logic/calculate-votes statement votes)]
     [:div.pl-2
      {:on-click (fn [e]
                   (js-wrap/stop-propagation e)
                   (rf/dispatch [:discussion/toggle-downvote statement]))}
      [:div.vote-box.down-vote [:i {:class (str "m-auto fas " (fa :arrow-down))}]]]]))

(defn- statement-card
  [edit-hash {:keys [statement/content] :as statement} attitude]
  (let [fa-label (logic/attitude->symbol attitude)]
    [:div.card.card-rounded.clickable.shadow-straight-light
     {:class (str "statement-card-" (name attitude))}
     [:div.d-flex.flex-row
      [:div.m-auto
       [:i.card-view-type {:class (str "fas " (fa fa-label))}]]
      [:div.card-view.card-body.py-0.pb-1
       [:div.d-flex.mt-1
        [:div.ml-auto
         [user/user-info (-> statement :statement/author :user/nickname) 32]]]
       [:div.my-1 [:p content]]
       [:div.d-flex
        [:div.mr-auto [badges/extra-discussion-info-badges statement edit-hash]]
        [up-down-vote statement]]]]]))

(defn conclusion-cards-list
  "Displays a list of conclusions."
  [conclusions share-hash]
  (let [path-params (:path-params @(rf/subscribe [:navigation/current-route]))
        admin-access-map @(rf/subscribe [:schnaqs/load-admin-access])
        edit-hash (get admin-access-map share-hash)]
    (if (seq conclusions)
      (let [sort-method @(rf/subscribe [:discussion.statements/sort-method])
            keyfn (case sort-method
                    :newest :db/txInstant
                    :popular #(logic/calculate-votes % @(rf/subscribe [:local-votes])))
            sorted-conclusions (sort-by keyfn > conclusions)]
        [:div.card-columns.card-columns-discussion.pb-3
         (for [conclusion sorted-conclusions]
           [:div {:key (:db/id conclusion)
                  :on-click (fn [_e]
                              (let [selection (.toString (.getSelection js/window))]
                                (when (zero? (count selection))
                                  (rf/dispatch [:discussion.select/conclusion conclusion])
                                  (rf/dispatch [:discussion.history/push conclusion])
                                  (rf/dispatch [:navigation/navigate :routes.schnaq.select/statement
                                                (assoc path-params :statement-id (:db/id conclusion))]))))}
            [statement-card edit-hash conclusion (logic/arg-type->attitude (:meta/argument-type conclusion))]])])
      [call-to-contribute])))

(rf/reg-event-fx
  :discussion.select/conclusion
  (fn [{:keys [db]} [_ conclusion]]
    (let [share-hash (get-in db [:current-route :parameters :path :share-hash])]
      {:db (assoc-in db [:discussion :conclusions :selected] conclusion)
       :fx [(http/xhrio-request db :post "/discussion/statements/for-conclusion"
                                [:discussion.premises/set-current]
                                {:selected-statement conclusion
                                 :share-hash share-hash}
                                [:ajax.error/as-notification])]})))

(rf/reg-event-db
  :discussion.premises/set-current
  (fn [db [_ {:keys [premises undercuts]}]]
    (assoc-in db [:discussion :premises :current] (concat premises undercuts))))

(rf/reg-sub
  :local-votes
  (fn [db _]
    (get db :votes)))

(rf/reg-event-fx
  :discussion/toggle-upvote
  (fn [{:keys [db]} [_ {:keys [db/id] :as statement}]]
    {:fx [(http/xhrio-request db :post "/votes/up/toggle" [:upvote-success statement]
                              {:statement-id id
                               :nickname (get-in db [:user :names :display] default-anonymous-display-name)
                               :share-hash (-> db :schnaq :selected :discussion/share-hash)}
                              [:ajax.error/as-notification])]}))

(rf/reg-event-fx
  :discussion/toggle-downvote
  (fn [{:keys [db]} [_ {:keys [db/id] :as statement}]]
    {:fx [(http/xhrio-request db :post "/votes/down/toggle" [:downvote-success statement]
                              {:statement-id id
                               :nickname (get-in db [:user :names :display] default-anonymous-display-name)
                               :share-hash (-> db :schnaq :selected :discussion/share-hash)}
                              [:ajax.error/as-notification])]}))

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