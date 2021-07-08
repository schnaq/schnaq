(ns schnaq.interface.views.discussion.card-elements
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [default-anonymous-display-name]]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.utils.markdown :as md]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.conclusion-card :as cards]
            [schnaq.interface.views.discussion.edit :as edit]
            [schnaq.interface.views.discussion.input :as input]
            [schnaq.interface.views.howto.elements :as how-to-elements]
            [schnaq.interface.views.navbar.for-discussions :as discussion-navbar]
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
        tooltip (if has-history? :history.back/tooltip :history.all-schnaqs/tooltip)]
    [:button.btn.btn-dark.w-100.h-100
     {:on-click #(rf/dispatch navigation)}
     [tooltip/block-element
      :bottom
      (labels tooltip)
      [:div.d-flex
       [:i.m-auto {:class (str "fa " (fa :arrow-left))}]]]]))

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
                 {:on-click #(rf/dispatch [:discussion.history/time-travel index])}
                 [:div.d-flex.flex-row
                  [:div {:class (str "highlight-card-" attitude)}]
                  [:div.history-card-content
                   (if (zero? index)
                     history-content
                     [tooltip/block-element :right tooltip history-content])]]])]]))])]))

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

(defn- sort-options
  "Displays the different sort options for card elements."
  []
  (let [sort-method @(rf/subscribe [:discussion.statements/sort-method])]
    [:section.h-100
     [:button.btn.btn-outline-primary.mr-2.h-100
      {:class (when (= sort-method :newest) "active")
       :on-click #(rf/dispatch [:discussion.statements.sort/set :newest])}
      (labels :badges.sort/newest)]
     [:button.btn.btn-outline-primary.h-100
      {:class (when (= sort-method :popular) "active")
       :on-click #(rf/dispatch [:discussion.statements.sort/set :popular])}
      (labels :badges.sort/popular)]]))

(defn- discussion-privacy-badge
  "A small badge displaying who can see the discussion!"
  [{:keys [discussion/states]}]
  (let [public? (contains? (set states) :discussion.state/public)]
    [:<>
     (if public?
       [:span.badge
        [:i.primary-light-color {:class (str "m-auto fas fa-lg " (fa :lock-open))}] " "
        (labels :discussion.privacy/public)]
       [:span.badge
        [:i.secondary-color {:class (str "m-auto fas fa-lg " (fa :lock-closed))}] " "
        (labels :discussion.privacy/private)])]))

(defn- title-and-input-element
  "Element containing Title and textarea input"
  [statement input is-topic? badges info-content]
  (let [title [md/as-markdown (:statement/content statement)]
        edit-active? @(rf/subscribe [:statement.edit/ongoing? (:db/id statement)])
        read-only? @(rf/subscribe [:schnaq.selected/read-only?])]
    [:<>
     (if is-topic?
       [:h2.h6-md-down title]
       (if edit-active?
         [edit/edit-card statement]
         [:h2.h6 title]))
     [:div.d-flex.flex-row.my-4
      [:div.mr-auto info-content]
      [:div.ml-auto badges]]
     [:div.line-divider.my-4]
     (if read-only?
       [:div.alert.alert-warning (labels :discussion.state/read-only-warning)]
       input)]))

(defn- topic-bubble-desktop [discussion statement input badges info-content is-topic?]
  [:div.p-2
   [:div.d-flex.mb-4
    [user/user-info (:statement/author statement) 42 (:statement/created-at statement)]
    [:div.ml-auto.my-auto
     [discussion-privacy-badge discussion]]]
   [title-and-input-element statement input is-topic? badges info-content]])

(defn- topic-bubble-mobile [{:discussion/keys [share-hash] :as discussion} statement input badges info-content]
  [:<>
   [:div.d-flex.mb-4
    ;; graph and badges
    [:div.mr-auto
     [discussion-navbar/graph-button share-hash]
     [discussion-navbar/summary-button share-hash]
     [:div.mt-2 badges]]
    ;; settings
    [:div.p-0
     [discussion-privacy-badge discussion]
     [:div.d-flex
      [:div.ml-auto.mr-2 [user/user-info (:statement/author statement) 32 (:statement/created-at statement)]]
      info-content]]]
   ;; title
   [title-and-input-element statement input]
   [:div.ml-3
    (labels :badges.sort/sort)
    [sort-options]]])

(defn- topic-bubble [content]
  (let [title (:discussion/title @(rf/subscribe [:schnaq/selected]))]
    (common/set-website-title! title)
    [:div.panel-white.mb-4
     [:div.discussion-light-background content]]))

(rf/reg-event-db
  :discussion.statements.sort/set
  (fn [db [_ method]]
    (assoc-in db [:discussion :statements :sort-method] method)))

(rf/reg-sub
  :discussion.statements/sort-method
  (fn [db _]
    (get-in db [:discussion :statements :sort-method] :newest)))

(defn- topic-view [{:keys [discussion/share-hash]} conclusions topic-content]
  [:<>
   [topic-bubble topic-content]
   (when conclusions
     [cards/conclusion-cards-list conclusions share-hash])])

(defn- show-how-to [is-topic?]
  (if is-topic?
    [how-to-elements/quick-how-to-schnaq]
    [how-to-elements/quick-how-to-pro-con]))

(defn search-bar
  "A search-bar to search inside a schnaq."
  []
  [:form.mx-3.h-100
   {:on-submit (fn [e]
                 (jq/prevent-default e)
                 (rf/dispatch [:schnaq/search (oget e [:target :elements "search-input" :value])]))}
   [:div.input-group.search-bar.h-100
    [:input.form-control.my-auto.search-bar-input.h-100
     {:type "text" :aria-label "Search" :placeholder
      (labels :schnaq.search/input) :name "search-input"}]
    [:div.input-group-append
     [:button.btn.button-muted.h-100
      {:type "submit"}
      [:i {:class (str "m-auto fas " (fa :search))}]]]]])

(defn action-view [has-history?]
  [:div.d-inline-block.text-dark.w-100.mb-3
   [:div.d-flex.flex-row
    [:div.mr-1
     [back-button has-history?]]
    [:div.mx-1
     [search-bar]]
    [:div.mx-1
     [sort-options]]
    [:div.d-flex.flex-row.ml-auto]]])

(defn discussion-view-mobile
  "Discussion view for mobile devices
  No history but fullscreen topic bubble and conclusions"
  [current-discussion statement input badges info-content conclusions history]
  (let [is-topic? (nil? history)]
    [:<>
     [topic-view current-discussion conclusions
      [topic-bubble-mobile current-discussion statement input badges info-content]]
     [show-how-to is-topic?]]))

(defn discussion-view-desktop
  "Discussion View for desktop devices.
  Displays a history on the left and a topic with conclusion in its center"
  [{:keys [discussion/share-hash] :as current-discussion} statement input badges info-content conclusions history]
  (let [is-topic? (nil? history)
        has-history? (seq history)]
    [:div.container-fluid
     [:div.row
      [:div.col-6.col-lg-5.py-4
       [topic-view current-discussion nil
        [topic-bubble-desktop current-discussion statement input badges info-content is-topic?]]
       [history-view history]]
      [:div.col-6.col-lg-7.py-4
       [action-view has-history?]
       [cards/conclusion-cards-list conclusions share-hash]
       [input/input-celebration-first]
       [:div.w-75.mx-auto [show-how-to is-topic?]]]]]))

(defn info-content-conclusion
  "Badges and up/down-votes to be displayed in the topic bubble."
  [statement]
  [cards/up-down-vote statement])
