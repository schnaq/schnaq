(ns schnaq.interface.views.discussion.conclusion-card
  (:require ["react-smart-masonry" :default Masonry]
            [clojure.string :as cstring]
            [com.fulcrologic.guardrails.core :refer [>defn-]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reitit.frontend.easy :as reitfe]
            [schnaq.database.specs :as specs]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.components.schnaq :as sc]
            [schnaq.interface.config :as config]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.markdown :as md]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.edit :as edit]
            [schnaq.interface.views.discussion.filters :as filters]
            [schnaq.interface.views.discussion.input :as input]
            [schnaq.interface.views.discussion.labels :as labels]
            [schnaq.interface.views.discussion.logic :as logic]
            [schnaq.interface.views.schnaq.activation :as activation]
            [schnaq.interface.views.schnaq.survey :as survey]
            [schnaq.interface.views.user :as user]))

(def ^:private card-fade-in-time
  "Set a common setting for fading in the cards, e.g. statement cards."
  0.1)

(defn- call-to-action-schnaq
  "If no contributions are available, add a call to action to engage the users."
  [body]
  [motion/fade-in-and-out
   [:article.call-to-contribute.m-3
    [:div.alert.alert-light.text-light.row.layered-wave-background.p-md-5.rounded-1
     [:div.col-2.py-md-5.d-flex
      [:img.w-75.align-self-center {:src (img-path :schnaqqifant/three-d-head)}]]
     [:div.col-10.py-md-5
      body]]]])

(defn- call-to-share
  "Present access code and link to schnaq.app."
  []
  [call-to-action-schnaq
   [:<>
    [:p.h5 (labels :qanda.call-to-action/display-code)]
    [:p.h1.py-3 [sc/access-code]]
    [:p.h5 (labels :qanda.call-to-action/intro-1) " "
     [:span.text-monospace.mx-2 {:href "https://schnaq.app"
                                 :target :_blank}
      "https://schnaq.app"]
     " "
     (labels :qanda.call-to-action/intro-2)]
    [:p.pt-3 [icon :info "m-auto fas"] " "
     (labels :qanda.call-to-action/help) " " [icon :share "m-auto fas"]]]])

;; -----------------------------------------------------------------------------

(defn up-down-vote
  "Add inline panel for up and down votes."
  [statement]
  (let [votes @(rf/subscribe [:local-votes])
        [local-upvote? local-downvote?] @(rf/subscribe [:votes/upvoted-or-downvoted (:db/id statement)])
        ;; Do not use or shortcut, since the value can be false and should be preferably selected over backend value
        upvoted? (if (nil? local-upvote?) (:meta/upvoted? statement) local-upvote?)
        downvoted? (if (nil? local-downvote?) (:meta/downvoted? statement) local-downvote?)]
    [:div.d-flex.flex-row.align-items-center
     [:div.mr-2
      {:class (if upvoted? "badge badge-upvote-selected" "badge badge-upvote")
       :on-click (fn [e]
                   (js-wrap/stop-propagation e)
                   (rf/dispatch [:discussion/toggle-upvote statement]))}
      [icon :arrow-up "vote-arrow m-auto"]]
     [:span.mr-3 (logic/get-up-votes statement votes)]
     [:div.mr-2
      {:class (if downvoted? "badge badge-downvote-selected" "badge badge-downvote")
       :on-click (fn [e]
                   (js-wrap/stop-propagation e)
                   (rf/dispatch [:discussion/toggle-downvote statement]))}
      [icon :arrow-down "vote-arrow m-auto"]]
     [:span (logic/get-down-votes statement votes)]]))

(defn- statement-information-row [statement]
  [:div.d-flex.flex-wrap.align-items-center
   [up-down-vote statement]
   [:div.ml-sm-0.ml-lg-auto
    [badges/extra-discussion-info-badges statement]]])

(defn- mark-as-answer-button
  "Show a button to mark a statement as an answer."
  [statement]
  (let [statement-labels (set (:statement/labels statement))
        label ":check"
        checked? (statement-labels label)]
    [:section.w-100
     [:button.btn.btn-sm.btn-link.text-dark.pr-0
      {:on-click #(if checked?
                    (rf/dispatch [:statement.labels/remove statement label])
                    (rf/dispatch [:statement.labels/add statement label]))}
      [:small.pr-2 (if checked?
                     (labels :qanda.button.mark/as-unanswered)
                     (labels :qanda.button.mark/as-answer))]
      [labels/build-label (if checked? label ":unchecked")]]]))

