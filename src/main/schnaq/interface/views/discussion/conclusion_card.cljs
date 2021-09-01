(ns schnaq.interface.views.discussion.conclusion-card
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.config :refer [default-anonymous-display-name]]
            [schnaq.interface.text.display-data :refer [fa labels img-path]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.markdown :as md]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.edit :as edit]
            [schnaq.interface.views.discussion.filters :as filters]
            [schnaq.interface.views.discussion.labels :as labels]
            [schnaq.interface.views.discussion.logic :as logic]
            [schnaq.interface.views.user :as user]))

(defn- call-to-contribute
  "If no contributions are available, add a call to action to engage the users."
  []
  [common/delayed-fade-in
   [:article.m-3
    [:div.alert.alert-light.text-light.row.blue-wave-background.p-md-5
     [:div.col-2.py-md-5.d-flex
      [:img.w-75.align-self-center {:src (img-path :schnaqqifant/flat)}]]
     [:div.col-10.py-md-5
      [:h2 (labels :call-to-contribute/body)]
      [:p.mt-5 (labels :how-to/ask-question)
       [:a.text-dark {:href (reitfe/href :routes/how-to)}
        (labels :how-to/answer-question)]]]]]])


;; -----------------------------------------------------------------------------

(defn up-down-vote
  "Add inline panel for up and down votes."
  [statement]
  (let [votes @(rf/subscribe [:local-votes])]
    [:div.d-flex
     [:div.px-2
      {:on-click (fn [e]
                   (js-wrap/stop-propagation e)
                   (rf/dispatch [:discussion/toggle-upvote statement]))}
      [:i.vote-arrow.up-vote {:class (str "m-auto fas " (fa :arrow-up))}]]
     [:h6.m-0 (logic/calculate-votes statement votes)]
     [:div.pl-2
      {:on-click (fn [e]
                   (js-wrap/stop-propagation e)
                   (rf/dispatch [:discussion/toggle-downvote statement]))}
      [:i.vote-arrow.down-vote {:class (str "m-auto fas " (fa :arrow-down))}]]]))

(defn statement-card
  [edit-hash statement]
  (let [path-params (:path-params @(rf/subscribe [:navigation/current-route]))
        statement-labels (set (:statement/labels statement))]
    [:article.card.statement-card.my-2
     [:div.d-flex.flex-row
      [:div {:class (str "highlight-card-" (name (or (:statement/type statement) :neutral)))}]
      [:div.card-view.card-body.py-2
       (when (:meta/new? statement)
         [:div.bg-primary.p-2.rounded-1.d-inline-block.text-white.small.float-right.mt-n3
          (labels :discussion.badges/new)])
       [:div.d-flex.justify-content-start.pt-2
        [user/user-info statement 42 "w-100"]]
       [:div.my-4]
       [md/as-markdown (:statement/content statement)]
       [:div.d-flex.flex-wrap
        [:a.badge.badge-pill.rounded-2.mr-1
         {:href (reitfe/href :routes.schnaq.select/statement (assoc path-params :statement-id (:db/id statement)))}
         [:i {:class (str "m-auto far " (fa :reply))}] [:span.ml-1 (labels :statement/reply)]]
        [up-down-vote statement]
        [:div.ml-sm-0.ml-lg-auto
         [badges/extra-discussion-info-badges statement edit-hash]]]
       (when (seq statement-labels)
         [:div.mx-1
          (for [label statement-labels]
            [:span.pr-1 {:key (str "show-label-" (:db/id statement) label)}
             [labels/build-label label]])])]]]))

(defn- statement-or-edit-wrapper
  "Either show the clickable statement, or its edit-view."
  [statement edit-hash]
  (let [currently-edited? @(rf/subscribe [:statement.edit/ongoing? (:db/id statement)])]
    (if currently-edited?
      [edit/edit-card statement]
      [statement-card edit-hash statement])))

(defn conclusion-cards-list
  "Displays a list of conclusions."
  [share-hash]
  (let [admin-access-map @(rf/subscribe [:schnaqs/load-admin-access])
        edit-hash (get admin-access-map share-hash)
        card-column-class (if shared-config/embedded? "card-columns-embedded" "card-columns-discussion ")
        current-premises @(rf/subscribe [:discussion.premises/current])]
    (if (seq current-premises)
      (let [sort-method @(rf/subscribe [:discussion.statements/sort-method])
            keyfn (case sort-method
                    :newest :statement/created-at
                    :popular #(logic/calculate-votes % @(rf/subscribe [:local-votes])))
            sorted-conclusions (sort-by keyfn > current-premises)
            active-filters @(rf/subscribe [:filters/active])
            filtered-conclusions (filters/filter-statements sorted-conclusions active-filters (rf/subscribe [:local-votes]))]
        [:div.card-columns.pb-3
         {:class card-column-class}
         (for [statement filtered-conclusions]
           (with-meta
             [statement-or-edit-wrapper statement edit-hash]
             {:key (:db/id statement)}))])
      [call-to-contribute])))

(rf/reg-event-fx
  :discussion.select/conclusion
  (fn [{:keys [db]} [_ conclusion]]
    (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
      {:db (assoc-in db [:discussion :conclusion :selected] conclusion)
       :fx [(http/xhrio-request db :get "/discussion/statements/for-conclusion"
                                [:discussion.premises/set-current]
                                {:conclusion-id (:db/id conclusion)
                                 :share-hash share-hash}
                                [:ajax.error/as-notification])]})))

(rf/reg-event-db
  :discussion.premises/set-current
  (fn [db [_ {:keys [premises]}]]
    (assoc-in db [:discussion :premises :current] premises)))

(rf/reg-sub
  :local-votes
  (fn [db _]
    (get db :votes)))

(rf/reg-event-fx
  :discussion/toggle-upvote
  (fn [{:keys [db]} [_ {:keys [db/id] :as statement}]]
    {:fx [(http/xhrio-request db :post "/discussion/statement/vote/up" [:upvote-success statement]
                              {:statement-id id
                               :nickname (get-in db [:user :names :display] default-anonymous-display-name)
                               :share-hash (-> db :schnaq :selected :discussion/share-hash)}
                              [:ajax.error/as-notification])]}))

(rf/reg-event-fx
  :discussion/toggle-downvote
  (fn [{:keys [db]} [_ {:keys [db/id] :as statement}]]
    {:fx [(http/xhrio-request db :post "/discussion/statement/vote/down" [:downvote-success statement]
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
