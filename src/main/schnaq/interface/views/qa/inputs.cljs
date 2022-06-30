(ns schnaq.interface.views.qa.inputs
  (:require [com.fulcrologic.guardrails.core :refer [>defn >defn-]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.lexical.editor :as lexical]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.qa.search :refer [throttled-search] :as search]
            [schnaq.interface.views.schnaq.activation :as activation]))

(defn- text-input-for-qanda
  "Input where users can enter their questions for Q&A."
  []
  (let [editor-id "qanda-input"
        editor-content @(rf/subscribe [:editor/content editor-id])
        submit-fn (fn [e] (.preventDefault e)
                    (let [form (oget e [:currentTarget :elements])
                          statement-text (oget form [:statement :value])
                          locked? (boolean (oget form ["?lock-card?" :checked]))]
                      (rf/dispatch [:editor/clear editor-id])
                      (rf/dispatch [:schnaq.qa.new-question/pulse true])
                      (rf/dispatch [:discussion.add.statement/starting statement-text locked?])))]
    [:form {:on-submit #(submit-fn %)
            :on-key-down #(when (toolbelt/ctrl-press? % 13) (submit-fn %))}
     [:label.form-label.h5.mb-3 {:for editor-id} (labels :qanda/add-question-label)]
     [:div.d-flex.flex-row.qanda-input-content.rounded-1
      [:div.highlight-card-neutral]
      [:input {:type :hidden
               :name "statement"
               :value (or editor-content "")}]
      [lexical/editor {:id editor-id
                       :focus? true ;; WIP
                       :on-text-change throttled-search
                       :placeholder (labels :statement.new/placeholder)}
       {:className "flex-grow-1"}]]
     [:button.btn.btn-lg.btn-secondary.w-100.shadow-sm.mt-3.rounded-1
      {:type "submit"
       :disabled (empty? editor-content)
       :title (labels :qanda.button/submit)
       :on-click #(matomo/track-event "Active User" "Action" "Submit Question")}
      (labels :qanda.button/submit)
      [icon :plane "m-auto ms-2"]]]))

(>defn- ask-question
  "Either display input or read-only warning."
  [background-schema]
  [:background/schema :ret :re-frame/component]
  (let [read-only? @(rf/subscribe [:schnaq.selected/read-only?])]
    [:div.mx-3
     [:div.mx-md-5.px-md-5.py-3
      (when (= background-schema :dark) {:class "text-white"})
      (if read-only?
        [:h3 (labels :qanda.state/read-only-warning)]
        [text-input-for-qanda])]]))

(>defn question-field-and-search-results
  "Combine input form and results list for uniform representation in ask-view
  and in other usages, e.g. the startpage.
  Backgroundtype can either be :light or :dark"
  [background-schema]
  [:background/schema :ret :re-frame/component]
  [:<>
   [:div.container.p-0.px-md-5
    [ask-question background-schema]]
   [:div.container-fluid.p-0.px-md-5
    [search/results-list background-schema]]])

(defn- qanda-content []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])]
    [pages/with-qanda-header
     {:page/heading (:discussion/title current-discussion)
      :page/classes "layered-wave-background h-100 d-flex flex-column min-height-85"}
     [:<>
      [:div.container
       [activation/activation-event-view]]
      [question-field-and-search-results :dark]]]))

(defn qanda-view
  "A view that represents the first page of schnaq participation or creation."
  []
  [qanda-content])
