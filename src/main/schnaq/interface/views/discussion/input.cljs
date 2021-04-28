(ns schnaq.interface.views.discussion.input
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [fa labels]]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.logic :as logic]))

(defn- argument-type-button
  "Button to select the attitude of an argument. Current attitude is subscribed via get-subscription.
  On-Click triggers the set-subscription."
  [id button-type label tooltip get-subscription set-subscription]
  (let [argument-type @(rf/subscribe [get-subscription])
        checked? (= button-type argument-type)]
    [:label.btn.btn-outline-primary.rounded-4
     (when checked? {:class "active"})
     [:input {:id (str "argument-type-button-" id) :type "radio" :name "options" :autoComplete "off"
              :defaultChecked checked?
              :title (labels tooltip)
              :on-click (fn [e] (jq/prevent-default e)
                          (rf/dispatch [set-subscription button-type]))}]
     (labels label)]))

(defn argument-type-choose-button
  "Button group to differentiate between the argument types. The button with a matching get-subscription will be checked.
  Clicking a button will dispatch the set-subscription with the button-type as argument."
  [get-subscription set-subscription]
  [:div.btn-group.btn-group-toggle {:data-toggle "buttons"}
   [argument-type-button "support" :argument.type/support
    :discussion.add.button/support :discussion/add-premise-against
    get-subscription set-subscription]
   [argument-type-button "neutral" :argument.type/neutral
    :discussion.add.button/neutral :discussion/add-premise-neutral
    get-subscription set-subscription]
   [argument-type-button "attack" :argument.type/attack
    :discussion.add.button/attack :discussion/add-premise-supporting
    get-subscription set-subscription]])

(defn- textarea-for-statements
  "Input, where users provide (starting) conclusions."
  [textarea-name]
  (let [pro-con-disabled? @(rf/subscribe [:schnaq.selected/pro-con?])
        argument-type @(rf/subscribe [:form/argument-type])
        current-route-name @(rf/subscribe [:navigation/current-route-name])
        current-color (case argument-type
                        :argument.type/support "text-primary"
                        :argument.type/attack "text-secondary"
                        :argument.type/neutral "text-dark")]
    [:div.input-group
     [:textarea.form-control.discussion-text-input-area
      {:name textarea-name :wrap "soft" :rows 1
       :auto-complete "off"
       :onInput #(toolbelt/height-to-scrollheight! (oget % :target))
       ;; first reset input then set height +1px in order to prevent scrolling
       :required true
       :data-dynamic-height true
       :placeholder (labels :discussion/add-argument-conclusion-placeholder)}]
     [:div.d-flex.justify-content-between.mt-1.justify-content-md-end.mt-md-0.w-100
      (when-not (or (= :routes.schnaq/start current-route-name) pro-con-disabled?)
        [:div.input-group-prepend
         [argument-type-choose-button :form/argument-type :form/argument-type!]])
      [:div.input-group-append
       [:button.btn
        {:type "submit" :class current-color
         :title (labels :discussion/create-argument-action)}
        [:i {:class (str "m-auto fas " (fa :plane))}]]]]]))

(defn input-form
  "Form to collect the user's statements."
  [textarea-name]
  (let [current-route-name @(rf/subscribe [:navigation/current-route-name])
        when-starting (fn [e] (jq/prevent-default e)
                        (rf/dispatch [:discussion.add.statement/starting
                                      (oget e [:currentTarget :elements])]))
        when-deeper-in-discussion (fn [e]
                                    (jq/prevent-default e)
                                    (logic/submit-new-premise (oget e [:currentTarget :elements])))
        event-to-send (if (= :routes.schnaq/start current-route-name)
                        when-starting when-deeper-in-discussion)]
    [:form.my-2
     {:on-submit #(event-to-send %)
      :on-key-down #(when (jq/ctrl-press % 13)
                      (event-to-send %))}
     [:div.discussion-input-container
      [textarea-for-statements textarea-name]]]))

(rf/reg-event-db
  :form/argument-type!
  (fn [db [_ argument-type]]
    (assoc-in db [:form :argument/type] argument-type)))

(rf/reg-sub
  :form/argument-type
  (fn [db]
    (get-in db [:form :argument/type] :argument.type/neutral)))