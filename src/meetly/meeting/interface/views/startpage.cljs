(ns meetly.meeting.interface.views.startpage
  (:require [re-frame.core :as rf]))

(defn startpage-view
  "A view that represents the first page of meetly participation or creation."
  []
  [:div
   [:h2 "Meetly - Save time on meetings"]
   [:div {:style {:min-height "300px"
                  :min-width "800px"
                  :background-color "green"}}
    [:p "Some info and stuff"]]
   [:br]
   [:input
    {:on-click #(rf/dispatch [:navigate :routes/meetings.create])
     :type "button"
     :value "Create Meetly"
     :style {:margin-bottom "1em"}}]])