(>defn- card-highlighting
  "Add card-highlighting to a statement card."
  [{:keys [statement/type]}]
  [::specs/statement :ret string?]
  (let [statement-type (when type (str "-" (name type)))]
    (str "highlight-card" statement-type)))

(defn- statement-card->editable-card
  "Wrap `statement-card-component`. Check if this statement is currently being
  edited, show edit-card if true."
  [statement statement-card-component]
  (let [currently-edited? @(rf/subscribe [:statement.edit/ongoing? (:db/id statement)])]
    (if currently-edited?
      [edit/edit-card-statement statement]
      statement-card-component)))

(defn statement-card
  "Display a full interactive statement. Takes `additional-content`, e.g. the
  answer of a question."
  ([statement]
   [statement-card statement nil])
  ([statement additional-content]
   (let [current-route @(rf/subscribe [:navigation/current-route-name])
         history-length (count @(rf/subscribe [:discussion-history]))
         mods-mark-only? @(rf/subscribe [:schnaq.selected.qa/mods-mark-only?])
         authenticated? @(rf/subscribe [:user/authenticated?])
         show-answer? (and (= 1 history-length)
                           (= :routes.schnaq.select/statement current-route) ;; history-length == 1 => a reply to a question
                           (or (not mods-mark-only?)
                               (and mods-mark-only? authenticated? @(rf/subscribe [:schnaq/edit-hash]))))]
     [:article.statement-card
      [:div.d-flex.flex-row
       [:div {:class (card-highlighting statement)}]
       [:div.card-view.card-body.py-2.px-0
        (when (:meta/new? statement)
          [:div.bg-primary.p-2.rounded-1.d-inline-block.text-white.small.float-right
           (labels :discussion.badges/new)])
        [:div.pt-2.d-flex.px-3
         [:div.mr-auto [user/user-info statement 32 "w-100"]]
         [:div.d-flex.flex-row.align-items-center.ml-auto
          (when show-answer? [mark-as-answer-button statement])
          [badges/edit-statement-dropdown-menu statement]]]
        [:div.my-4]
        [:div.text-typography.px-3
         [md/as-markdown (:statement/content statement)]
         [statement-information-row statement]]
        [:div.ml-1.mr-3
         [input/reply-in-statement-input-form statement]
         additional-content]]]])))

(defn reduced-statement-card
  "A reduced statement-card focusing on the statement."
  [statement with-answer?]
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [motion/fade-in-and-out
     [:article.statement-card.mt-1.border
      [:div.d-flex.flex-row
       [:div.mr-2 {:class (str "highlight-card-reduced highlight-card-" (name (or (:statement/type statement) :neutral)))}]
       [:div.card-view.card-body.p-2
        [:div.d-flex.justify-content-start.pt-2
         [user/user-info statement 25 "w-100"]
         [badges/edit-statement-dropdown-menu statement]]
        [:div.my-3]
        [:div.text-typography
         [md/as-markdown (:statement/content statement)]]
        [:div.d-flex.flex-wrap.align-items-center
         [:a.btn.btn-sm.btn-outline-dark.mr-3.px-1.py-0
          {:href (reitfe/href :routes.schnaq.select/statement {:share-hash share-hash
                                                               :statement-id (:db/id statement)})}
          [icon :comments "my-auto mr-1"]
          [:small (labels :statement/discuss)]]
         [up-down-vote statement]
         (when with-answer?
           [:div.d-flex.flex-row.align-items-center.ml-auto
            [mark-as-answer-button statement]])]]]]]))

(defn reduced-or-edit-card
  "Wrap reduced statement card to make it editable."
  [statement args]
  [statement-card->editable-card statement [reduced-statement-card statement args]])

