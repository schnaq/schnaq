(ns schnaq.interface.views.discussion.card-elements
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [default-anonymous-display-name]]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.conclusion-card :as cards]
            [schnaq.interface.views.howto.elements :as how-to-elements]
            [schnaq.interface.views.user :as user]
            [schnaq.user :as user-utils]))

(defn- home-button-mobile
  "Home button for history view"
  [history-length]
  [:div.d-inline-block.d-md-block.pr-2.pr-md-0.mt-md-4.pt-2.pt-md-0
   [:div.clickable.card-history-home.text-center
    {:on-click
     #(rf/dispatch [:discussion.history/time-travel history-length])}
    [tooltip/block-element
     :right
     (labels :history.home/tooltip)
     [:div [:small (labels :history.home/text)]]]]])

(defn history-view-mobile
  "History view displayed in the left column in the desktop view."
  [history]
  (let [indexed-history (map-indexed #(vector (- (count history) %1 1) %2) history)]
    [:<>
     ;; home button
     [home-button-mobile (count indexed-history)]
     ;; history
     (for [[index statement] indexed-history]
       (let [user (:statement/author statement)
             nickname (user-utils/statement-author statement)]
         [:div.d-inline-block.d-md-block.pr-2.pr-md-0.text-dark.pt-2.pt-md-0
          {:key (str "history-" (:db/id statement))}
          (let [attitude (name (or (:statement/type statement) :neutral))]
            [:div.card-history.clickable.mt-md-4
             {:class (str "statement-card-" attitude " mobile-attitude-" attitude)
              :on-click #(rf/dispatch [:discussion.history/time-travel index])}
             [:div.history-card-content.text-center
              [tooltip/block-element
               :right
               (str (labels :tooltip/history-statement) nickname)
               [common/avatar user 42]]]])]))]))

(defn- back-button
  "Return to your schnaqs Button"
  [has-history?]
  (let [back-feed [:navigation/navigate :routes.schnaqs/personal]
        back-history [:discussion.history/time-travel 1]
        navigation (if has-history? back-history back-feed)
        label (if has-history? :history.back/text :history.all-schnaqs/text)
        tooltip (if has-history? :history.back/tooltip :history.all-schnaqs/tooltip)]
    [:div.d-inline-block.text-dark.w-100
     [:div.clickable.card-history-home
      {:on-click
       #(rf/dispatch navigation)}
      [tooltip/block-element
       :right
       (labels tooltip)
       [:div.d-flex
        [:i.mt-1.mr-3 {:class (str "fa " (fa :arrow-left))}]
        [:div [:h5 (labels label)]]]]]]))

(defn- discussion-start-button
  "Discussion start button for history view"
  [history-length]
  (let [schnaq @(rf/subscribe [:schnaq/selected])
        title (:discussion/title schnaq)]
    [:div.clickable.card-history-home.text-dark
     {:on-click #(rf/dispatch [:discussion.history/time-travel history-length])}
     [tooltip/block-element
      :right
      (labels :history.home/tooltip)
      [:div.text-center
       [:h6 title]
       [:p.text-muted.mb-0 (labels :history.home/text)]
       [badges/static-info-badges schnaq]]]]))

(defn history-view
  "History view displayed in the left column in the desktop view."
  [history]
  (let [indexed-history (map-indexed #(vector (- (count history) %1 1) %2) history)
        has-history? (seq indexed-history)]
    [:<>
     ;; my schnaqs button
     [back-button has-history?]
     ;; discussion start button
     (when has-history?
       [:section.history-wrapper
        [:h5.p-2.text-center (labels :history/title)]
        [discussion-start-button (count indexed-history)]
        ;; history
        (for [[index statement] indexed-history]
          (let [max-word-count 20
                nickname (user-utils/statement-author statement)
                user (:statement/author statement)
                statement-content (-> statement :statement/content)
                tooltip (str (labels :tooltip/history-statement) nickname)
                history-content [:<>
                                 [:div.d-flex.flex-row
                                  [:h6 (str (labels :history.statement/user) nickname)]
                                  [:div.ml-auto [common/avatar user 22]]]
                                 (toolbelt/truncate-to-n-words statement-content max-word-count)]]
            [:article {:key (str "history-container-" (:db/id statement))}
             [:div.history-thread-line {:key (str "history-divider-" (:db/id statement))}]
             [:div.d-inline-block.d-md-block.text-dark
              {:key (str "history-" (:db/id statement))}
              (let [attitude (name (or (:statement/type statement) :neutral))]
                [:div.card-history.clickable
                 {:class (str "statement-card-" attitude " mobile-attitude-" attitude)
                  :on-click #(rf/dispatch [:discussion.history/time-travel index])}
                 [:div.history-card-content
                  (if (zero? index)
                    history-content
                    [tooltip/block-element :right tooltip history-content])]])]]))])]))

(defn- graph-button
  "Rounded square button to navigate to the graph view"
  [share-hash]
  [:button.btn.btn-sm.btn-outline-primary.shadow-sm.rounded-2
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
          nickname (get-in db [:user :names :display] default-anonymous-display-name)
          statement-text (oget form [:statement-text :value])]
      {:fx [(http/xhrio-request db :post "/discussion/statements/starting/add"
                                [:discussion.add.statement/starting-success form]
                                {:statement statement-text
                                 :nickname nickname
                                 :share-hash share-hash}
                                [:ajax.error/as-notification])]})))

(rf/reg-event-fx
  :discussion.add.statement/starting-success
  (fn [_ [_ form new-starting-statements]]
    (let [starting-conclusions (:starting-conclusions new-starting-statements)
          statement-with-creation-secret (first (filter :statement/creation-secret
                                                        starting-conclusions))]
      {:fx [[:dispatch [:notification/add
                        #:notification{:title (labels :discussion.notification/new-content-title)
                                       :body (labels :discussion.notification/new-content-body)
                                       :context :success}]]
            [:dispatch [:discussion.query.conclusions/set-starting new-starting-statements]]
            (when (= 1 (count starting-conclusions))
              [:dispatch [:celebrate/schnaq-filled]])
            (when statement-with-creation-secret
              [:dispatch [:discussion.statements/add-creation-secret statement-with-creation-secret]])
            [:form/clear form]]})))

(rf/reg-event-fx
  :discussion.query.conclusions/starting
  (fn [{:keys [db]} _]
    (let [share-hash (get-in db [:current-route :parameters :path :share-hash])]
      {:fx [(http/xhrio-request db :post "/discussion/conclusions/starting"
                                [:discussion.query.conclusions/set-starting]
                                {:share-hash share-hash})]})))

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
    [:div.text-center.privacy-indicator
     (if public?
       [:span.badge.badge-secondary-outline
        [:i {:class (str "m-auto fas fa-lg " (fa :lock-open))}] " "
        (labels :discussion.privacy/public)]
       [:span.badge.badge-primary-outline
        [:i {:class (str "m-auto fas fa-lg " (fa :shield))}] " "
        (labels :discussion.privacy/private)])]))

(defn- title-and-input-element
  "Element containing Title and textarea input"
  [content input is-topic?]
  (let [title (:content content)
        read-only? @(rf/subscribe [:schnaq.selected/read-only?])]
    [:<>
     [toolbelt/desktop-mobile-switch
      (if is-topic?
        [:h2.align-self-center title]
        [:h6 title])
      [:h2.align-self-center.display-6 title]]
     [:div.line-divider.my-4]
     (if read-only?
       [:div.alert.alert-warning (labels :discussion.state/read-only-warning)]
       input)]))

(defn- topic-bubble-desktop [{:discussion/keys [share-hash] :as discussion} content input badges info-content is-topic?]
  [:div.row
   ;; graph
   [:div.col-2
    [graph-button share-hash]
    [:div.mt-3 badges]]
   ;; title
   [:div.col-8
    [:div.d-flex.mb-4
     [discussion-privacy-badge discussion]
     [:div.ml-auto
      [user/user-info (:author content) 42 (:statement/created-at content)]]]
    [title-and-input-element content input is-topic?]]
   ;; up-down votes and statistics
   [:div.col-2.pr-3
    [:div.float-right
     info-content]]])

(defn- topic-bubble-mobile [{:discussion/keys [share-hash] :as discussion} content input badges info-content]
  [:<>
   [:div.d-flex.mb-4
    ;; graph and badges
    [:div.mr-auto
     [graph-button share-hash]
     [:div.mt-2 badges]]
    ;; settings
    [:div.p-0
     [discussion-privacy-badge discussion]
     [:div.d-flex
      [:div.ml-auto.mr-2 [user/user-info (:author content) 32 (:statement/created-at content)]]
      info-content]]]
   ;; title
   [title-and-input-element content input]])

(defn- topic-bubble [content]
  (let [title (:discussion/title @(rf/subscribe [:schnaq/selected]))]
    (common/set-website-title! title)
    [:div.panel-white.md-4
     [:div.discussion-light-background content]]))

(rf/reg-event-db
  :discussion.statements.sort/set
  (fn [db [_ method]]
    (assoc-in db [:discussion :statements :sort-method] method)))

(rf/reg-sub
  :discussion.statements/sort-method
  (fn [db _]
    (get-in db [:discussion :statements :sort-method] :newest)))

(defn- sort-options
  "Displays the different sort options for card elements."
  []
  (let [sort-method @(rf/subscribe [:discussion.statements/sort-method])]
    [:section.py-2.text-right
     [:p.small.mb-0
      (labels :badges.sort/sort)
      [:button.btn.btn-outline-primary.btn-sm.mx-1
       {:class (when (= sort-method :newest) "active")
        :on-click #(rf/dispatch [:discussion.statements.sort/set :newest])}
       (labels :badges.sort/newest)]
      [:button.btn.btn-outline-primary.btn-sm
       {:class (when (= sort-method :popular) "active")
        :on-click #(rf/dispatch [:discussion.statements.sort/set :popular])}
       (labels :badges.sort/popular)]]]))

(defn- topic-view [{:keys [discussion/share-hash]} conclusions topic-content]
  [:<>
   [topic-bubble topic-content]
   [sort-options]
   [cards/conclusion-cards-list conclusions share-hash]])

(defn- show-how-to [is-topic?]
  (if is-topic?
    [how-to-elements/quick-how-to-schnaq]
    [how-to-elements/quick-how-to-pro-con]))

(defn discussion-view-mobile
  "Discussion view for mobile devices
  No history but fullscreen topic bubble and conclusions"
  [current-discussion content input badges info-content conclusions history]
  (let [is-topic? (nil? history)]
    [:<>
     [topic-view current-discussion conclusions
      [topic-bubble-mobile current-discussion content input badges info-content]]
     [show-how-to is-topic?]]))

(defn discussion-view-desktop
  "Discussion View for desktop devices.
  Displays a history on the left and a topic with conclusion in its center"
  [current-discussion statement input badges info-content conclusions history]
  (let [is-topic? (nil? history)]
    [:div.container-fluid
     [:div.row.px-0.mx-0
      [:div.col-3.py-4
       [history-view history]]
      [:div.col-9.py-4.px-0
       [topic-view current-discussion conclusions
        [topic-bubble-desktop current-discussion statement input badges info-content is-topic?]]
       [:div.w-75.mx-auto [show-how-to is-topic?]]]]]))

(defn info-content-conclusion
  "Badges and up/down-votes to be displayed in the topic bubble."
  [statement]
  [cards/up-down-vote-breaking statement])
