(ns schnaq.interface.views.discussion.conclusion-card
  (:require [clj-fuzzy.metrics :as clj-fuzzy]
            [clojure.string :as cstring]
            [com.fulcrologic.guardrails.core :refer [>defn- ?]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [schnaq.database.specs :as specs]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.components.schnaq :as sc]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.markdown :as md]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.card-elements :as elements]
            [schnaq.interface.views.discussion.edit :as edit]
            [schnaq.interface.views.discussion.input :as input]
            [schnaq.interface.views.discussion.labels :as labels]
            [schnaq.interface.views.discussion.truncated-content :as truncated-content]
            [schnaq.interface.views.loading :as loading]
            [schnaq.interface.views.schnaq.activation :as activation]
            [schnaq.interface.views.schnaq.poll :as poll]
            [schnaq.interface.views.schnaq.reactions :as reactions]
            [schnaq.interface.views.schnaq.wordcloud-card :as wordcloud-card]
            [schnaq.interface.views.user :as user]
            [schnaq.shared-toolbelt :as shared-tools]))

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
    [:p.pt-3 [icon :info "m-auto fas"] " "
     (labels :qanda.call-to-action/help) " " [icon :share "m-auto fas"]]]])

;; -----------------------------------------------------------------------------

(defn- statement-information-row [statement]
  (let [statement-id (:db/id statement)]
    [:div.d-flex.flex-wrap.align-items-center
     [reactions/up-down-vote statement]
     [:div.ms-sm-0.ms-lg-auto.d-flex.align-items-center
      (when ((set (:statement/labels statement)) ":question")
        [labels/build-label ":question"])
      (if (:statement/locked? statement)
        [elements/locked-statement-icon statement-id]
        [badges/show-number-of-replies statement])
      (when (:statement/pinned? statement)
        [elements/pinned-statement-icon statement-id])]]))

(defn- mark-as-answer-button
  "Show a button to mark a statement as an answer."
  [statement]
  (let [statement-labels (set (:statement/labels statement))
        label ":check"
        checked? (statement-labels label)
        authenticated? @(rf/subscribe [:user/authenticated?])
        mods-mark-only? @(rf/subscribe [:schnaq.selected.qa/mods-mark-only?])
        show-button? (or (not mods-mark-only?)
                         (and mods-mark-only? authenticated? @(rf/subscribe [:schnaq/edit-hash])))]
    (when show-button?
      [:section.w-100
       [:button.btn.btn-sm.btn-link.text-dark.pe-0
        {:on-click #(if checked?
                      (rf/dispatch [:statement.labels/remove statement label])
                      (rf/dispatch [:statement.labels/add statement label]))}
        [:small.pe-2 (if checked?
                       (labels :qanda.button.mark/as-unanswered)
                       (labels :qanda.button.mark/as-answer))]
        [labels/build-label (if checked? label ":unchecked")]]])))

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
        read-only? @(rf/subscribe [:schnaq.selected/read-only?])
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
         [user/user-info statement 25 "w-100"]
         [badges/edit-statement-dropdown-menu statement]]
        [:div.text-typography
         [truncated-content/statement statement]]
        [:div.d-flex.flex-wrap.align-items-center
         [discuss-answer-button statement]
         [reactions/up-down-vote statement]
         [:div.d-flex.flex-row.align-items-center.ms-auto
          [mark-as-answer-button statement]]]]]]]))

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

