(ns meetly.meeting.interface.views
  (:require [reagent.dom]
            [re-frame.core :as rf]
            [clojure.string :as str]
            [oops.core :refer [oget]]))

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

(defn create-meeting-form []
  [:div.create-meeting-form
   [:form {:on-submit (fn [e] (.preventDefault e)
                        (rf/dispatch [:new-meeting (oget e [:target :elements "0" :value])]))}
    [:label {:for "title"} "Title:"]
    [:input#title {:type "text" :name "title"}]
    [:input {:type "submit" :value "Create Meetly"}]]])

(defn meetings-list []
  [:div.meetings-list
   [:h3 "Meetings"]
   (let [meetings @(rf/subscribe [:meetings])]
     (for [meeting meetings]
       [:p meeting]))])

(defn ui
  []
  [:div
   [:h1 "Meetly Central"]
   [:hr]
   [:h2 "Meeting controls"]
   [create-meeting-form]
   [:hr]
   [meetings-list]
   [:hr]
   [clock]
   [color-input]])

