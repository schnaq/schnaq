(ns schnaq.interface.views.discussion.input
  (:require [goog.string :refer [format]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.lexical.editor :as lexical]
            [schnaq.interface.config :as config]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.card-elements :as card-elements]
            [schnaq.interface.views.discussion.logic :as logic]
            [schnaq.interface.views.user :as user]
            [schnaq.user :refer [display-name posts-limit-reached?]]))

(defn- post-limit-reached-alert []
  (let [author @(rf/subscribe [:schnaq/author])]
    [:div.alert.alert-primary {:role :alert}
     (labels :feature.limit.posts/alert)
     " "
     (display-name author)]))

(defn- statement-type-button
  "Button to select the attitude of a statement. Current attitude is subscribed via get-subscription.
  On-Click triggers the set-event with statement-type as last parameter."
  [statement-type label tooltip get-subscription set-event]
  (let [current-attitude @(rf/subscribe get-subscription)
        checked? (= statement-type current-attitude)
        uuid (random-uuid)]
    [:<>
     [:input.btn-check {:id uuid
                        :type "radio" :name "options" :autoComplete "off"
                        :title (labels tooltip)
                        :on-click (fn [e] (.preventDefault e)
                                    (rf/dispatch (conj set-event statement-type)))}]
     [:label.btn.btn-outline-dark
      (cond-> {:for uuid}
        checked? (assoc :class "active"))
      (labels label)]]))

(defn statement-type-choose-button
  "Button group to differentiate between the statement types. The button with a matching get-subscription will be checked.
  Clicking a button will dispatch the set-subscription with the button-type as parameter."
  [get-subscription set-event sm?]
  (let [additional-btn-class (if sm? "btn-group-sm" "")]
    [:div.btn-group.mt-1.me-2 {:class additional-btn-class}
     [statement-type-button :statement.type/support
      :discussion.add.button/support :discussion/add-premise-against
      get-subscription set-event]
     [statement-type-button :statement.type/neutral
      :discussion.add.button/neutral :discussion/add-premise-neutral
      get-subscription set-event]
     [statement-type-button :statement.type/attack
      :discussion.add.button/attack :discussion/add-premise-supporting
      get-subscription set-event]]))

(defn- textarea-highlighting
  "Add highlighting to textarea based on the selected attitude."
  [field]
  (let [attitude (case @(rf/subscribe [:form/statement-type field])
                   :statement.type/support "support"
                   :statement.type/attack "attack"
                   "neutral")]
    [:div.highlight-card-reduced.highlight-card-reverse
     {:class (str "highlight-card-" attitude)}]))

