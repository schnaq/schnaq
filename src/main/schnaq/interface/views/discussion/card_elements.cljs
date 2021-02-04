(ns schnaq.interface.views.discussion.card-elements
  (:require [ajax.core :as ajax]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.conclusion-card :as cards]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.logic :as logic]
            [schnaq.interface.views.user :as user]))

(defn- home-button-mobile
  "Home button for history view"
  [history-length]
  [:div.d-inline-block.d-md-block.pr-2.pr-md-0.mt-md-4.pt-2.pt-md-0
   [:div.clickable.card-history-home.text-center
    {:on-click
     #(rf/dispatch [:discussion.history/time-travel history-length])}
    [tooltip/nested-div
     "right"
     (labels :history.home/tooltip)
     [:<> [:div [:small (labels :history.home/text)]]]]]])

(defn history-view-mobile
  "History view displayed in the left column in the desktop view."
  [history]
  (let [indexed-history (map-indexed #(vector (- (count history) %1 1) %2) history)]
    [:<>
     ;; home button
     [home-button-mobile (count indexed-history)]
     ;; history
     (for [[index statement] indexed-history]
       (let [nickname (-> statement :statement/author :user/nickname)]
         [:div.d-inline-block.d-md-block.pr-2.pr-md-0.text-dark.pt-2.pt-md-0
          {:key (str "history-" (:db/id statement))}
          (let [attitude (name (logic/arg-type->attitude (:meta/argument-type statement)))]
            [:div.card-history.clickable.mt-md-4
             {:class (str "statement-card-" attitude " mobile-attitude-" attitude)
              :on-click #(rf/dispatch [:discussion.history/time-travel index])}
             [:div.history-card-content.text-center
              [tooltip/nested-div
               "right"
               (str (labels :tooltip/history-statement) nickname)
               [common/avatar nickname 42]]]])]))]))

(defn- back-to-feed-button
  "Return to your schnaqs Button"
  []
  (let [feed @(rf/subscribe [:feed/get-current])
        feed-route (case feed
                     :personal :routes.meetings/my-schnaqs
                     :routes/public-discussions)]
    [:div.d-inline-block.text-dark.w-100
     [:div.clickable.card-history-home
      {:on-click
       #(rf/dispatch [:navigation/navigate feed-route])}
      [tooltip/nested-div
       "right"
       (labels :history.all-schnaqs/tooltip)
       [:div.d-flex
        [:i.mt-1.mr-3 {:class (str "fa " (fa :arrow-left))}]
        [:div [:h5 (labels :history.all-schnaqs/text)]]]]]]))

(defn- discussion-start-button
  "Discussion start button for history view"
  [history-length]
  [:div.clickable.card-history-home.mt-4.text-dark
   {:on-click #(rf/dispatch [:discussion.history/time-travel history-length])}
   [tooltip/nested-div
    "right"
    (labels :history.home/tooltip)
    [:div.ml-4
     [:div [:h5 (labels :history.home/text)]]
     [:div.mr-auto [badges/current-schnaq-info-badges]]]]])

(defn history-view
  "History view displayed in the left column in the desktop view."
  [history]
  (let [indexed-history (map-indexed #(vector (- (count history) %1 1) %2) history)]
    [:<>
     ;; my schnaqs button
     [back-to-feed-button]
     ;; discussion start button
     (when (seq indexed-history)
       [discussion-start-button (count indexed-history)])
     ;; history
     (for [[index statement] (butlast indexed-history)]
       (let [nickname (-> statement :statement/author :user/nickname)
             content (-> statement :statement/content)]
         [:div {:key (str "history-container-" (:db/id statement))}
          [:div.history-thread-line {:key (str "history-divider-" (:db/id statement))}]
          [:div.d-inline-block.d-md-block.text-dark
           {:key (str "history-" (:db/id statement))}
           (let [attitude (name (logic/arg-type->attitude (:meta/argument-type statement)))]
             [:div.card-history.clickable
              {:class (str "statement-card-" attitude " mobile-attitude-" attitude)
               :on-click #(rf/dispatch [:discussion.history/time-travel index])}
              [:div.history-card-content
               [tooltip/nested-div
                "right"
                (str (labels :tooltip/history-statement) nickname)
                [:<>
                 [:div.d-flex.flex-row
                  [:h6 (str (labels :history.statement/user) nickname)]
                  [:div.ml-auto [common/avatar nickname 22]]]
                 [:span content]]]]])]]))]))

(defn- graph-button
  "Rounded square button to navigate to the graph view"
  [share-hash]
  [:button.btn.btn-sm.btn-outline-primary.rounded-2
   {:on-click #(rf/dispatch
                 [:navigation/navigate :routes/graph-view
                  {:share-hash share-hash}])}
   [:img
    {:src (img-path :icon-graph) :alt (labels :graph.button/text)
     :title (labels :graph.button/text)
     :width "40rem"}]
   [:div (labels :graph.button/text)]])

(rf/reg-event-fx
  :discussion.add.statement/starting
  (fn [{:keys [db]} [_ form]]
    (let [share-hash (get-in db [:current-route :parameters :path :share-hash])
          nickname (get-in db [:user :name] "Anonymous")
          statement-text (oget form [:statement-text :value])]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/statements/starting/add")
                          :format (ajax/transit-request-format)
                          :params {:statement statement-text
                                   :nickname nickname
                                   :share-hash share-hash}
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

(rf/reg-event-fx
  :discussion.query.conclusions/starting
  (fn [{:keys [db]} _]
    (let [share-hash (get-in db [:current-route :parameters :path :share-hash])]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/conclusions/starting")
                          :format (ajax/transit-request-format)
                          :params {:share-hash share-hash}
                          :response-format (ajax/transit-response-format)
                          :on-success [:discussion.query.conclusions/set-starting]
                          :on-failure [:ajax.error/to-console]}]]})))

(rf/reg-event-fx
  :discussion.query.conclusions/set-starting
  (fn [{:keys [db]} [_ {:keys [starting-conclusions]}]]
    {:db (assoc-in db [:discussion :conclusions :starting] starting-conclusions)
     :fx [[:dispatch [:votes.local/reset]]]}))

(rf/reg-event-db
  :votes.local/reset
  (fn [db _]
    (assoc db :votes {:up {}
                      :down {}})))

(defn- discussion-privacy-badge
  "A small badge displaying who can see the discussion!"
  [{:keys [discussion/states]}]
  (let [public? (contains? (set states) :discussion.state/public)]
    [:div.text-center.mt-2.privacy-indicator
     (if public?
       [:span.badge.badge-secondary-outline
        [:i {:class (str "m-auto fas fa-lg " (fa :lock-open))}] " "
        (labels :discussion.privacy/public)]
       [:span.badge.badge-primary-outline
        [:i {:class (str "m-auto fas fa-lg " (fa :shield))}] " "
        (labels :discussion.privacy/private)])]))

(defn- title-and-input-element
  "Element containing Title and textarea input"
  [title input]
  [:<>
   [toolbelt/desktop-mobile-switch
    [:h2.align-self-center title]
    [:h2.align-self-center.display-6 title]]
   [:div.line-divider.my-4]
   input])

(defn- topic-bubble-desktop
  [{:discussion/keys [share-hash] :as discussion} title input info-content]
  [:div.row
   ;; graph
   [:div.col-2
    [graph-button share-hash]]
   ;; title
   [:div.col-8
    [title-and-input-element title input]]
   ;; up-down votes and statistics
   [:div.col-2.pr-3
    [:div.float-right
     info-content
     [discussion-privacy-badge discussion]]]])

(defn- topic-bubble-mobile
  [{:discussion/keys [share-hash] :as discussion} title input info-content]
  [:<>
   [:div.d-flex
    ;; graph
    [:div.mr-auto.mb-5
     [graph-button share-hash]]
    ;; settings
    [:div.p-0
     info-content
     [discussion-privacy-badge discussion]]]
   ;; title
   [title-and-input-element title input]])

(defn- topic-bubble [content]
  (let [title (:discussion/title @(rf/subscribe [:schnaq/selected]))]
    (common/set-website-title! title)
    [:div.topic-view.shadow-straight-light.md-4
     [:div.discussion-light-background content]]))

(defn- topic-view [{:keys [discussion/share-hash]} conclusions topic-content]
  [:<>
   [topic-bubble topic-content]
   [cards/conclusion-cards-list conclusions share-hash]])

(defn discussion-view-mobile
  "Discussion view for mobile devices
  No history but fullscreen topic bubble and conclusions"
  [current-discussion title input info-content conclusions]
  [:<>
   [topic-view current-discussion conclusions
    [topic-bubble-mobile current-discussion title input info-content]]])

(defn discussion-view-desktop
  "Discussion View for desktop devices.
  Displays a history on the left and a topic with conclusion in its center"
  [current-discussion title input info-content conclusions history]
  [:div.container-fluid
   [:div.row.px-0.mx-0
    [:div.col-2.py-4
     [history-view history]]
    [:div.col-9.py-4.px-0
     [topic-view current-discussion conclusions
      [topic-bubble-desktop current-discussion title input info-content]]]]])

(defn info-content-conclusion
  "Badges and up/down-votes to be displayed in the right of the topic bubble."
  [statement edit-hash]
  [:<>
   [cards/up-down-vote-breaking statement]
   [badges/extra-discussion-info-badges statement edit-hash]
   [:div.pt-3
    [user/user-info (-> statement :statement/author :user/nickname) 32]]])
