(ns schnaq.interface.views.qa.inputs
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.qa.search :refer [throttled-search] :as search]))

(defn- text-input-for-qanda
  "Input where users can enter their questions for Q&A."
  []
  (let [textarea-name "statement-text"
        attitude-class "highlight-card-neutral"
        submit-fn (fn [e]
                    (jq/prevent-default e)
                    (rf/dispatch [:discussion.add.statement/starting
                                  (oget e [:currentTarget :elements])])
                    (rf/dispatch [:schnaq.qa.new-question/pulse true]))]
    [:form {:on-submit #(submit-fn %)
            :on-key-down #(when (jq/ctrl-press % 13) (submit-fn %))}
     [:div.d-flex.flex-row.discussion-input-content.rounded-1.mb-3
      [:div {:class attitude-class}]
      [:div.w-100.pt-3
       [:div.form-group
        [:textarea.form-control.discussion-text-input-area.form-control-lg
         {:name textarea-name :wrap "soft" :rows 2
          :auto-complete "off" :autoFocus true
          :onInput #(toolbelt/height-to-scrollheight! (oget % :target))
          :required true :data-dynamic-height true
          :placeholder (labels :qanda/add-question)
          :on-key-down #(throttled-search %)}]]]]
     [:div.input-group-append
      [:button.btn.btn-lg.btn-primary.w-100.shadow-sm.mt-5
       {:type "submit" :title (labels :qanda.button/submit)}
       [:div.d-inline-block
        [:div.d-flex.flex-row.justify-content-center
         [:div.mr-3 (labels :qanda.button/submit)]
         [icon :plane "m-auto"]]]]]]))

(defn- ask-question
  "Either display input or read-only warning."
  []
  (let [read-only? @(rf/subscribe [:schnaq.selected/read-only?])]
    [:div.panel-white.p-5.mt-md-5
     [:div.my-2.mx-lg-5.p-md-5
      (if read-only?
        [:h3 (labels :qanda.state/read-only-warning)]
        [text-input-for-qanda])]]))

(defn qanda-content []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])]
    [pages/with-qanda-view-header
     {:page/heading (:discussion/title current-discussion)}
     [:div.container.p-0.px-md-5
      [ask-question]
      [search/results-list]]]))

(defn qanda-view
  "A view that represents the first page of schnaq participation or creation."
  []
  [qanda-content])
