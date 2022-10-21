(ns schnaq.interface.views.discussion.conclusion-card
  (:require ["react-smart-masonry" :default Masonry]
            [clojure.string :as cstring]
            [com.fulcrologic.guardrails.core :refer [>defn- ?]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [schnaq.database.specs :as specs]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.components.schnaq :as sc]
            [schnaq.interface.config :as config]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.markdown :as md]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.card-elements :as elements]
            [schnaq.interface.views.discussion.edit :as edit]
            [schnaq.interface.views.discussion.input :as input]
            [schnaq.interface.views.discussion.labels :as labels]
            [schnaq.interface.views.discussion.share :refer [small-share-schnaq-button]]
            [schnaq.interface.views.discussion.truncated-content :as truncated-content]
            [schnaq.interface.views.loading :as loading]
            [schnaq.interface.views.schnaq.activation :as activation]
            [schnaq.interface.views.schnaq.activation-cards :as activation-cards]
            [schnaq.interface.views.schnaq.poll :as poll]
            [schnaq.interface.views.schnaq.reactions :as reactions]
            [schnaq.interface.views.schnaq.wordcloud-card :as wordcloud-card]
            [schnaq.interface.views.user :as user]))

(defn- question?
  "Returns whether a statement has been marked as a question."
  [statement]
  (contains? (set (:statement/labels statement)) ":question"))

(defn- call-to-action-schnaq
  "If no contributions are available, add a call to action to engage the users."
  [body]
  [motion/fade-in-and-out
   [:article.call-to-contribute.m-3
    [:div.alert.alert-light.text-light.row.layered-wave-background.p-md-5.rounded-1
     [:div.col-2.py-md-5.d-flex
      [:img.w-75.align-self-center {:src (img-path :schnaqqifant/three-d-head)
                                    :alt (labels :schnaqqifant/three-d-head-alt-text)}]]
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
     [:span.font-monospace.mx-2 {:href "https://schnaq.app"
                                 :target :_blank}
      "https://schnaq.app"]
     " "
     (labels :qanda.call-to-action/intro-2)]
    [:p.pt-3 [icon :info "me-1"]
     (labels :qanda.call-to-action/help)
     [small-share-schnaq-button]]]])

;; -----------------------------------------------------------------------------
(defn- mark-as-answer-button
  "Show a button to mark a statement as an answer."
  [statement]
  (let [statement-labels (set (:statement/labels statement))
        label ":check"
        checked? (statement-labels label)
        mods-mark-only? @(rf/subscribe [:schnaq/state? :discussion.state.qa/mark-as-moderators-only])
        show-button? (and (not @(rf/subscribe [:schnaq.state/read-only?]))
                          (or (not mods-mark-only?)
                              (and mods-mark-only? @(rf/subscribe [:user/moderator?]))))]
    (when show-button?
      [:section
       [:button.btn.btn-sm.btn-link.text-dark.px-0
        {:on-click #(if checked?
                      (rf/dispatch [:statement.labels/remove statement label])
                      (rf/dispatch [:statement.labels/add statement label]))}
        [:small.pe-2 (if checked?
                       (labels :qanda.button.mark/as-unanswered)
                       (labels :qanda.button.mark/as-answer))]
        [labels/build-label (if checked? label ":unchecked")]]])))

(defn- statement-information-row [statement]
  (let [statement-id (:db/id statement)]
    [:div.d-flex.flex-wrap.align-items-center.pb-1
     (if (:statement/locked? statement)
       [elements/locked-statement-icon statement-id]
       [badges/show-number-of-replies statement])
     (when (:statement/pinned? statement)
       [elements/pinned-statement-icon statement-id])
     (when ((set (:statement/labels statement)) ":question")
       [labels/build-label ":question"])
     (when-not @(rf/subscribe [:routes.schnaq/start?])
       [:div.d-flex.flex-row.align-items-center.ms-auto
        [mark-as-answer-button statement]])]))

