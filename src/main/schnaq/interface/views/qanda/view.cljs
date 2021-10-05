(ns schnaq.interface.views.qanda.view
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [fa]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.pages :as pages]))

(defn- textarea-for-qanda
  "Input, where users can enter their questions for Q&A."
  []
  (let [textarea-name "statement-text"
        attitude-class "highlight-card-neutral"]
    [:<>
     [:div.d-flex.flex-row.discussion-input-content.rounded-1.mb-3
      [:div {:class attitude-class}]
      [:div.w-100.pt-3
       [:div.form-group
        [:textarea.form-control.discussion-text-input-area
         {:name textarea-name :wrap "soft" :rows 2
          :auto-complete "off"
          :autoFocus true
          :onInput #(toolbelt/height-to-scrollheight! (oget % :target))
          :required true
          :data-dynamic-height true
          :placeholder (labels :qanda/add-question)}]]]]
     [:div.input-group-append
      [:button.btn.btn-lg.btn-primary.w-100.shadow-sm.mt-3
       {:type "submit" :title (labels :discussion/create-argument-action)}
       [:div.d-inline-block
        [:div.d-flex.flex-row.justify-content-center
         [:div.mr-3 (labels :statement.edit.button/submit)]
         [:i {:class (str "m-auto fas " (fa :plane))}]]]]]]))

(defn- qanda-input-form
  "Form to collect the user's statements."
  []
  (let [when-starting (fn [e] (jq/prevent-default e)
                        (rf/dispatch [:discussion.add.statement/starting
                                      (oget e [:currentTarget :elements])]))]
    [:form.my-2.mx-lg-5
     {:on-submit #(when-starting %)
      :on-key-down #(when (jq/ctrl-press % 13)
                      (when-starting %))}
     [textarea-for-qanda]]))

(defn ask-question []
  [:div.panel-white.p-5
   [qanda-input-form]])

(defn qanda-content []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])]
    [pages/with-discussion-header
     {:page/heading (:discussion/title current-discussion)}
     [:div.container.p-0
      [ask-question]]]))

(defn qanda-view
  "A view that represents the first page of schnaq participation or creation."
  []
  [qanda-content])
