(ns schnaq.interface.views.discussion.card-elements
  (:require [clojure.string :as cstring]
            [goog.functions :as gfun]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.components.schnaq :as sc]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.utils.markdown :as md]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.conclusion-card :as cards]
            [schnaq.interface.views.discussion.edit :as edit]
            [schnaq.interface.views.discussion.filters :as filters]
            [schnaq.interface.views.discussion.input :as input]
            [schnaq.interface.views.discussion.labels :as labels]
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
        back-history [:discussion.history/time-travel 1]
        navigation-target (cond
                            has-history? back-history
                            shared-config/embedded? nil
                            :else back-feed)
        tooltip (if has-history? :history.back/tooltip :history.all-schnaqs/tooltip)]
    (when navigation-target
      [tooltip/text
       (labels tooltip)
       [:button.btn.btn-dark.button-discussion-options.h-100.w-100.p-3
        {:on-click #(rf/dispatch navigation-target)}
        [:div.d-flex
         [icon :arrow-left "m-auto"]]]])))


(defn- discussion-start-button
  "Discussion start button for history view"
  []
  (let [schnaq @(rf/subscribe [:schnaq/selected])
        title (:discussion/title schnaq)]
    [:a.text-decoration-none
     {:href (rfe/href :routes.schnaq/start {:share-hash (:discussion/share-hash schnaq)})}
     [:div.clickable.card-history-home.text-dark
      [tooltip/text
       (labels :history.home/tooltip)
       [:div.text-center
        [:h6 title]
        [:p.text-muted.mb-0 (labels :history.home/text)]
        [badges/static-info-badges schnaq]]
       {:placement :right}]]]))

(defn history-view
  "History view displayed in the left column in the desktop view."
  []
  (let [history @(rf/subscribe [:discussion-history])
        indexed-history (map-indexed #(vector (- (count history) %1 1) %2) history)
        has-history? (seq indexed-history)]
    (when has-history?
      [:section.history-wrapper
       [:h5.p-2.text-center (labels :history/title)]
       [discussion-start-button]
       ;; history
       (for [[index statement] indexed-history]
         (let [max-word-count 20
               nickname (user-utils/statement-author statement)
               user (:statement/author statement)
               statement-content (-> statement :statement/content)
               tooltip-text (gstring/format "%s %s" (labels :tooltip/history-statement) nickname)
               history-content [:div
                                [:div.d-flex.flex-row
                                 [:h6 (labels :history.statement/user) " " (toolbelt/truncate-to-n-chars nickname 20)]
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
                    [tooltip/text tooltip-text history-content {:placement :right}])]]])]]))])))

(rf/reg-event-fx
  :discussion.add.statement/starting
  (fn [{:keys [db]} [_ form]]
    (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
          statement-text (oget form [:statement-text :value])]
      {:db (update-in db [:schnaq :selected :meta-info :all-statements] inc)
       :fx [(http/xhrio-request db :post "/discussion/statements/starting/add"
                                [:discussion.add.statement/starting-success form]
                                {:statement statement-text
                                 :share-hash share-hash
                                 :display-name (toolbelt/current-display-name db)}
                                [:ajax.error/as-notification])]})))

(rf/reg-event-fx
  :discussion.add.statement/starting-success
  (fn [{:keys [db]} [_ form new-starting-statements]]
    (let [starting-conclusions (:starting-conclusions new-starting-statements)
          statement-with-creation-secret (first (filter :statement/creation-secret
                                                        starting-conclusions))]
      {:fx [[:dispatch [:notification/add
                        #:notification{:title (labels :discussion.notification/new-content-title)
                                       :body (labels :discussion.notification/new-content-body)
                                       :context :success}]]
            [:dispatch [:discussion.query.conclusions/set-starting new-starting-statements]]
            (when (and (= 1 (count starting-conclusions))
                       (not shared-config/embedded?)
                       (not= :discussion.mode/qanda (get-in db [:schnaq :selected :discussion/mode])))
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
                                {:share-hash share-hash
                                 :display-name (toolbelt/current-display-name db)})]})))

