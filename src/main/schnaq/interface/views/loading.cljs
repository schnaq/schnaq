(ns schnaq.interface.views.loading
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.translations :refer [labels]]))

(defn spinner-icon
  "Display a spinner icon."
  []
  [:div.spinner-border.text-primary {:role "loading-status"}
   [:span.visually-hidden "Loading..."]])

(defn loading-placeholder
  "Placeholder to give feedback to user, that data is currently on its way."
  []
  [motion/fade-in-and-out
   [:section.text-center.pt-4
    [spinner-icon]
    [:p.lead.pt-3 (labels :loading.placeholder/lead)]
    [motion/fade-in-and-out
     [:p.text-info
      "🤔 "
      (labels :loading.placeholder/takes-too-long)]
     5]]])

(defn loading-card
  "Show a card when loading takes longer time."
  []
  [:div.statement-column
   [motion/fade-in-and-out
    [:div.statement-card
     [loading-placeholder]]
    1]])

;; -----------------------------------------------------------------------------

(rf/reg-event-db
 :loading/statements
 (fn [db [_ toggle]]
   (if toggle
     (assoc-in db [:loading :statements?] true)
     (update db :loading dissoc :statements?))))

(rf/reg-sub
 :loading/statements?
 (fn [db]
   (get-in db [:loading :statements?])))
