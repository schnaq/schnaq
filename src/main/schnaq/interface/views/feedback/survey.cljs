(ns schnaq.interface.views.feedback.survey
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]))

(def ^:private survey-iframe
  "Include Google Form."
  [:iframe.w-100
   {:src "https://docs.google.com/forms/d/e/1FAIpQLSf1DlY7W5GQ28ECSa3BpPOwnC5IOtVnyz62BS8yTRjiu6HvQw/viewform?embedded=true"
    :width "640" :height "1500"
    :frame-border "0" :margin-height "0" :margin-width "0"}
   (labels :feedbacks.survey/loading)])

(defn view
  "Main view composing the survey."
  []
  (let [show-survey? @(rf/subscribe [:survey/show?])
        id "show-survey-checkbox"]
    [:<>
     [:p (labels :feedbacks.survey/primer)]
     [:div.form-check.pb-3
      [:input {:id id
               :class "form-check-input"
               :type "checkbox"
               :checked (or show-survey? false)
               :on-change #(rf/dispatch [:survey/show! (oget % [:target :checked])])}]
      [:label {:class "form-check-label"
               :for id}
       (labels :feedbacks.survey/checkbox)]]
     (when show-survey?
       survey-iframe)]))

(rf/reg-event-db
  :survey/show!
  (fn [db [_ bool]]
    (assoc-in db [:survey :show?] bool)))

(rf/reg-sub
  :survey/show?
  (fn [db _]
    (get-in db [:survey :show?])))