(rf/reg-event-fx
  :discussion.query.conclusions/set-starting
  (fn [{:keys [db]} [_ {:keys [starting-conclusions]}]]
    (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
          visited (map :db/id starting-conclusions)]
      {:db (-> db
               (assoc-in [:discussion :premises :current] starting-conclusions)
               (update-in [:visited :statement-ids share-hash] #(set (concat %1 %2)) visited))
       ;; hier die seen setzen
       :fx [[:dispatch [:votes.local/reset]]]})))

(rf/reg-event-db
  :votes.local/reset
  (fn [db _]
    (update db :votes
            dissoc :up
            dissoc :down)))


;; -----------------------------------------------------------------------------

(defn- sort-options
  "Displays the different sort options for card elements."
  []
  (let [sort-method @(rf/subscribe [:discussion.statements/sort-method])
        button-title (case sort-method
                       :newest (labels :badges.sort/newest)
                       :popular (labels :badges.sort/popular)
                       (labels :badges/sort))
        dropdown-menu-id "dropdownSortButton"]
    [:<>
     [:small.text-muted (labels :badges/sort)]
     [sc/discussion-options-dropdown
      button-title
      dropdown-menu-id
      [{:on-click #(rf/dispatch [:discussion.statements.sort/set :newest])
        :label-key :badges.sort/newest}
       {:on-click #(rf/dispatch [:discussion.statements.sort/set :popular])
        :label-key :badges.sort/popular}]]]))


;; -----------------------------------------------------------------------------

(defn- current-topic-badges [schnaq statement is-topic?]
  (let [badges-start [badges/static-info-badges-discussion schnaq]
        badges-conclusion [badges/extra-discussion-info-badges statement]
        badges (if is-topic? badges-start badges-conclusion)]
    [:div.ml-auto badges]))

(defn- title-view [statement is-topic?]
  (let [title [md/as-markdown (:statement/content statement)]
        edit-active? @(rf/subscribe [:statement.edit/ongoing? (:db/id statement)])]
    (if is-topic?
      (if edit-active?
        [edit/edit-card-discussion statement]
        [:h2.h6-md-down title])
      (if edit-active?
        [edit/edit-card-statement statement]
        [:h2.h6 title]))))

(defn- title-and-input-element
  "Element containing Title and textarea input"
  [statement]
  (let [schnaq @(rf/subscribe [:schnaq/selected])
        is-topic? (= :routes.schnaq/start @(rf/subscribe [:navigation/current-route-name]))
        read-only? @(rf/subscribe [:schnaq.selected/read-only?])
        info-content [info-content-conclusion statement (:discussion/edit-hash statement)]
        input-style (if is-topic? "statement-text" "premise-text")
        statement-labels (set (:statement/labels statement))]
    [:<>
     [title-view statement is-topic?]
     [:div.d-flex.flex-row.mt-4.mb-2
      (when-not is-topic?
        [:div.mr-auto info-content])
      [current-topic-badges schnaq statement is-topic?]]
     (for [label statement-labels]
       [:span.pr-1 {:key (str "show-label-" (:db/id statement) label)}
        [labels/build-label label]])
     [:div.line-divider.my-2.my-md-4]
     (if read-only?
       [:div.alert.alert-warning (labels :discussion.state/read-only-warning)]
       [input/input-form input-style])]))

(defn- topic-bubble-view []
  (let [{:discussion/keys [title author created-at] :as schnaq} @(rf/subscribe [:schnaq/selected])
        current-conclusion @(rf/subscribe [:discussion.conclusion/selected])
        content {:db/id (:db/id schnaq)
                 :statement/content title
                 :statement/author author
                 :statement/created-at created-at}
        is-topic? (= :routes.schnaq/start @(rf/subscribe [:navigation/current-route-name]))
        statement (if is-topic? content current-conclusion)]
    [motion/move-in :left
     [:div.p-2
      [:div.d-flex.flex-wrap.mb-4
       [user/user-info statement 42 nil]]
      [title-and-input-element statement]]]))

(defn- topic-view [content]
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

(defn- show-how-to []
  (let [is-topic? (= :routes.schnaq/start @(rf/subscribe [:navigation/current-route-name]))]
    (if is-topic?
      [how-to-elements/quick-how-to-schnaq]
      [how-to-elements/quick-how-to-pro-con])))

(def throttled-in-schnaq-search
  (gfun/throttle
    #(rf/dispatch [:discussion.statements/search (oget % [:target :value])])
    500))

(defn- search-clear-button
  [clear-id]
  (let [search-string @(rf/subscribe [:schnaq.search.current/search-string])
        action-icon (if (cstring/blank? search-string) :search :times)]
    [:div.input-group-append
     [:button.btn.button-muted.h-100
      {:on-click (fn [_e]
                   (jq/clear-input clear-id)
                   (rf/dispatch [:schnaq.search.current/clear-search-string]))}
      [icon action-icon "m-auto"]]]))

(defn search-bar
  "A search-bar to search inside a schnaq."
  []
  (let [search-input-id "search-bar"
        route-name @(rf/subscribe [:navigation/current-route-name])
        selected-statement-id (get-in @(rf/subscribe [:navigation/current-route]) [:path-params :statement-id])]
    [:form.h-100
     {:on-submit #(jq/prevent-default %)
      :key (str route-name selected-statement-id)}
     [:div.input-group.search-bar.h-100.panel-white.p-0
      [:input.form-control.my-auto.search-bar-input.h-100
       {:id search-input-id
        :type "text"
        :aria-label "Search"
        :placeholder (labels :schnaq.search/input)
        :name "search-input"
        :on-key-up #(throttled-in-schnaq-search %)}]
      [search-clear-button search-input-id]]]))

(defn action-view []
  [:div.d-inline-block.text-dark.w-100.mb-3
   [:div.d-flex.flex-row.flex-wrap
    [:div.d-flex.flex-grow-1.flex-lg-grow-0
     [:div.mr-1.ml-1.ml-md-0
      [back-button]]
     [:div.ml-1.mr-1.mr-lg-3.flex-grow-1.flex-lg-grow-0
      [search-bar]]]
    [:div.d-flex
     [:div.mx-1.pr-2
      [sort-options]]
     [:div.mx-1.mt-auto
      (if @(rf/subscribe [:schnaq.mode/qanda?])
        (when (= :routes.schnaq/start @(rf/subscribe [:navigation/current-route-name]))
          [filters/filter-answered-statements])
        [filters/filter-button])]]
    [:div.d-flex.flex-row.ml-auto]]])

(defn- search-info []
  (let [search-string @(rf/subscribe [:schnaq.search.current/search-string])
        search-results @(rf/subscribe [:schnaq.search.current/result])]
    [motion/move-in :left
     [:div.my-4
      [:div.d-inline-block
       [:h2 (labels :schnaq.search/heading)]
       [:div.row.mx-0.mt-4.mb-3
        [:img.dashboard-info-icon-sm {:src (img-path :icon-search)}]
        [:div.text.display-6.my-auto.mx-3
         search-string]]]
      [:div.row.m-0
       [:img.dashboard-info-icon-sm {:src (img-path :icon-posts)}]
       (if (empty? search-results)
         [:p.mx-3 (labels :schnaq.search/new-search-title)]
         [:p.mx-3 (str (count search-results) " " (labels :schnaq.search/results))])]]]))

(defn discussion-view
  "Displays a history  and input field on the left and conclusions in its center"
  [share-hash]
  (let [search-inactive? (cstring/blank? @(rf/subscribe [:schnaq.search.current/search-string]))]
    [:div.container-fluid
     [:div.row
      [:div.col-md-6.col-lg-4.py-md-4.px-0.px-md-3
       [topic-view
        (if search-inactive?
          [topic-bubble-view]
          [search-info])]
       [:div.d-none.d-md-block [history-view]]]
      [:div.col-md-6.col-lg-8.py-md-4.px-0.px-md-3
       [action-view]
       [cards/conclusion-cards-list share-hash]
       [:div.d-md-none [history-view]]
       [:div.mx-auto
        {:class (when-not shared-config/embedded? "col-11 col-md-12 col-lg-12 col-xl-10")}
        [show-how-to]]]]]))

(rf/reg-sub
  :schnaq.search.current/search-string
  (fn [db _]
    (get-in db [:search :schnaq :current :search-string] "")))

(rf/reg-event-db
  :schnaq.search.current/clear-search-string
  (fn [db _]
    (assoc-in db [:search :schnaq :current :search-string] "")))

(rf/reg-sub
  :schnaq.search.current/result
  (fn [db _]
    (get-in db [:search :schnaq :current :result] [])))