(defn- premise-card-editor
  "Input, where users provide premises."
  [{:keys [db/id]} editor-id]
  (let [editor-content @(rf/subscribe [:editor/content editor-id])]
    [:<>
     [:div.input-group [textarea-highlighting id]
      [:input {:type :hidden
               :name "statement"
               :value (or editor-content "")}]
      [lexical/editor {:id editor-id
                       :file-storage :schnaq/by-share-hash
                       :placeholder (labels :statement.new/placeholder)
                       :toolbar? false}
       {:className "flex-grow-1 lexical-editor-sm"}]
      [:button.btn.btn-sm.btn-outline-dark
       {:type :submit
        :disabled (empty? editor-content)
        :title (labels :discussion/create-argument-action)
        :on-click #(matomo/track-event "Active User" "Action" "Submit Post")}
       [:div.d-flex.flex-row
        [:div.d-none.d-lg-block.me-1 (labels :statement/new)]
        [icon :plane "m-auto"]]]]]))

(defn- conclusion-card-editor
  "Input, where users provide (starting) conclusions."
  [editor-id]
  (let [author @(rf/subscribe [:schnaq/author])
        schnaq @(rf/subscribe [:schnaq/selected])
        limit-reached? (posts-limit-reached? author schnaq)
        editor-content @(rf/subscribe [:editor/content editor-id])]
    (if (and limit-reached? shared-config/enforce-limits?)
      [post-limit-reached-alert]
      (when-not @(rf/subscribe [:schnaq.selected/read-only?])
        [:<>
         [:div.input-group
          [textarea-highlighting :selected]
          [:input {:type :hidden
                   :name "statement"
                   :value (or editor-content "")}]
          [lexical/editor {:id editor-id
                           :file-storage :schnaq/by-share-hash
                           :toolbar? true
                           :focus? (not config/in-iframe?)
                           :placeholder (labels :statement.new/placeholder)}
           {:className "flex-grow-1"}]
          [:button.btn.btn-outline-secondary
           {:type :submit
            :disabled (empty? editor-content)
            :title (labels :discussion/create-argument-action)
            :on-click #(matomo/track-event "Active User" "Action" "Submit Post")}
           [:div.d-flex.flex-row
            [:div.d-none.d-lg-block.me-1 (labels :statement/new)]
            [icon :plane "m-auto"]]]]
         (when @(rf/subscribe [:user/moderator?])
           [:div.form-check.pt-2
            [:input.form-check-input
             {:type :checkbox
              :name "lock-card?"
              :id "lock-card?"}]
            [:label.form-check-label
             {:for "lock-card?"}
             (labels :discussion/lock-statement)]])]))))

(defn- topic-input-area
  "Input form with an option to chose statement type."
  [editor-id]
  (let [starting-route? @(rf/subscribe [:routes.schnaq/start?])
        pro-con-disabled? @(rf/subscribe [:schnaq.selected/pro-con?])]
    [:<>
     [:div.pb-3
      [user/current-user-info 40 "text-primary fs-6"]]
     [conclusion-card-editor editor-id]
     (when-not (or starting-route? pro-con-disabled?)
       [:div.mt-3
        [statement-type-choose-button
         [:form/statement-type :selected]
         [:form/statement-type! :selected]]])]))

(defn input-form
  "Form to collect the user's statements."
  []
  (let [starting-route? @(rf/subscribe [:routes.schnaq/start?])
        when-starting #(let [form (oget % [:currentTarget :elements])
                             statement-text (oget form [:statement :value])
                             locked? (boolean (oget form ["?lock-card?" :checked]))]
                         (rf/dispatch [:discussion.add.statement/starting statement-text locked?]))
        when-deeper-in-discussion #(logic/submit-new-premise (oget % [:currentTarget :elements]))
        event-to-send (if starting-route? when-starting when-deeper-in-discussion)
        editor-id :conclusion-card-editor
        submit-fn (fn [e]
                    (.preventDefault e)
                    (rf/dispatch [:editor/clear editor-id])
                    (event-to-send e))]
    (if (:statement/locked? @(rf/subscribe [:schnaq.statements/focus]))
      [:div.pt-3.ps-1
       [card-elements/locked-statement-icon]]
      [:form.my-md-2
       {:on-submit submit-fn
        :on-key-down #(when (toolbelt/ctrl-press? % "Enter") (submit-fn %))}
       [topic-input-area editor-id]])))

(defn reply-in-statement-input-form
  "Input form inside a statement card. This form is used to directly reply to a statement inside its own card."
  [statement]
  (let [author @(rf/subscribe [:schnaq/author])
        schnaq @(rf/subscribe [:schnaq/selected])
        limit-reached? (posts-limit-reached? author schnaq)
        statement-id (:db/id statement)
        statement-type @(rf/subscribe [:form/statement-type statement-id])
        pro-con-disabled? @(rf/subscribe [:schnaq.selected/pro-con?])
        read-only? @(rf/subscribe [:schnaq.selected/read-only?])
        locked? (:statement/locked? statement)
        hide-input-replies @(rf/subscribe [:ui/setting :hide-input-replies])
        editor-id (format "%s-%s" "premise-card-editor" (:db/id statement))
        answer-to-statement-event
        (fn [e]
          (.preventDefault e)
          (rf/dispatch [:editor/clear editor-id])
          (logic/reply-to-statement (:db/id statement) statement-type (oget e [:currentTarget :elements])))
        forbidden-write? (or locked? read-only? hide-input-replies (and limit-reached? shared-config/enforce-limits?))]
    [:form.my-md-2
     {:on-submit #(answer-to-statement-event %)
      :on-key-down #(when (toolbelt/ctrl-press? % "Enter")
                      (answer-to-statement-event %))}
     (when-not forbidden-write?
       [premise-card-editor statement editor-id])
     [:div.d-flex.flex-wrap
      (when-not (or forbidden-write? pro-con-disabled?)
        [statement-type-choose-button
         [:form/statement-type statement-id]
         [:form/statement-type! statement-id] true])
      [:div.ms-auto.small.mt-2.flex-shrink-1 [user/user-info statement 20 "w-100"]]]]))

(rf/reg-event-db
 ;; Assoc statement-type with statement-id as key. The current topic is assigned via :selected
 :form/statement-type!
 (fn [db [_ statement-id statement-type]]
   (assoc-in db [:form :statement-type statement-id] statement-type)))

(rf/reg-sub
 ;; Get corresponding statement-type via statement-id. The current topic is accessed via :selected
 :form/statement-type
 (fn [db [_ statement-id]]
   (get-in db [:form :statement-type statement-id] :statement.type/neutral)))