(>defn- card-highlighting
  "Add card-highlighting to a statement card."
  ([statement]
   [(? ::specs/statement) :ret vector?]
   [card-highlighting statement nil])
  ([statement additional-classes]
   [(? ::specs/statement) (? string?) :ret vector?]
   (let [answered? (seq @(rf/subscribe [:statements/answers (:db/id statement)]))
         statement-type (when (:statement/type statement)
                          (str "-" (name (:statement/type statement))))
         highlight-class (if answered?
                           "highlight-card-answered"
                           (str "highlight-card" statement-type))]
     [:div {:class (str highlight-class " " additional-classes)}])))

(defn- statement-card->editable-card
  "Wrap `statement-card-component`. Check if this statement is currently being
  edited, show edit-card if true."
  [statement-id statement-card-component]
  (let [currently-edited? @(rf/subscribe [:statement.edit/ongoing? statement-id])]
    (if currently-edited?
      [edit/edit-card-statement statement-id]
      statement-card-component)))

(defn- discuss-answer-button [statement]
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])
        statement-num (:meta/sub-statement-count statement 0)
        read-only? @(rf/subscribe [:schnaq.state/read-only?])
        locked? (:statement/locked? statement)
        button-label (if (or read-only? locked?) :statement/replies :statement/discuss)]
    [:a.btn.btn-sm.btn-outline-dark.me-3.px-1.py-0
     {:href (navigation/href :routes.schnaq.select/statement {:share-hash share-hash
                                                              :statement-id (:db/id statement)})}
     [:div.d-flex.flex-wrap.align-items-center
      [:small (labels button-label)]
      [:small.ms-2
       statement-num
       [icon :comment/alt "ms-1"]]]]))

(defn reduced-statement-card
  "A reduced statement-card focusing on the statement."
  [statement-id]
  (let [statement @(rf/subscribe [:schnaq/statement statement-id])]
    [motion/fade-in-and-out
     [:article.statement-card.mt-1.border
      [:div.d-flex.flex-row
       [card-highlighting statement "me-2 highlight-card-reduced"]
       [:div.card-view.card-body.p-2
        [:div.d-flex.justify-content-start.pt-2
         [mark-as-answer-button statement]
         [badges/statement-dropdown-menu {:class "ms-auto"} statement]]
        [:div.text-typography
         [truncated-content/statement statement]]
        [:div.d-flex.flex-wrap.align-items-center
         [discuss-answer-button statement]
         [reactions/up-down-vote statement]
         [:div.d-flex.flex-row.align-items-center.ms-auto
          [:div.small
           [user/user-info statement 20 "w-100"]]]]]]]]))

(defn- reduced-or-edit-card
  "Wrap reduced statement card to make it editable."
  [statement-id]
  [statement-card->editable-card statement-id [reduced-statement-card statement-id]])

(defn- answers [statement-id]
  (let [answers-ids @(rf/subscribe [:statements/answers statement-id])]
    (when (seq answers-ids)
      [:div.mt-2
       (for [answer-id answers-ids]
         (with-meta
           [reduced-or-edit-card answer-id]
           {:key (str "answer-" answer-id)}))])))

(defn- replies [_props _statement-id]
  (let [collapsed? (reagent/atom true)]
    (fn [props statement-id]
      (let [reply-ids @(rf/subscribe [:statements/replies statement-id])
            rotation (if @collapsed? 0 180)
            button-icon [motion/rotate rotation [icon :collapse-down "my-auto"]]
            button-content (if @collapsed?
                             (labels :qanda.button.show/replies)
                             (labels :qanda.button.hide/replies))]
        (when (not-empty reply-ids)
          [:div props
           [:button.btn.btn-transparent.btn-no-outline
            {:type "button" :aria-expanded "false"
             :on-click (fn [_] (swap! collapsed? not))}
            [:span.me-2 button-content] button-icon]
           [motion/collapse-in-out
            @collapsed?
            (for [reply-id reply-ids]
              (with-meta
                [reduced-or-edit-card reply-id]
                {:key (str "reply-" reply-id)}))]])))))

