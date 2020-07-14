(ns meetly.meeting.interface.views.clock
  (:require [clojure.string :as str]
            [re-frame.core :as rf]))

;; #### Views ####

(defn clock
  []
  [:div.example-clock
   {:style {:color @(rf/subscribe [:time-color])}}
   (-> @(rf/subscribe [:time])
       .toTimeString
       (str/split " ")
       first)])

(defn color-input
  []
  [:div.color-input
   "Time color: "
   [:input {:type "text"
            :value @(rf/subscribe [:time-color])
            :on-change #(rf/dispatch [:time-color-change (-> % .-target .-value)])}]])

(defn re-frame-example-view []
  [:div
   [clock]
   [color-input]])

;; #### Events ####

(rf/reg-sub
  :time
  (fn [db _]
    (:time db)))

(rf/reg-sub
  :time-color
  (fn [db _]
    (:time-color db)))

;; #### Subs ####

(rf/reg-event-db
  :time-color-change
  (fn [db [_ new-color-value]]
    (assoc db :time-color new-color-value)))


(rf/reg-event-db
  :timer
  (fn [db [_ new-time]]
    (assoc db :time new-time)))
