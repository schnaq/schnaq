(ns meetly.meeting.interface.analytics.core
  (:require [meetly.meeting.interface.views.base :as base]
            [meetly.meeting.interface.text.display-data :refer [labels]]
            [meetly.meeting.interface.config :refer [config]]
            [re-frame.core :as rf]
            [ajax.core :as ajax]))

(defn analytics-dashboard-view
  "The dashboard displaying all analytics."
  []
  [:div
   [base/nav-header]
   (let [meetings-num @(rf/subscribe [:analytics/number-of-meetings-overall])]
     [:div.container.px-5.py-3
      [:div.card-deck
       [:div.card
        [:div.card-body
         [:h5.card-title (labels :analytics/overall-meetings)]
         [:p.card-text.display-1 meetings-num]
         [:p.card-text [:small.text-muted "Last updated ..."]]]]
       [:div.card
        [:div.card-body
         [:h5.card-title "Card title"]
         [:p.card-text "This card has supporting text below as a natural lead-in to additional content."]
         [:p.card-text [:small.text-muted "Last updated ..."]]]]
       [:div.card
        [:div.card-body
         [:h5.card-title "Card title"]
         [:p.card-text "This is a wider card with supporting text below as a natural lead-in to additional content. This card has even longer content than the first to show that equal height action."]
         [:p.card-text [:small.text-muted "Last updated ..."]]]]]])])



;; #### Events ####

(rf/reg-event-fx
  :analytics/load-dashboard
  (fn [_ _]
    {:dispatch-n [[:analytics/load-meeting-num]]}))

(rf/reg-event-fx
  :analytics/load-meeting-num
  (fn [_ _]
    {:http-xhrio {:method :post
                  :uri (str (:rest-backend config) "/analytics/meetings")
                  :format (ajax/transit-request-format)
                  :response-format (ajax/transit-response-format)
                  :on-success [:analytics/meeting-num-loaded]
                  :on-failure [:ajax-failure]}}))

(rf/reg-event-db
  :analytics/meeting-num-loaded
  (fn [db [_ {:keys [meetings-num]}]]
    (assoc-in db [:analytics :meetings-num :overall] meetings-num)))

;; #### Subs ####

(rf/reg-sub
  :analytics/number-of-meetings-overall
  (fn [db _]
    (get-in db [:analytics :meetings-num :overall])))