(defn statement-card
  "Display a full interactive statement. Takes `additional-content`, e.g. the
  answer of a question."
  [statement-id]
  (let [statement @(rf/subscribe [:schnaq/statement statement-id])]
    [:article.statement-card
     {:class (if (question? statement) "statement-question" "")}
     [:div.d-flex
      [card-highlighting statement]
      [:div.d-flex.flex-column.w-100.card-body.ps-0
       [:div.d-flex.flex-row
        [:div.flex-grow-1
         [:div.text-typography
          [truncated-content/statement statement]
          [statement-information-row statement]]]
        [:div.px-md-2.px-1
         [badges/statement-dropdown-menu nil statement]
         [reactions/up-down-vote-vertical {:class "pt-1"} statement]
         (when (:meta/new? statement)
           [:div.bg-primary.px-1.rounded-1.d-inline-block.text-white.small
            (labels :discussion.badges/new)])]]
       [:<>
        [input/reply-in-statement-input-form statement]
        [answers statement-id]
        [replies {:class "text-left"} statement-id]]]]]))

(defn- sort-statements
  "Sort statements according to the filter method."
  [statements sort-method local-votes]
  (let [keyfn (case sort-method
                :newest :statement/created-at
                :popular #(reactions/calculate-votes % local-votes))]
    (sort-by keyfn > statements)))

(defn- input-form-or-disabled-alert
  "Dispatch to show input form or an alert that it is currently not allowed to 
  add statements."
  []
  (if @(rf/subscribe [:schnaq.state/read-only?])
    [:<>
     [icon :lock "mx-2 text-primary"]
     [:span.small.text-muted (labels :discussion.state/read-only-warning)]]
    [input/input-form]))

;; -----------------------------------------------------------------------------

(defn- current-topic-badges
  "Badges which are shown if a statement is selected."
  [statement]
  (let [starting-route? @(rf/subscribe [:routes.schnaq/start?])]
    [:div.ms-auto
     (if starting-route?
       [badges/static-info-badges-discussion]
       [:div.d-flex.flex-row
        [badges/show-number-of-replies statement]
        [reactions/up-down-vote statement]
        [badges/statement-dropdown-menu {:class "ms-3"} statement]])]))

(defn- title-view [statement]
  (let [starting-route? @(rf/subscribe [:routes.schnaq/start?])
        title [md/as-markdown (:statement/content statement)]
        edit-active? @(rf/subscribe [:statement.edit/ongoing? (:db/id statement)])]
    (if edit-active?
      (if starting-route?
        [edit/edit-card-discussion statement]
        [edit/edit-card-statement (:db/id statement)])
      [:h2.fs-6 title])))

(defn- topic-bubble-view []
  (let [{:discussion/keys [title author created-at] :as schnaq} @(rf/subscribe [:schnaq/selected])
        content {:db/id (:db/id schnaq)
                 :statement/content title
                 :statement/author author
                 :statement/created-at created-at}
        starting-route? @(rf/subscribe [:routes.schnaq/start?])
        statement-or-topic (if starting-route? content @(rf/subscribe [:schnaq.statements/focus]))]
    [motion/fade-in-and-out
     [:<>
      [:div.d-flex.flex-wrap.mb-3
       [:div.small
        [user/user-info statement-or-topic 20 nil]]
       [current-topic-badges statement-or-topic]]
      [title-view statement-or-topic]]]))

(defn- search-info []
  (let [search-string @(rf/subscribe [:schnaq.search.current/search-string])
        search-results @(rf/subscribe [:schnaq.search.current/result])]
    [motion/fade-in-and-out
     [:div.my-4
      [:div.d-inline-block
       [:h2 (labels :schnaq.search/heading)]
       [:div.row.mx-0.mt-4.mb-3
        [:img.dashboard-info-icon-sm {:src (img-path :icon-search)
                                      :alt (labels :icon.search/alt-text)}]
        [:div.text.display-6.my-auto.mx-3
         search-string]]]
      [:div.row.m-0
       [:img.dashboard-info-icon-sm {:src (img-path :icon-posts)
                                     :alt (labels :icon.posts/alt-text)}]
       (if (empty? search-results)
         [:p.mx-3 (labels :schnaq.search/new-search-title)]
         [:p.mx-3 (str (count search-results) " " (labels :schnaq.search/results))])]]]))

(defn info-card
  "Contains information about the viewed schnaq or focus statement. Depending on the level of view."
  []
  (let [focus-statement @(rf/subscribe [:schnaq.statements/focus])
        answered? (seq @(rf/subscribe [:statements/answers (:db/id focus-statement)]))
        search-inactive? (cstring/blank? @(rf/subscribe [:schnaq.search.current/search-string]))]
    [motion/fade-in-and-out
     [:section.info-card
      [:div.d-flex.flex-row
       (when answered? [:div.highlight-card-answered])
       [:div.card-view.card-body
        (if search-inactive?
          [topic-bubble-view]
          [search-info])]]]
     motion/card-fade-in-time]))

(defn- deactivated-selection-card-tab
  "A single tab that is deactivated."
  [tab-content]
  [:li.nav-item
   [:button.nav-link.text-muted
    {:role "button"}
    [tooltip/text
     (labels :schnaq.input-type/pro-only)
     tab-content]]])

(defn selection-card
  "Dispatch the different input options, e.g. questions, poll or activation.
  The poll and activation feature are not available for free plan users."
  []
  (let [selected-option (reagent/atom :question)
        on-click #(reset! selected-option %)
        active-class #(when (= @selected-option %) "active")
        iconed-heading (fn [icon-key label]
                         [:<> [icon icon-key "me-1"] (labels label)])]
    (fn []
      (let [poll-tab [:span [iconed-heading :chart-pie :schnaq.input-type/poll]]
            activation-tab [:span [iconed-heading :magic :schnaq.input-type/activation]]
            word-cloud-tab [:span [iconed-heading :cloud :schnaq.input-type/word-cloud]]
            pro-user? @(rf/subscribe [:user/pro?])
            moderator? @(rf/subscribe [:user/moderator?])
            read-only? @(rf/subscribe [:schnaq.state/read-only?])
            posts-disabled? @(rf/subscribe [:schnaq.state/posts-disabled?])
            top-level? @(rf/subscribe [:routes.schnaq/start?])]
        (when (or moderator? (not posts-disabled?))
          [motion/fade-in-and-out
           [:section.selection-card
            [:div.card-view.card-body
             (when top-level?
               (when (and (not read-only?) moderator?)
                 [:ul.selection-tab.nav.nav-tabs
                  {:ref (fn [_element]
                          (js/setTimeout #(rf/dispatch [:tour/start-if-not-visited :discussion]) 1000))} ;; wait a second until tour appears
                  [:li.nav-item
                   [:button.nav-link {:class (active-class :question)
                                      :role "button"
                                      :on-click #(on-click :question)}
                    [iconed-heading :info-question :schnaq.input-type/statement]]]
                  (if pro-user?
                    [:<>
                     [:li.nav-item
                      [:button.nav-link
                       {:class (active-class :poll)
                        :role "button"
                        :on-click #(on-click :poll)}
                       poll-tab]]
                     [:li.nav-item
                      [:button.nav-link
                       {:class (active-class :activation)
                        :role "button"
                        :on-click #(on-click :activation)}
                       activation-tab]]
                     [:li.nav-item
                      [:button.nav-link
                       {:class (active-class :word-cloud)
                        :role "button"
                        :on-click #(on-click :word-cloud)}
                       word-cloud-tab]]]
                    [:<>
                     [deactivated-selection-card-tab poll-tab]
                     [deactivated-selection-card-tab activation-tab]
                     [deactivated-selection-card-tab word-cloud-tab]])]))
             (if top-level?
               (case @selected-option
                 :question [input-form-or-disabled-alert]
                 :poll [poll/poll-form]
                 :activation [activation/activation-tab]
                 :word-cloud [wordcloud-card/wordcloud-tab])
               [input-form-or-disabled-alert])]]
           motion/card-fade-in-time])))))

(defn- statement-list-item
  "The highest part of a statement-list."
  [statement-id]
  [motion/fade-in-and-out
   [statement-card->editable-card statement-id [statement-card statement-id]]
   motion/card-fade-in-time])

(rf/reg-sub
 :schnaq.statements/filtered-sorted-visible
 :<- [:discussion.statements/sort-method]
 :<- [:local-votes]
 :<- [:discussion.statements/show]
 :<- [:filters/questions?]
 (fn [[sort-method local-votes shown-statements questions-only?] _]
   (let [question-filtered-statements (if questions-only?
                                        (filter #((set (:statement/labels %)) ":question") shown-statements)
                                        shown-statements)
         sorted-conclusions (sort-statements question-filtered-statements sort-method local-votes)
         grouped-statements (group-by #(true? (:statement/pinned? %)) sorted-conclusions)
         non-pinned-statements (get grouped-statements false)
         pinned-statements (get grouped-statements true)
         sorted-filtered-statements (concat pinned-statements non-pinned-statements)]
     (map :db/id sorted-filtered-statements))))

(defn- statements-list []
  (let [sorted-statements @(rf/subscribe [:schnaq.statements/filtered-sorted-visible])
        answered-only? @(rf/subscribe [:filters/answered? true])
        unanswered-only? @(rf/subscribe [:filters/answered? false])]
    (for [index (range (count sorted-statements))
          :let [statement-id (nth sorted-statements index)
                answers @(rf/subscribe [:statements/answers statement-id])
                any-filter-active (or answered-only? unanswered-only?)]
          ;; This when needs to be done here and not in the statement-list-item to prevent empty components
          :when (or (not any-filter-active)
                    (and any-filter-active
                         (or (and answered-only? (seq answers))
                             (and (not answered-only?) (empty? answers)))))]
      (with-meta
        [statement-list-item statement-id]
        {:key (str "statement-" statement-id)}))))

(defn card-container
  "Prepare a list of visible cards and group them together."
  []
  (let [search? (not= "" @(rf/subscribe [:schnaq.search.current/search-string]))
        statements (doall (statements-list))
        top-level? @(rf/subscribe [:routes.schnaq/start?])
        schnaq-loading? @(rf/subscribe [:loading :schnaq?])
        access-code @(rf/subscribe [:schnaq.selected/access-code])
        hide-input? @(rf/subscribe [:ui/setting :hide-input])
        hide-activations? @(rf/subscribe [:ui/setting :hide-activations])
        number-of-rows @(rf/subscribe [:ui/setting :num-rows])
        show-call-to-share? (and top-level? access-code
                                 (not (or search? (seq statements))))
        activations (when (and (not hide-activations?) @(rf/subscribe [:schnaq/activations?])) [activation-cards/activation-cards])]
    (if schnaq-loading?
      [loading/loading-card]
      [:div.row
       [:> Masonry
        {:autoArrange false ;; autoArrange is turned off, because it produces rendering issues and huge frame rate drops
         :breakpoints config/breakpoints
         :columns {:xs 1 :lg (or number-of-rows 2)}
         :gap 10}
        [:section
         [info-card]
         (when-not hide-input? [selection-card])]
        activations
        statements]

       (when show-call-to-share?
         [call-to-share])])))
