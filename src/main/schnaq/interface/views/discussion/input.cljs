(ns schnaq.interface.views.discussion.input
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.logic :as logic]))

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

;; TODO pack das in den entsprechenden Namespace, falls hier nicht richtig. Sub drunter auch
(rf/reg-event-db
 :schnaq.question.input/set-current
 (fn [db [_ current-input]]
   (assoc-in db [:schnaq :question :input] current-input)))

(rf/reg-sub
 :schnaq.question.input/current
 (fn [db _]
   (get-in db [:schnaq :question :input] "")))

;; TODO beachte auf welcher Ebene gerade getippt wird
;; TODO next: zeige / sortiere nur Inhalte die das Keyword beinhalten
;; TODO? : Maybe hamming distance
;; TODO? : Maybe synonyme? (Aber ohne Spracherkennung schwierig)
(defn- textarea-for-statements
  "Input, where users provide (starting) conclusions."
  [textarea-name placeholder send-button-label statement-type autofocus? small?]
  (when-not @(rf/subscribe [:schnaq.selected/read-only?])
    (let [attitude (case statement-type
                     :statement.type/support "support"
                     :statement.type/attack "attack"
                     :statement.type/neutral "neutral")
          additional-form-class (when small? "form-control-sm")
          additional-btn-class (when small? "btn-sm")
          additional-highlight-class (when small? "highlight-card-reduced ")]
      [:div.input-group
       [:div {:class (str additional-highlight-class "highlight-card-reverse highlight-card-" attitude)}]
       [:textarea.form-control.textarea-resize-none
        {:id "main-question-input"
         :class additional-form-class
         :name textarea-name :wrap "soft" :rows 1
         :auto-complete "off"
         :autoFocus autofocus?
         :onInput #(toolbelt/height-to-scrollheight! (oget % :target))
         :required true
         :data-dynamic-height true
         :placeholder placeholder
         :on-key-up #(rf/dispatch [:schnaq.question.input/set-current (oget % [:?target :value])])}]
       [:button.btn.btn-outline-dark
        {:class additional-btn-class
         :type "submit" :title (labels :discussion/create-argument-action)}
        [:div.d-flex.flex-row
         [:div.d-none.d-lg-block.me-1 send-button-label]
         [icon :plane "m-auto"]]]])))

(defn- topic-input-area
  "Input form with an option to chose statement type."
  []
  (let [starting-route? @(rf/subscribe [:schnaq.routes/starting?])
        textarea-name (if starting-route? "statement-text" "premise-text")
        pro-con-disabled? @(rf/subscribe [:schnaq.selected/pro-con?])
        statement-type (if starting-route?
                         :statement.type/neutral
                         @(rf/subscribe [:form/statement-type :selected]))
        send-button-label (if starting-route?
                            (labels :statement/ask)
                            (labels :statement/reply))
        placeholder (if starting-route?
                      (labels :statement.ask/placeholder)
                      (labels :statement.reply/placeholder))]
    [:<>
     [textarea-for-statements
      textarea-name
      placeholder
      send-button-label
      statement-type
      true
      false]
     (when-not (or starting-route? pro-con-disabled?)
       [:div.mt-3
        [statement-type-choose-button
         [:form/statement-type :selected]
         [:form/statement-type! :selected]]])]))

(defn input-form
  "Form to collect the user's statements."
  []
  (let [starting-route? @(rf/subscribe [:schnaq.routes/starting?])
        when-starting (fn [e] (.preventDefault e)
                        (rf/dispatch [:discussion.add.statement/starting
                                      (oget e [:currentTarget :elements])]))
        when-deeper-in-discussion (fn [e]
                                    (.preventDefault e)
                                    (logic/submit-new-premise (oget e [:currentTarget :elements])))
        event-to-send (if starting-route?
                        when-starting when-deeper-in-discussion)]
    [:form.my-md-2
     {:on-submit #(event-to-send %)
      :on-key-down #(when (toolbelt/ctrl-press? % 13) (event-to-send %))}
     [topic-input-area]]))

(defn reply-in-statement-input-form
  "Input form inside a statement card. This form is used to directly reply to a statement inside its own card."
  [statement]
  (let [statement-id (:db/id statement)
        form-name (str "answer-to-statement-" statement-id)
        placeholder (labels :statement.reply/placeholder)
        send-button-label (labels :statement/reply)
        statement-type @(rf/subscribe [:form/statement-type statement-id])
        pro-con-disabled? @(rf/subscribe [:schnaq.selected/pro-con?])
        answer-to-statement-event (fn [e]
                                    (.preventDefault e)
                                    (logic/reply-to-statement
                                     statement
                                     statement-type
                                     form-name
                                     (oget e [:currentTarget :elements])))]
    [:form.my-md-2
     {:on-submit #(answer-to-statement-event %)
      :on-key-down #(when (toolbelt/ctrl-press? % 13)
                      (answer-to-statement-event %))}
     [textarea-for-statements
      form-name
      placeholder
      send-button-label
      statement-type
      false
      true]
     (when-not pro-con-disabled?
       [statement-type-choose-button
        [:form/statement-type statement-id]
        [:form/statement-type! statement-id] true])]))

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
