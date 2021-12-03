(ns schnaq.interface.views.discussion.input
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.logic :as logic]))

(defn- statement-type-button
  "Button to select the attitude of a statement. Current attitude is subscribed via get-subscription.
  On-Click triggers the set-event with statement-type as last parameter."
  [statement-type label tooltip get-subscription set-event]
  (let [current-attitude @(rf/subscribe get-subscription)
        checked? (= statement-type current-attitude)]
    [:label.btn.btn-outline-dark.shadow-sm.py-2
     (when checked? {:class "active"})
     [:input {:type "radio" :name "options" :autoComplete "off"
              :defaultChecked checked?
              :title (labels tooltip)
              :on-click (fn [e] (jq/prevent-default e)
                          (rf/dispatch (conj set-event statement-type)))}]
     (labels label)]))

(defn statement-type-choose-button
  "Button group to differentiate between the statement types. The button with a matching get-subscription will be checked.
  Clicking a button will dispatch the set-subscription with the button-type as parameter."
  [get-subscription set-event]
  [:div.btn-group.btn-group-toggle {:data-toggle "buttons"}
   [statement-type-button :statement.type/support
    :discussion.add.button/support :discussion/add-premise-against
    get-subscription set-event]
   [statement-type-button :statement.type/neutral
    :discussion.add.button/neutral :discussion/add-premise-neutral
    get-subscription set-event]
   [statement-type-button :statement.type/attack
    :discussion.add.button/attack :discussion/add-premise-supporting
    get-subscription set-event]])

(defn- textarea-for-statements
  "Input, where users provide (starting) conclusions."
  [textarea-name placeholder send-button-label statement-type]
  (let [attitude (case statement-type
                   :statement.type/support "support"
                   :statement.type/attack "attack"
                   :statement.type/neutral "neutral")]
    [:div.input-group
     [:div {:class (str "highlight-card-reverse highlight-card-" attitude)}]
     [:textarea.form-control.textarea-resize-none
      {:name textarea-name :wrap "soft" :rows 1
       :auto-complete "off"
       :autoFocus true
       :onInput #(toolbelt/height-to-scrollheight! (oget % :target))
       ;; first reset input then set height +1px in order to prevent scrolling
       :required true
       :data-dynamic-height true
       :placeholder placeholder}]
     [:div.input-group-append
      [:button.btn.btn-outline-dark
       {:type "submit" :title (labels :discussion/create-argument-action)}
       [:div.d-flex.flex-row
        [:div.d-none.d-lg-block.mr-1 send-button-label]
        [icon :plane "m-auto"]]]]]))

(defn- topic-input-area
  "Input form with an option to chose statement type."
  [textarea-name]
  (let [current-route-name @(rf/subscribe [:navigation/current-route-name])
        starting-route? (= :routes.schnaq/start current-route-name)
        pro-con-disabled? @(rf/subscribe [:schnaq.selected/pro-con?])
        statement-type (if starting-route? :statement.type/neutral
                                           @(rf/subscribe [:form/statement-type]))
        send-button-label (if starting-route? (labels :statement/ask)
                                              (labels :statement/reply))
        placeholder (if starting-route? (labels :statement.ask/placeholder)
                                        (labels :statement.reply/placeholder))]
    [:<>
     [textarea-for-statements
      textarea-name
      placeholder
      send-button-label
      statement-type]
     (when-not (or starting-route? pro-con-disabled?)
       [:div.input-group-prepend.mt-3
        [statement-type-choose-button [:form/statement-type] [:form/statement-type!]]])]))

(defn input-form
  "Form to collect the user's statements."
  [textarea-name]
  (let [current-route-name @(rf/subscribe [:navigation/current-route-name])
        starting-route? (= :routes.schnaq/start current-route-name)
        when-starting (fn [e] (jq/prevent-default e)
                        (rf/dispatch [:discussion.add.statement/starting
                                      (oget e [:currentTarget :elements])]))
        when-deeper-in-discussion (fn [e]
                                    (jq/prevent-default e)
                                    (logic/submit-new-premise (oget e [:currentTarget :elements])))
        event-to-send (if starting-route?
                        when-starting when-deeper-in-discussion)]
    [:form.my-md-2
     {:on-submit #(event-to-send %)
      :on-key-down #(when (jq/ctrl-press % 13)
                      (event-to-send %))}
     [topic-input-area textarea-name]]))

(defn reply-in-statement-input-form
  "Input form inside a statement card. This form is used to directly reply to a statement inside its own card."
  [statement]
  (let [form-name (str "answer-to-statement-" (:db/id statement))
        placeholder (labels :statement.reply/placeholder)
        send-button-label (labels :statement/reply)
        statement-type :statement.type/neutral
        answer-to-statement-event (fn [e]
                                    (jq/prevent-default e)
                                    (logic/reply-to-statement
                                     statement
                                     statement-type
                                     form-name
                                     (oget e [:currentTarget :elements])))]
    [:form.my-md-2
     {:on-submit #(answer-to-statement-event %)
      :on-key-down #(when (jq/ctrl-press % 13)
                      (answer-to-statement-event %))}
     [textarea-for-statements
      form-name
      placeholder
      send-button-label
      statement-type]]))

(rf/reg-event-db
 :form/statement-type!
 (fn [db [_ statement-type]]
   (assoc-in db [:form :statement-type] statement-type)))

(rf/reg-sub
 :form/statement-type
 (fn [db]
   (get-in db [:form :statement-type] :statement.type/neutral)))
