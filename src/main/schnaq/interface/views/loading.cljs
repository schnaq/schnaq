(ns schnaq.interface.views.loading
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.common :as common]))

(defn- spinner-icon
  "Display a spinner icon."
  []
  [:div.spinner-border.text-primary {:role "loading-status"}
   [:span.sr-only "Loading..."]])

(defn spinner
  "Place a spinner on top of a view.
   Activate spinner with `(rf/dispatch [:spinner/active! true])`
   Deactivate spinner with `(rf/dispatch [:spinner/active! false])`"
  []
  (reagent/create-class
    {:display-name "Spinner View"
     :reagent-render
     (fn [_this]
       (let [spinner-is-loading? @(rf/subscribe [:spinner/active?])]
         (when spinner-is-loading?
           [:div.spinner-position
            [spinner-icon]])))
     :component-will-unmount
     (fn [_this]
       (rf/dispatch [:spinner/active! false]))}))

(defn loading-placeholder
  "Placeholder to give feedback to user, that data is currently on its way."
  []
  [common/delayed-fade-in
   [:section.alert.alert-primary.text-center.pt-4
    [spinner-icon]
    [:p.lead.pt-3 (labels :loading.placeholder/lead)]
    [common/delayed-fade-in
     [:p.text-secondary
      "🤔 "
      (labels :loading.placeholder/takes-too-long)]
     5000]]])


;; -----------------------------------------------------------------------------

(rf/reg-sub
  :spinner/active?
  (fn [db _]
    (get-in db [:spinner :active?] false)))

(rf/reg-event-db
  :spinner/active!
  (fn [db [_ toggle]]
    (assoc-in db [:spinner :active?] toggle)))