(defn- answers [statement]
  (let [answers (filter #(some #{":check"} (:statement/labels %)) (:statement/children statement))]
    (when (seq answers)
      [:div.mt-2
       (for [answer answers]
         (with-meta
           [reduced-or-edit-card answer :with-answer]
           {:key (str "answer-" (:db/id answer))}))])))

(defn- replies [statement]
  (let [statement-id (:db/id statement)
        collapsed? @(rf/subscribe [:toggle-replies/is-collapsed? statement-id])
        button-content (if collapsed?
                         [:<> [icon :collapse-up "my-auto mr-2"]
                          (labels :qanda.button.show/replies)]
                         [:<> [icon :collapse-down "my-auto mr-2"]
                          (labels :qanda.button.hide/replies)])
        collapsible-id (str "collapse-Replies-" statement-id)
        replies (filter #(not-any? #{":check"} (:statement/labels %)) (:statement/children statement))
        starting-route? @(rf/subscribe [:schnaq.routes/starting?])
        mods-mark-only? @(rf/subscribe [:schnaq.selected.qa/mods-mark-only?])
        authenticated? @(rf/subscribe [:user/authenticated?])
        with-answer? (and starting-route?
                          (or (not mods-mark-only?)
                              (and mods-mark-only? authenticated? @(rf/subscribe [:schnaq/edit-hash]))))]
    (when (not-empty replies)
      [:<>
       [:button.btn.btn-transparent.border-0
        {:type "button" :data-toggle "collapse" :aria-expanded "false"
         :data-target (str "#" collapsible-id)
         :on-click #(rf/dispatch [:toggle-replies/is-collapsed! statement-id (not collapsed?)])
         :aria-controls collapsible-id}
        button-content]
       [:div.collapse {:id collapsible-id}
        (for [reply replies]
          (with-meta
            [reduced-or-edit-card reply with-answer?]
            {:key (str "reply-" (:db/id reply))}))]])))

(rf/reg-event-db
 :toggle-replies/is-collapsed!
 (fn [db [_ statement-id collapsed?]]
   (assoc-in db [:statements :toggled-replies statement-id] collapsed?)))

(rf/reg-event-db
 :toggle-replies/clear!
 (fn [db _]
   (assoc-in db [:statements :toggled-replies] {})))

(rf/reg-sub
 :toggle-replies/is-collapsed?
 (fn [db [_ statement-id]]
   (get-in db [:statements :toggled-replies statement-id] true)))

(defn answer-card
  "Display the answer directly inside the statement itself."
  [statement]
  [statement-card statement
   [:<>
    [answers statement]
    [replies statement]]])

(defn- answer-or-edit-card
  "Either show the clickable statement, or its edit-view."
  [statement]
  [statement-card->editable-card statement [answer-card statement]])

(defn- sort-statements
  "Sort statements according to the filter method. If we are in q-and-a-mode,
  then always display own statements first."
  [user statements sort-method local-votes]
  (let [username (get-in user [:names :display])
        selection-function (if (:authenticated? user)
                             #(= (:id user) (get-in % [:statement/author :db/id]))
                             #(= username (get-in % [:statement/author :user/nickname])))
        keyfn (case sort-method
                :newest :statement/created-at
                :popular #(logic/calculate-votes % local-votes))
        own-statements (sort-by keyfn > (filter selection-function statements))
        other-statements (sort-by keyfn > (remove selection-function statements))]
    (if (= "Anonymous" username)
      (sort-by keyfn > statements)
      (concat own-statements other-statements))))

(defn- input-form-or-disabled-alert
  "Dispatch to show input form or an alert that it is currently not allowed to 
  add statements."
  []
  (if @(rf/subscribe [:schnaq.selected/read-only?])
    [:div.alert.alert-warning (labels :discussion.state/read-only-warning)]
    [input/input-form]))

;; -----------------------------------------------------------------------------


(defn- current-topic-badges [schnaq statement]
  (let [badges-start [badges/static-info-badges-discussion schnaq]
        badges-conclusion [badges/extra-discussion-info-badges statement true]
        starting-route? @(rf/subscribe [:schnaq.routes/starting?])
        badges (if starting-route? badges-start badges-conclusion)]
    [:div.ml-auto badges]))

(defn- title-view [statement]
  (let [starting-route? @(rf/subscribe [:schnaq.routes/starting?])
        title [md/as-markdown (:statement/content statement)]
        edit-active? @(rf/subscribe [:statement.edit/ongoing? (:db/id statement)])]
    (if edit-active?
      (if starting-route?
        [edit/edit-card-discussion statement]
        [edit/edit-card-statement statement])
      [:h2.h6 title])))

(defn- title-and-input-element
  "Element containing Title and textarea input"
  [statement]
  (let [statement-labels (set (:statement/labels statement))]
    [:<>
     [title-view statement]
     (for [label statement-labels]
       [:span.pr-1 {:key (str "show-label-" (:db/id statement) label)}
        [labels/build-label label]])]))

(defn- topic-bubble-view []
  (let [{:discussion/keys [title author created-at] :as schnaq} @(rf/subscribe [:schnaq/selected])
        current-conclusion @(rf/subscribe [:discussion.conclusion/selected])
        content {:db/id (:db/id schnaq)
                 :statement/content title
                 :statement/author author
                 :statement/created-at created-at}
        starting-route? @(rf/subscribe [:schnaq.routes/starting?])
        statement (if starting-route? content current-conclusion)
        info-content [up-down-vote statement]]
    [motion/move-in :left
     [:div.p-2
      [:div.d-flex.flex-wrap.mb-4
       [user/user-info statement 32 nil]
       [:div.d-flex.flex-row.ml-auto
        (when-not starting-route?
          [:div.mr-auto info-content])
        [current-topic-badges schnaq statement]]]
      [title-and-input-element statement]]]))

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

(defn- topic-or-search-content []
  (let [search-inactive? (cstring/blank? @(rf/subscribe [:schnaq.search.current/search-string]))]
    [:div.overflow-hidden.mb-4
     (if search-inactive?
       [topic-bubble-view]
       [search-info])]))

(defn selection-card
  "Dispatch the different input options, e.g. questions, survey or activation.
  The survey and activation feature are not available for free plan users."
  []
  (let [selected-option (reagent/atom :question)
        on-click #(reset! selected-option %)
        active-class #(when (= @selected-option %) "active")
        iconed-heading (fn [icon-key label]
                         [:<> [icon icon-key] " " (labels label)])]
    (fn []
      (let [survey-tab [:span [iconed-heading :chart-pie :schnaq.input-type/survey]]
            activation-tab [:span [iconed-heading :magic :schnaq.input-type/activation]]
            beta-user? @(rf/subscribe [:user/beta-tester?])
            admin? @(rf/subscribe [:schnaq.current/admin-access])
            disabled-tooltip-key (cond
                                   (not beta-user?) :schnaq.input-type/beta-only
                                   (not admin?) :schnaq.input-type/not-admin
                                   :else :schnaq.input-type/coming-soon)
            top-level? (= :routes.schnaq/start @(rf/subscribe [:navigation/current-route-name]))]
        [motion/fade-in-and-out
         [:section.selection-card
          [topic-or-search-content]
          [:ul.nav.nav-tabs
           [:li.nav-item
            [:a.nav-link {:class (active-class :question)
                          :href "#"
                          :on-click #(on-click :question)}
             [iconed-heading :info-question (if top-level? :schnaq.input-type/question :schnaq.input-type/answer)]]]
           (when top-level?
             (if (and beta-user? admin?)
               [:<>
                [:li.nav-item
                 [:a.nav-link
                  {:class (active-class :survey)
                   :href "#"
                   :on-click #(on-click :survey)}
                  survey-tab]]
                [:li.nav-item
                 [:a.nav-link
                  {:class (active-class :activation)
                   :href "#"
                   :on-click #(on-click :activation)}
                  activation-tab]]]
               [:<>
                [:li.nav-item
                 [:a.nav-link.text-muted
                  {:href "#"}
                  [tooltip/text (labels disabled-tooltip-key) survey-tab]]]
                [:li.nav-item
                 [:a.nav-link.text-muted
                  {:href "#"}
                  [tooltip/text (labels disabled-tooltip-key) activation-tab]]]]))]
          (case @selected-option
            :question [input-form-or-disabled-alert]
            :survey [survey/survey-form]
            :activation [activation/activation-tab])]
         card-fade-in-time]))))

(defn- statements-list []
  (let [active-filters @(rf/subscribe [:filters/active])
        sort-method @(rf/subscribe [:discussion.statements/sort-method])
        local-votes @(rf/subscribe [:local-votes])
        user @(rf/subscribe [:user/current])
        shown-premises @(rf/subscribe [:discussion.statements/show])
        sorted-conclusions (sort-statements user shown-premises sort-method local-votes)
        filtered-conclusions (filters/filter-statements sorted-conclusions active-filters @(rf/subscribe [:local-votes]))]
    (for [statement filtered-conclusions]
      [:div.statement-column
       {:key (:db/id statement)}
       [motion/fade-in-and-out
        [answer-or-edit-card statement]
        card-fade-in-time]])))

(defn conclusion-cards-list
  "Prepare a list of statements and group them together."
  []
  (let [search? (not= "" @(rf/subscribe [:schnaq.search.current/search-string]))
        statements (statements-list)
        top-level? @(rf/subscribe [:schnaq.routes/starting?])
        activation (when top-level? [activation/activation-card])
        surveys (when top-level? (survey/survey-list))
        access-code @(rf/subscribe [:schnaq.selected/access-code])]
    [:div.row
     [:div.statement-column
      [selection-card]]
     activation
     surveys
     statements
     (when-not (or search? (seq statements) (seq surveys) (not access-code))
       [call-to-share])]))

(rf/reg-event-fx
 :discussion.select/conclusion
 (fn [{:keys [db]} [_ conclusion]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:db (assoc-in db [:discussion :conclusion :selected] conclusion)
      :fx [(http/xhrio-request db :get "/discussion/statements/for-conclusion"
                               [:discussion.premises/set-current]
                               {:conclusion-id (:db/id conclusion)
                                :share-hash share-hash
                                :display-name (tools/current-display-name db)}
                               [:ajax.error/as-notification])]})))

(rf/reg-event-fx
 :discussion.statements/reload
 (fn [{:keys [db]} _]
   (let [path (get-in db [:current-route :data :name])]
     (case path
       :routes.schnaq.select/statement {:fx [[:dispatch [:discussion.query.statement/by-id]]]}
       :routes.schnaq/start {:fx [[:dispatch [:discussion.query.conclusions/starting]]]}
       {}))))

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
 (fn [{:keys [db]} [_ {:keys [db/id meta/upvoted?] :as statement}]]
   {:db (-> db
            (update-in [:votes :own :up id] #(not (if (nil? %) upvoted? %)))
            (assoc-in [:votes :own :down id] false))
    :fx [(http/xhrio-request db :post "/discussion/statement/vote/up" [:upvote-success statement]
                             {:statement-id id
                              :nickname (tools/current-display-name db)
                              :share-hash (-> db :schnaq :selected :discussion/share-hash)}
                             [:ajax.error/as-notification])]}))

(rf/reg-event-fx
 :discussion/toggle-downvote
 (fn [{:keys [db]} [_ {:keys [db/id meta/downvoted?] :as statement}]]
   {:db (-> db
            (assoc-in [:votes :own :up id] false)
            (update-in [:votes :own :down id] #(not (if (nil? %) downvoted? %))))
    :fx [(http/xhrio-request db :post "/discussion/statement/vote/down" [:downvote-success statement]
                             {:statement-id id
                              :nickname (tools/current-display-name db)
                              :share-hash (-> db :schnaq :selected :discussion/share-hash)}
                             [:ajax.error/as-notification])]}))

(rf/reg-event-db
 :upvote-success
 (fn [db [_ {:keys [db/id]} {:keys [operation]}]]
   (case operation
     :added (update-in db [:votes :up id] inc)
     :removed (update-in db [:votes :up id] dec)
     :switched (-> db
                   (update-in [:votes :up id] inc)
                   (update-in [:votes :down id] dec)))))

(rf/reg-event-db
 :downvote-success
 (fn [db [_ {:keys [db/id]} {:keys [operation]}]]
   (case operation
     :added (update-in db [:votes :down id] inc)
     :removed (update-in db [:votes :down id] dec)
     :switched (-> db
                   (update-in [:votes :down id] inc)
                   (update-in [:votes :up id] dec)))))

(rf/reg-sub
 :votes/upvoted-or-downvoted
 (fn [db [_ statement-id]]
   [(get-in db [:votes :own :up statement-id])
    (get-in db [:votes :own :down statement-id])]))
