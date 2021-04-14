(ns schnaq.interface.views.discussion.input
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [fa labels]]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.logic :as logic]))

(defn- argument-type-button
  "Button to select current attitude."
  [id button-type tooltip]
  (let [argument-type @(rf/subscribe [:form/argument-type])
        checked? (= button-type argument-type)]
    [:input {:id id :type "radio" :name "options" :autoComplete "off"
             :defaultChecked checked?
             :title (labels tooltip)
             :on-click (fn [e] (jq/prevent-default e)
                         (rf/dispatch [:form/argument-type! button-type]))}]))

(defn- argument-type-choose-button
  "Switch to differentiate between the argument types."
  []
  (let [argument-type @(rf/subscribe [:form/argument-type])
        active-class (fn [argument-type current-button] {:class (when (= argument-type current-button) "active")})
        set-active (partial active-class argument-type)]
    [:div.btn-group.btn-group-toggle {:data-toggle "buttons"}
     [:label.btn.btn-outline-primary.rounded-4
      (set-active :argument.type/support)
      [argument-type-button "support" :argument.type/support :discussion/add-premise-against]
      (labels :discussion.add.button/support)]
     [:label.btn.btn-outline-dark
      (set-active :argument.type/neutral)
      [argument-type-button "neutral" :argument.type/neutral :discussion/add-premise-neutral]
      (labels :discussion.add.button/neutral)]
     [:label.btn.btn-outline-secondary.rounded-4
      (set-active :argument.type/attack)
      [argument-type-button "attack" :argument.type/attack :discussion/add-premise-supporting]
      (labels :discussion.add.button/attack)]]))

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
     (when-not (or (= :routes.schnaq/start current-route-name) pro-con-disabled?)
       [:div.input-group-prepend
        [argument-type-choose-button]])
     [:textarea.form-control.discussion-text-input-area
      {:name textarea-name :wrap "soft" :rows 1
       :auto-complete "off"
       :onInput #(toolbelt/height-to-scrollheight! (oget % :target))
       ;; first reset input then set height +1px in order to prevent scrolling
       :required true
       :data-dynamic-height true
       :placeholder (labels :discussion/add-argument-conclusion-placeholder)}]
     [:div.input-group-append
      [:button.btn
       {:type "submit" :class current-color
        :title (labels :discussion/create-argument-action)}
       [:i {:class (str "m-auto fas " (fa :plane))}]]]]))

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