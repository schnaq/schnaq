(ns schnaq.interface.views.loading
  (:require [schnaq.interface.components.motion :as motion]
            [schnaq.interface.translations :refer [labels]]))

(defn spinner-icon
  "Display a spinner icon."
  []
  [:div.spinner-border.text-primary {:role "loading-status"}
   [:span.sr-only "Loading..."]])

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
