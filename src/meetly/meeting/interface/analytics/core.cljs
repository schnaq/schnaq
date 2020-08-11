(ns meetly.meeting.interface.analytics.core
  (:require [meetly.meeting.interface.views.base :as base]
            [meetly.meeting.interface.text.display-data :refer [labels]]
            [meetly.meeting.interface.config :refer [config]]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [ajax.core :as ajax]))

(defn- analytics-card
  "A single card containing a metric and a title."
  [title metric]
  [:div.card
   [:div.card-body
    [:h5.card-title title]
    [:p.card-text.display-1 metric]
    [:p.card-text [:small.text-muted "Last updated ..."]]]])

(defn analytics-dashboard-view
  "The dashboard displaying all analytics."
  []
  [:div
   [base/nav-header]
   (let [meetings-num @(rf/subscribe [:analytics/number-of-meetings-overall])
         usernames-num @(rf/subscribe [:analytics/number-of-usernames-overall])
         average-agendas @(rf/subscribe [:analytics/number-of-average-agendas])]
     (println average-agendas " Agendas")
     [:div.container.px-5.py-3
      [:div.card-deck
       [analytics-card (labels :analytics/overall-meetings) meetings-num]
       [analytics-card (labels :analytics/user-numbers) usernames-num]
       [analytics-card (labels :analytics/average-agendas-title) average-agendas]]])])

;; #### Events ####

(rf/reg-event-fx
  :analytics/load-dashboard
  (fn [_ _]
    {:dispatch-n [[:analytics/load-meeting-num]
                  [:analytics/load-usernames-num]
                  [:analytics/load-average-number-of-agendas]]}))

(rf/reg-event-fx
  :analytics/load-meeting-num
  (fn [{:keys [db]} _]
    {:http-xhrio {:method :post
                  :uri (str (:rest-backend config) "/analytics/meetings")
                  :format (ajax/transit-request-format)
                  :params {:password (-> db :admin :password)}
                  :response-format (ajax/transit-response-format)
                  :on-success [:analytics/meeting-num-loaded]
                  :on-failure [:ajax-failure]}}))

(rf/reg-event-fx
  :analytics/load-usernames-num
  (fn [{:keys [db]} _]
    {:http-xhrio {:method :post
                  :uri (str (:rest-backend config) "/analytics/usernames")
                  :format (ajax/transit-request-format)
                  :params {:password (-> db :admin :password)}
                  :response-format (ajax/transit-response-format)
                  :on-success [:analytics/usernames-num-loaded]
                  :on-failure [:ajax-failure]}}))

(rf/reg-event-fx
  :analytics/load-average-number-of-agendas
  (fn [{:keys [db]} _]
    (println "In load agendas")
    {:http-xhrio {:method :post
                  :uri (str (:rest-backend config) "/analytics/agendas-per-meeting")
                  :format (ajax/transit-request-format)
                  :params {:password (-> db :admin :password)}
                  :response-format (ajax/transit-response-format)
                  :on-success [:analytics/agendas-per-meeting-loaded]
                  :on-failure [:ajax-failure]}}))

(rf/reg-event-db
  :analytics/meeting-num-loaded
  (fn [db [_ {:keys [meetings-num]}]]
    (assoc-in db [:analytics :meetings-num :overall] meetings-num)))

(rf/reg-event-db
  :analytics/usernames-num-loaded
  (fn [db [_ {:keys [usernames-num]}]]
    (assoc-in db [:analytics :usernames-num :overall] usernames-num)))

(rf/reg-event-db
  :analytics/agendas-per-meeting-loaded
  (fn [db [_ {:keys [average-agendas]}]]
    (println "Writing " average-agendas)
    (assoc-in db [:analytics :agendas :average-per-meeting] (gstring/format "%.2f" average-agendas))))

;; #### Subs ####

(rf/reg-sub
  :analytics/number-of-meetings-overall
  (fn [db _]
    (get-in db [:analytics :meetings-num :overall])))

(rf/reg-sub
  :analytics/number-of-usernames-overall
  (fn [db _]
    (get-in db [:analytics :usernames-num :overall])))

(rf/reg-sub
  :analytics/number-of-average-agendas
  (fn [db _]
    (get-in db [:analytics :agendas :average-per-meeting])))