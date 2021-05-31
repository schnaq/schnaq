(ns schnaq.interface.views.discussion.conclusion-card
  (:require ["react-markdown" :as ReactMarkdown]
            ["remark-gfm" :as gfm]
            [clojure.string :as cstring]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.config :refer [default-anonymous-display-name]]
            [schnaq.interface.text.display-data :refer [fa labels img-path]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.edit :as edit]
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
        [:i.vote-arrow {:class (str "m-auto fas " (fa :arrow-up))}]]]]
     [:h6.d-flex.p-2.m-0 (logic/calculate-votes statement votes)]
     [:div.d-flex
      [:div.px-2
       {:on-click (fn [e]
                    (js-wrap/stop-propagation e)
                    (rf/dispatch [:discussion/toggle-downvote statement]))}
       [:div.vote-box.down-vote.align-bottom
        [:i.vote-arrow {:class (str "m-auto fas " (fa :arrow-down))}]]]]]))

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
      [:div.vote-box.down-vote [:i.vote-arrow {:class (str "m-auto fas " (fa :arrow-down))}]]]]))

  ;; TODO links in topic-cards parsen

(defn statement-card
  [edit-hash statement]
  (let [path-params (:path-params @(rf/subscribe [:navigation/current-route]))
        on-click-fn (fn [_e]
                      (let [selection (js-wrap/to-string (.getSelection js/window))]
                        (when (zero? (count selection))
                          (rf/dispatch [:discussion.select/conclusion statement])
                          (rf/dispatch [:discussion.history/push statement])
                          (rf/dispatch [:navigation/navigate :routes.schnaq.select/statement
                                        (assoc path-params :statement-id (:db/id statement))]))))]
    [:div {:on-click on-click-fn}
     [:article.card.statement-card.clickable
      {:class (str "statement-card-" (name (or (:statement/type statement) :neutral)))}
      [:div.card-view.card-body.py-0.pb-1
       [:div.d-flex.justify-content-end.pt-2
        [user/user-info (:statement/author statement) 32 (:statement/created-at statement)]]
       [:div.my-1
        [:> ReactMarkdown {:children (:statement/content statement)
                           :remarkPlugins [gfm]}]]
       [:div.d-flex
        [:a.badge.badge-primary.rounded-2.mr-2 {:href "#" :on-click on-click-fn}
         (labels :statement/reply)]
        [badges/extra-discussion-info-badges statement edit-hash]
        [:div.ml-auto
         [up-down-vote statement]]]]]]))

(defn- statement-or-edit-wrapper
  "Either show the clickable statement, or its edit-view."
  [statement edit-hash]
  (let [currently-edited? @(rf/subscribe [:statement.edit/ongoing? (:db/id statement)])]
    (if currently-edited?
      [edit/edit-card statement]
      [statement-card edit-hash statement])))

(defn conclusion-cards-list
  "Displays a list of conclusions."
  [conclusions share-hash]
  (let [admin-access-map @(rf/subscribe [:schnaqs/load-admin-access])
        edit-hash (get admin-access-map share-hash)]
    (if (seq conclusions)
      (let [sort-method @(rf/subscribe [:discussion.statements/sort-method])
            keyfn (case sort-method
                    :newest :statement/created-at
                    :popular #(logic/calculate-votes % @(rf/subscribe [:local-votes])))
            sorted-conclusions (sort-by keyfn > conclusions)]
        [:div.card-columns.card-columns-discussion.pb-3
         (for [statement sorted-conclusions]
           (with-meta
             [statement-or-edit-wrapper statement edit-hash]
             {:key (:db/id statement)}))])
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
  (fn [db [_ {:keys [premises]}]]
    (assoc-in db [:discussion :premises :current] premises)))

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
