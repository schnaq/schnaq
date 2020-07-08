(ns meetly.meeting.interface.views
  (:require [reagent.dom]
            [re-frame.core :as rf]
            [clojure.string :as str]))

;; -- Domino 5 - View Functions ----------------------------------------------

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
            :on-change #(rf/dispatch [:time-color-change (-> % .-target .-value)])}]]) ;; <---

(defn ui
  []
  [:div
   [:h1 "Meetly Central"]
   [:hr]
   [:h2 "Meeting controls"]
   [clock]
   [color-input]])

