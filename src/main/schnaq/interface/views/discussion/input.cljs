(ns schnaq.interface.views.discussion.input
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.card-elements :as card-elements]
            [schnaq.interface.views.discussion.logic :as logic]
            [schnaq.shared-toolbelt :as shared-tools]))

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
    [:div.btn-group.mt-1.ms-1 {:class additional-btn-class}
     [statement-type-button :statement.type/support
      :discussion.add.button/support :discussion/add-premise-against
      get-subscription set-event]
     [statement-type-button :statement.type/neutral
      :discussion.add.button/neutral :discussion/add-premise-neutral
      get-subscription set-event]
     [statement-type-button :statement.type/attack
      :discussion.add.button/attack :discussion/add-premise-supporting
      get-subscription set-event]]))

(rf/reg-event-db
 :schnaq.question.input/set-current
 (fn [db [_ current-input]]
   (assoc-in db [:schnaq :question :input] current-input)))

(rf/reg-sub
 :schnaq.question.input/current
 (fn [db _]
   (shared-tools/tokenize-string (get-in db [:schnaq :question :input] ""))))

(defn- textarea-highlighting
  "Add highlighting to textarea based on the selected attitude."
  [field]
  (let [attitude (case @(rf/subscribe [:form/statement-type field])
                   :statement.type/support "support"
                   :statement.type/attack "attack"
                   "neutral")]
    [:div.highlight-card-reduced.highlight-card-reverse
     {:class (str "highlight-card-" attitude)}]))

(defn- premise-card-textarea
  "Input, where users provide premises."
  [{:keys [db/id]}]
  [:div.input-group
   [textarea-highlighting id]
   [:textarea.form-control.form-control-sm.textarea-resize-none
    {:name "statement" :wrap "soft" :rows 1
     :auto-complete "off"
     :onInput #(toolbelt/height-to-scrollheight! (oget % :target))
     :required true
     :data-dynamic-height true
     :placeholder (labels :statement.new/placeholder)}]
   [:button.btn.btn-sm.btn-outline-dark
    {:type "submit"
     :title (labels :discussion/create-argument-action)
     :on-click #(matomo/track-event "Active User", "Action", "Submit Post")}
    [:div.d-flex.flex-row
     [:div.d-none.d-lg-block.me-1 (labels :statement/new)]
     [icon :plane "m-auto"]]]])

(defn- conclusion-card-textarea
  "Input, where users provide (starting) conclusions."
  []
  (when-not @(rf/subscribe [:schnaq.selected/read-only?])
    [:<>
     [:div.input-group
      [textarea-highlighting :selected]
      [:textarea.form-control.textarea-resize-none
       {:name "statement" :wrap "soft" :rows 1
        :auto-complete "off"
        :autoFocus true
        :onInput #(toolbelt/height-to-scrollheight! (oget % :target))
        :required true
        :data-dynamic-height true
        :placeholder (labels :statement.new/placeholder)
        :on-key-up #(rf/dispatch [:schnaq.question.input/set-current (oget % [:?target :value])])}]
      [:button.btn.btn-outline-dark
       {:type "submit"
        :title (labels :discussion/create-argument-action)
        :on-click #(matomo/track-event "Active User", "Action", "Submit Post")}
       [:div.d-flex.flex-row
        [:div.d-none.d-lg-block.me-1 (labels :statement/new)]
        [icon :plane "m-auto"]]]]
     (when (and @(rf/subscribe [:user/authenticated?]) @(rf/subscribe [:schnaq.current/admin-access]))
       [:div.form-check.pt-2
        [:input.form-check-input
         {:type "checkbox"
          :name "lock-card?"}]
        [:label.form-check-label
         {:for "lock-card?"}
         (labels :discussion/lock-statement)]])]))

(defn- topic-input-area
  "Input form with an option to chose statement type."
  []
  (let [starting-route? @(rf/subscribe [:schnaq.routes/starting?])
        pro-con-disabled? @(rf/subscribe [:schnaq.selected/pro-con?])]
    [:<>
     [conclusion-card-textarea]
     (when-not (or starting-route? pro-con-disabled?)
       [:div.mt-3
        [statement-type-choose-button
         [:form/statement-type :selected]
         [:form/statement-type! :selected]]])]))

(defn input-form
  "Form to collect the user's statements."
  []
  (let [starting-route? @(rf/subscribe [:schnaq.routes/starting?])
        when-starting (fn [e]
                        (.preventDefault e)
                        (rf/dispatch [:discussion.add.statement/starting (oget e [:currentTarget :elements])]))
        when-deeper-in-discussion (fn [e]
                                    (.preventDefault e)
                                    (logic/submit-new-premise (oget e [:currentTarget :elements])))
        event-to-send (if starting-route?
                        when-starting when-deeper-in-discussion)]
    (if (:statement/locked? @(rf/subscribe [:discussion.conclusion/selected]))
      [:div.pt-3.ps-1
       [card-elements/locked-statement-icon]]
      [:form.my-md-2
       {:on-submit #(event-to-send %)
        :on-key-down #(when (toolbelt/ctrl-press? % 13) (event-to-send %))}
       [topic-input-area]])))

(defn reply-in-statement-input-form
  "Input form inside a statement card. This form is used to directly reply to a statement inside its own card."
  [statement]
  (let [statement-id (:db/id statement)
        statement-type @(rf/subscribe [:form/statement-type statement-id])
        pro-con-disabled? @(rf/subscribe [:schnaq.selected/pro-con?])
        read-only? @(rf/subscribe [:schnaq.selected/read-only?])
        locked? (:statement/locked? statement)
        answer-to-statement-event
        (fn [e]
          (.preventDefault e)
          (logic/reply-to-statement statement statement-type (oget e [:currentTarget :elements])))]
    (when-not (or locked? read-only?)
      [:form.my-md-2
       {:on-submit #(answer-to-statement-event %)
        :on-key-down #(when (toolbelt/ctrl-press? % 13)
                        (answer-to-statement-event %))}
       [premise-card-textarea statement]
       (when-not pro-con-disabled?
         [statement-type-choose-button
          [:form/statement-type statement-id]
          [:form/statement-type! statement-id] true])])))

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
