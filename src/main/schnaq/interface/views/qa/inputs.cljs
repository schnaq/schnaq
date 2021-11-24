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
  (let [input-id "qanda-input"
        current-route @(rf/subscribe [:navigation/current-route-name])
        submit-fn (fn [e] (jq/prevent-default e)
                    (rf/dispatch [:discussion.add.statement/starting
                                  (oget e [:currentTarget :elements])])
                    (rf/dispatch [:schnaq.qa.new-question/pulse true]))]
    [:form {:on-submit #(submit-fn %)
            :on-key-down #(when (jq/ctrl-press % 13) (submit-fn %))}
     [:label.h5.mb-3 {:for input-id} (labels :qanda/add-question-label)]
     [:div.d-flex.flex-row.qanda-input-content.rounded-1
      [:div {:class "highlight-card-neutral"}]
      [:div.form-group.w-100.mb-0
       [:textarea.form-control.discussion-text-input-area.m-1
        {:name "statement-text" :wrap "soft" :rows 1 :id input-id
         :auto-complete "off" :autoFocus (= :routes.schnaq/qanda current-route)
         :onInput #(toolbelt/height-to-scrollheight! (oget % :target))
         :required true :data-dynamic-height true
         :placeholder (labels :qanda/add-question)
         :on-key-up #(throttled-search %)}]]]
     [:button.btn.btn-lg.btn-secondary.w-100.shadow-sm.mt-3.rounded-1
      {:type "submit" :title (labels :qanda.button/submit)}
      [:div.d-inline-block
       [:div.d-flex.flex-row.justify-content-center
        [:div.mr-3 (labels :qanda.button/submit)]
        [icon :plane "m-auto"]]]]]))

(defn- ask-question
  "Either display input or read-only warning."
  [background-type]
  (let [read-only? @(rf/subscribe [:schnaq.selected/read-only?])]
    [:div.mx-3
     [:div.mx-md-5.px-md-5.py-3
      (when (= background-type :dark)
        {:class "text-white"})
      (if read-only?
        [:h3 (labels :qanda.state/read-only-warning)]
        [text-input-for-qanda])]]))

(defn question-field-and-search-results
  "Combine input form and results list for uniform representation in ask-view
  and in other usages, e.g. the startpage.
  Backgroundtyoe can either be :light or :dark"
  [background-type]
  [:<>
   [ask-question background-type]
   [search/results-list background-type]])

(defn- qanda-content []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])]
    [pages/with-qanda-header
     {:page/heading (:discussion/title current-discussion)
      :page/classes "base-wrapper layered-wave-background h-100 d-flex flex-column"}
     [:<>
      [:div.container.p-0.px-md-5
       [question-field-and-search-results :dark]]
      [:div.wave-bottom-typography.d-flex.align-self-end.mt-auto]]]))

(defn qanda-view
  "A view that represents the first page of schnaq participation or creation."
  []
  [qanda-content])
