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
            [schnaq.interface.views.user :as user]
            [schnaq.user :as user-utils]))

(defn info-content-conclusion
  "Badges and up/down-votes to be displayed in the topic bubble."
  [statement]
  [cards/up-down-vote statement])

(defn- back-button
  "Return to your schnaqs Button"
  []
  (let [history @(rf/subscribe [:discussion-history])
        has-history? (seq history)
        back-feed [:navigation/navigate :routes.schnaqs/personal]
        current-route @(rf/subscribe [:navigation/current-route-name])
        is-search-view? (= current-route :routes.search/schnaq)
        steps-back (if is-search-view? 0 1)
        back-history [:discussion.history/time-travel steps-back]
        navigation (if (or is-search-view? has-history?) back-history back-feed)
        tooltip (if has-history? :history.back/tooltip :history.all-schnaqs/tooltip)]
    [:button.btn.btn-dark.w-100.h-100.p-3
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
  []
  (let [history @(rf/subscribe [:discussion-history])
        indexed-history (map-indexed #(vector (- (count history) %1 1) %2) history)
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
                                  [:h6 (labels :history.statement/user) (toolbelt/truncate-to-n-chars nickname 20)]
                                  [:div.ml-auto [common/avatar user 22]]]
                                 (toolbelt/truncate-to-n-words statement-content max-word-count)]]
            [:article {:key (str "history-container-" (:db/id statement))}
             [:div.history-thread-line {:key (str "history-divider-" (:db/id statement))}]
             [:div.d-inline-block.d-md-block.text-dark.w-100
              {:key (str "history-" (:db/id statement))}
              (let [attitude (name (or (:statement/type statement) :neutral))]
                [:div.card-history.clickable.w-100
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
    (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
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
    (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
      {:fx [(http/xhrio-request db :get "/discussion/conclusions/starting"
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
  "A small badge displaying who can see the discussion."
  []
  (let [{:discussion/keys [states share-hash]} @(rf/subscribe [:schnaq/selected])
        public? (contains? (set states) :discussion.state/public)
        [label class] (if public? [:discussion.privacy/public (str "primary-light-color m-auto fas fa-lg " (fa :lock-open))]
                                  [:discussion.privacy/private (str "secondary-color m-auto fas fa-lg " (fa :lock-closed))])]
    [:span.badge.my-auto.ml-auto {:key (str "discussion-privacy-badge-" share-hash)}
     [:i {:class class}] " " (labels label)]))

(defn- current-topic-badges [schnaq statement is-topic?]
  (let [badges-start [badges/static-info-badges schnaq]
        badges-conclusion [badges/extra-discussion-info-badges statement (:discussion/edit-hash schnaq)]
        badges (if is-topic? badges-start badges-conclusion)]
    [:div.ml-auto badges]))

(defn- title-view [statement is-topic?]
  (let [title [md/as-markdown (:statement/content statement)]
        edit-active? @(rf/subscribe [:statement.edit/ongoing? (:db/id statement)])]
    (if is-topic?
      [:h2.h6-md-down title]
      (if edit-active?
        [edit/edit-card statement]
        [:h2.h6.font-weight-bold title]))))

(defn- title-and-input-element
  "Element containing Title and textarea input"
  [statement]
  (let [schnaq @(rf/subscribe [:schnaq/selected])
        is-topic? (= :routes.schnaq/start @(rf/subscribe [:navigation/current-route-name]))
        read-only? @(rf/subscribe [:schnaq.selected/read-only?])
        info-content [info-content-conclusion statement (:discussion/edit-hash statement)]
        input-style (if is-topic? "statement-text" "premise-text")]
    [:<>
     [title-view statement is-topic?]
     [:div.d-flex.flex-row.my-4
      (when-not is-topic?
        [:div.mr-auto info-content])
      [current-topic-badges schnaq statement is-topic?]]
     [:div.line-divider.my-4]
     (if read-only?
       [:div.alert.alert-warning (labels :discussion.state/read-only-warning)]
       [input/input-form input-style])]))

(defn- topic-bubble-view []
  (let [{:discussion/keys [title author created-at]} @(rf/subscribe [:schnaq/selected])
        history @(rf/subscribe [:discussion-history])
        current-conclusion (last history)
        content {:statement/content title :statement/author author :statement/created-at created-at}
        is-topic? (= :routes.schnaq/start @(rf/subscribe [:navigation/current-route-name]))
        statement (if is-topic? content current-conclusion)]
    [:div.p-2
     [:div.d-flex.flex-wrap.mb-4
      [user/user-info (:statement/author statement) 42 (:statement/created-at statement) nil]
      [discussion-privacy-badge]]
     [title-and-input-element statement]]))

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

(defn- show-how-to []
  (let [is-topic? (= :routes.schnaq/start @(rf/subscribe [:navigation/current-route-name]))]
    (if is-topic?
      [how-to-elements/quick-how-to-schnaq]
      [how-to-elements/quick-how-to-pro-con])))

(defn search-bar
  "A search-bar to search inside a schnaq."
  []
  [:form.mx-3.h-100
   {:on-submit (fn [e]
                 (jq/prevent-default e)
                 (rf/dispatch [:discussion.statements/search (oget e [:target :elements "search-input" :value])]))}
   [:div.input-group.search-bar.h-100
    [:input.form-control.my-auto.search-bar-input.h-100
     {:type "text" :aria-label "Search" :placeholder
      (labels :schnaq.search/input) :name "search-input"}]
    [:div.input-group-append
     [:button.btn.button-muted.h-100
      {:type "submit"}
      [:i {:class (str "m-auto fas " (fa :search))}]]]]])

(defn action-view []
  [:div.d-inline-block.text-dark.w-100.mb-3
   [:div.d-flex.flex-row.flex-wrap
    [:div.mr-1.my-1
     [back-button]]
    [:div.m-1
     [search-bar]]
    [:div.m-1
     [sort-options]]
    [:div.d-flex.flex-row.ml-auto]]])

(defn discussion-view
  "Discussion View for desktop devices.
  Displays a history on the left and a topic with conclusion in its center"
  [{:keys [discussion/share-hash] :as current-discussion} conclusions]
  [:div.container-fluid
   [:div.row
    [:div.col-md-6.col-lg-4.py-4.px-0.px-md-3
     [topic-view current-discussion nil
      [topic-bubble-view]]
     [:div.d-none.d-md-block [history-view]]]
    [:div.col-md-6.col-lg-8.py-4.px-0.px-md-3
     [action-view]
     [cards/conclusion-cards-list conclusions share-hash]
     [:div.d-md-none [history-view]]
     [input/input-celebration-first]
     [:div.w-75.mx-auto [show-how-to]]]]])