(defn- replies [_statement-id]
  (let [collapsed? (reagent/atom true)]
    (fn [statement-id]
      (let [reply-ids @(rf/subscribe [:statements/replies statement-id])
            rotation (if @collapsed? 0 180)
            button-icon [motion/rotate rotation [icon :collapse-down "my-auto"]]
            button-content (if @collapsed?
                             (labels :qanda.button.show/replies)
                             (labels :qanda.button.hide/replies))]
        (when (not-empty reply-ids)
          [:<>
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
     [:div.d-flex.flex-row
      [card-highlighting statement]
      [:div.card-view.card-body.py-2.px-0
       (when (:meta/new? statement)
         [:div.bg-primary.p-2.rounded-1.d-inline-block.text-white.small.float-end
          (labels :discussion.badges/new)])
       [:div.pt-2.d-flex.px-3
        [:div.me-auto [user/user-info statement 32 "w-100"]]
        [:div.d-flex.flex-row.align-items-center.ms-auto
         (when-not @(rf/subscribe [:schnaq.routes/starting?])
           [mark-as-answer-button statement])
         [badges/edit-statement-dropdown-menu statement]]]
       [:div.my-4]
       [:div.text-typography.px-3
        [truncated-content/statement statement]
        [statement-information-row statement]]
       [:div.mx-3
        [input/reply-in-statement-input-form statement]
        [answers statement-id]
        [replies statement-id]]]]]))

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
                :popular #(reactions/calculate-votes % local-votes))
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
    [:<>
     [icon :lock "mx-2 text-primary"]
     [:span.small.text-muted (labels :discussion.state/read-only-warning)]]
    [input/input-form]))

;; -----------------------------------------------------------------------------

(defn- current-topic-badges
  "Badges which are shown if a statement is selected."
  [schnaq statement]
  (let [starting-route? @(rf/subscribe [:schnaq.routes/starting?])]
    [:div.ms-auto
     (if starting-route?
       [badges/static-info-badges-discussion schnaq]
       [:div.d-flex.flex-row
        [badges/show-number-of-replies statement]
        [reactions/up-down-vote statement]
        [badges/edit-statement-dropdown-menu statement]])]))

(defn- title-view [statement]
  (let [starting-route? @(rf/subscribe [:schnaq.routes/starting?])
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
        starting-route? @(rf/subscribe [:schnaq.routes/starting?])
        statement-or-topic (if starting-route? content @(rf/subscribe [:schnaq.statements/focus]))]
    [motion/move-in :left
     [:<>
      [:div.d-flex.flex-wrap.mb-3
       [:div.small
        [user/user-info statement-or-topic 20 nil]]
       [current-topic-badges schnaq statement-or-topic]]
      [title-view statement-or-topic]]]))

(defn- search-info []
  (let [search-string @(rf/subscribe [:schnaq.search.current/search-string])
        search-results @(rf/subscribe [:schnaq.search.current/result])]
    [motion/move-in :left
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
  (let [pro-user? @(rf/subscribe [:user/pro-user?])
        admin-access? @(rf/subscribe [:schnaq.current/admin-access])
        disabled-tooltip-key (cond
                               (not pro-user?) :schnaq.input-type/pro-only
                               (not admin-access?) :schnaq.input-type/not-admin
                               :else :schnaq.input-type/coming-soon)]
    [:li.nav-item
     [:button.nav-link.text-muted
      {:role "button"}
      [tooltip/text
       (labels disabled-tooltip-key)
       tab-content]]]))

(defn selection-card
  "Dispatch the different input options, e.g. questions, poll or activation.
  The poll and activation feature are not available for free plan users."
  []
  ;; TODO add info card everywhere the selection card is used
  (let [selected-option (reagent/atom :question)
        on-click #(reset! selected-option %)
        active-class #(when (= @selected-option %) "active")
        iconed-heading (fn [icon-key label]
                         [:<> [icon icon-key] " " (labels label)])]
    (fn []
      (let [poll-tab [:span [iconed-heading :chart-pie :schnaq.input-type/poll]]
            activation-tab [:span [iconed-heading :magic :schnaq.input-type/activation]]
            word-cloud-tab [:span [iconed-heading :cloud :schnaq.input-type/word-cloud]]
            pro-user? @(rf/subscribe [:user/pro-user?])
            admin-access? @(rf/subscribe [:schnaq.current/admin-access])
            read-only? @(rf/subscribe [:schnaq.selected/read-only?])
            top-level? (= :routes.schnaq/start @(rf/subscribe [:navigation/current-route-name]))]
        [motion/fade-in-and-out
         [:section.selection-card
          [:div.card-view.card-body
           (when-not read-only?
             [:ul.selection-tab.nav.nav-tabs
              [:li.nav-item
               [:button.nav-link {:class (active-class :question)
                                  :role "button"
                                  :on-click #(on-click :question)}
                [iconed-heading :info-question :schnaq.input-type/statement]]]
              (when top-level?
                (if (and pro-user? admin-access?)
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
                   [deactivated-selection-card-tab word-cloud-tab]]))])
           (case @selected-option
             :question [input-form-or-disabled-alert]
             :poll [poll/poll-form]
             :activation [activation/activation-tab]
             :word-cloud [wordcloud-card/wordcloud-tab])]]
         motion/card-fade-in-time]))))

(defn- delay-fade-in-for-subsequent-content [index]
  (+ (/ (inc index) 20) motion/card-fade-in-time))

(defn- some-levenshtein
  "Takes a list of strings and is truthy if the target-string matches any of the tokens
  without breaking the max levenshtein-distance."
  [target-string tokens]
  (some #(> 2 (clj-fuzzy/levenshtein target-string %)) tokens))

(defn- score-hit
  "Returns a numerical score how many tokens of `token-list` are a match for the
  supplied `string`."
  [token-list string]
  (let [string-tokens (shared-tools/tokenize-string string)]
    (->> token-list
         (map #(some-levenshtein % string-tokens))
         (filter true?)
         count)))

(defn- statement-list-item
  "The highest part of a statement-list. Knows when to show itself and when not."
  [statement-id index]
  (let [answered-only? @(rf/subscribe [:filters/answered? true])
        unanswered-only? @(rf/subscribe [:filters/answered? false])
        answers @(rf/subscribe [:statements/answers statement-id])
        item-component
        [:div.statement-column
         [motion/fade-in-and-out
          [statement-card->editable-card statement-id [statement-card statement-id]]
          (delay-fade-in-for-subsequent-content index)]]]
    (if (or answered-only? unanswered-only?)
      ;; Other filters are handled in statement-list-generator
      (when (or (and answered-only? (seq answers))
                (and (not answered-only?) (empty? answers)))
        item-component)
      item-component)))

(rf/reg-sub
 :schnaq.statements/filtered-sorted-visible
 :<- [:discussion.statements/sort-method]
 :<- [:local-votes]
 :<- [:discussion.statements/show]
 :<- [:user/current]
 :<- [:schnaq.question.input/current]
 :<- [:filters/questions?]
 (fn [[sort-method local-votes shown-statements user current-input-tokens questions-only?] _]
   (let [question-filtered-statements (if questions-only?
                                        (filter #((set (:statement/labels %)) ":question") shown-statements)
                                        shown-statements)
         sorted-conclusions (sort-statements user question-filtered-statements sort-method local-votes)
         grouped-statements (group-by #(true? (:statement/pinned? %)) sorted-conclusions)
         input-filtered-statements
         (if (> 101 (count sorted-conclusions))
           ;; Temporary toggle. Remove if performance with lots of statements is better
           (sort-by #(score-hit current-input-tokens (:statement/content %)) > (get grouped-statements false))
           (get grouped-statements false))
         pinned-statements (get grouped-statements true)
         sorted-filtered-statements (concat pinned-statements input-filtered-statements)]
     (map :db/id sorted-filtered-statements))))

(defn- statements-list []
  (let [sorted-statements @(rf/subscribe [:schnaq.statements/filtered-sorted-visible])]
    (for [index (range (count sorted-statements))
          :let [statement-id (nth sorted-statements index)]]
      (with-meta
        [statement-list-item statement-id index]
        {:key statement-id}))))

(defn card-container
  "Prepare a list of visible cards and group them together."
  []
  (let [search? (not= "" @(rf/subscribe [:schnaq.search.current/search-string]))
        statements (statements-list)
        top-level? @(rf/subscribe [:schnaq.routes/starting?])
        schnaq-loading? @(rf/subscribe [:loading/schnaq?])
        activation (when top-level? [activation/activation-card])
        polls (when top-level? (poll/poll-list))
        wordcloud (when top-level? [wordcloud-card/wordcloud-card])
        access-code @(rf/subscribe [:schnaq.selected/access-code])
        question-input @(rf/subscribe [:schnaq.question.input/current])
        show-call-to-share? (and top-level? access-code
                                 (not (or search? (seq statements) (seq polls))))
        _cards (if (not-empty question-input)
                 ;; TODO evaluate if showing statements first while typing is needed at all
                 [statements activation polls wordcloud]
                 [activation polls wordcloud statements])]
    (if schnaq-loading?
      [loading/loading-card]
      [:<>
       [:div.row
        [:div.statement-column
         [info-card]
         [selection-card]]
        ;; TODO show all interactions in scrollable slideshow
        activation polls wordcloud]
       [:div.row
        (if show-call-to-share?
          [call-to-share]
          statements)]])))
