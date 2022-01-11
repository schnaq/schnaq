(ns schnaq.interface.views.schnaq.survey
  (:require [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.inputs :as inputs]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as jsw]))

(defn- survey-option
  "Returns a single option component. Can contain a button for removal of said component."
  ([placeholder rank]
   [inputs/text placeholder (str "survey-option-" rank)]))

;; TODO ongoing survey Karte

(defn- survey-form
  "Input form to create a survey with multiple options."
  []
  (let [option-count (reagent/atom 2)]
    (fn []
      [:form.pt-2
       {:on-submit (fn [event]
                     (jsw/prevent-default event)
                     (rf/dispatch [:schnaq.survey/create-new
                                   (oget event [:target :elements])
                                   @option-count]))}
       [:div.form-group
        [:label {:for :survey-topic} (labels :schnaq.survey.create/topic-label)]
        [inputs/text (labels :schnaq.survey.create/placeholder) :survey-topic]
        [:small.form-text.text-muted (labels :schnaq.survey.create/hint)]]
       [:div.form-group
        [:label (labels :schnaq.survey.create/options-label)]
        [survey-option "Pyrrhus" 1]
        [survey-option "Surus" 2]
        (for [rank (range 3 (inc @option-count))]
          (with-meta
            [survey-option (str (labels :schnaq.survey.create/options-placeholder) " " rank) rank]
            {:key (str "survey-option-key-" rank)}))]
       [:div.text-center.mb-3
        [:button.btn.btn-dark.mr-2
         {:type :button
          :on-click #(swap! option-count inc)}
         [icon :plus] " " (labels :schnaq.survey.create/add-button)]
        (when (> @option-count 2)
          [:button.btn.btn-dark
           {:type :button
            :on-click #(swap! option-count dec)}
           [icon :minus] " " (labels :schnaq.survey.create/remove-button)])]
       [:div.form-check.form-check-inline
        [:input#radio-single-choice.form-check-input
         {:type "radio"
          :name :radio-type-choice
          :value :single
          :defaultChecked true}]
        [:label.form-check-label
         {:for :radio-single-choice} (labels :schnaq.survey.create/single-choice-label)]]
       [:div.form-check.form-check-inline
        [:input#radio-multiple-choice.form-check-input
         {:type "radio"
          :name :radio-type-choice
          :value :multiple}]
        [:label.form-check-label
         {:for :radio-multiple-choice} (labels :schnaq.survey.create/multiple-choice-label)]]
       [:div.text-center.pt-2
        [:button.btn.btn-primary.w-75 {:type "submit"} (labels :schnaq.survey.create/submit-button)]]])))

(rf/reg-event-fx
 :schnaq.survey/create-new
 (fn [{:keys [db]} [_ form-elements number-of-options]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         params {:title (oget form-elements :survey-topic :value)
                 :survey-type (case (oget form-elements :radio-type-choice :value)
                                "single" :survey.type/single-choice
                                "multiple" :survey.type/multiple-choice)
                 :options (mapv #(oget+ form-elements (str "survey-option-" %) :value)
                                (range 1 (inc number-of-options)))
                 :share-hash share-hash
                 :edit-hash (get-in db [:schnaqs :admin-access share-hash])}]
     {:fx [(http/xhrio-request db :post "/survey"
                               [:schnaq.survey.create-new/success]
                               params)]})))

(rf/reg-event-db
 :schnaq.survey.create-new/success
 ;; TODO write the survey to the  appropriate place, once it is shown
 (fn [db [_ response]]
   (println (:new-survey response))
   db))
