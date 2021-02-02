(ns schnaq.interface.views.discussion.input
  (:require [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [fa labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.logic :as logic]))

(>defn- button-styling
  "Dispatch button styling by argument-type."
  [argument-type]
  [keyword? :ret vector?]
  (if (= :argument.type/support argument-type)
    ["btn-outline-primary" :argument.type/attack :discussion.add.button/support
     :discussion/add-premise-supporting true]
    ["btn-outline-secondary" :argument.type/support :discussion.add.button/attack
     :discussion/add-premise-against false]))

(defn- argument-type-choose-button
  "Switch to differentiate between the argument types."
  []
  (let [argument-type @(rf/subscribe [:form/argument-type])
        switch-key (gstring/format "control-input-%s" (str (random-uuid)))
        [outline next-type button-label tooltip switch-state] (button-styling argument-type)]
    [:button.btn
     {:type "button" :class outline :title (labels tooltip)
      :on-click #(rf/dispatch [:form/argument-type! next-type])}
     [:div.custom-control.custom-switch
      [:input.custom-control-input {:id switch-key :type "checkbox"
                                    :name "premise-choice" :value argument-type
                                    :default-checked switch-state}]
      [:label.custom-control-label {:for switch-key} (labels button-label)]]]))

(defn- textarea-for-statements
  "Input, where users provide (starting) conclusions."
  [textarea-name]
  (let [current-route-name @(rf/subscribe [:navigation/current-route-name])]
    [:div.input-group
     (when-not (= :routes.schnaq/start current-route-name)
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
      [:button.btn.text-primary
       {:type "submit"
        :title (labels :discussion/create-argument-action)}
       [:i {:class (str "m-auto fas " (fa :plane))}]]]]))

(defn input-form
  "Form to collect the user's statements."
  [textarea-name]
  (let [current-route-name @(rf/subscribe [:navigation/current-route-name])
        when-starting (fn [e] (js-wrap/prevent-default e)
                        (rf/dispatch [:discussion.add.statement/starting
                                      (oget e [:target :elements])]))
        when-deeper-in-discussion (fn [e]
                                    (js-wrap/prevent-default e)
                                    (logic/submit-new-premise (oget e [:target :elements])))]
    [:form.my-2
     {:on-submit (if (= :routes.schnaq/start current-route-name)
                   when-starting when-deeper-in-discussion)}
     [:div.discussion-input-container
      [textarea-for-statements textarea-name]]]))

(rf/reg-event-db
  :form/argument-type!
  (fn [db [_ argument-type]]
    (assoc-in db [:form :argument/type] argument-type)))

(rf/reg-sub
  :form/argument-type
  (fn [db]
    (get-in db [:form :argument/type] :argument.type/support)))