(ns meetly.meeting.interface.events
  (:require [re-frame.core :as rf]))

;; -- Domino 2 - Event Handlers -----------------------------------------------

(rf/reg-event-db                                            ;; sets up initial application state
  :initialize                                               ;; usage:  (dispatch [:initialize])
  (fn [_ _]                                                 ;; the two parameters are not important here, so use _
    {:time (js/Date.)                                       ;; What it returns becomes the new application state
     :time-color "#f88"}))                                  ;; so the application state will initially be a map with two keys


(rf/reg-event-db                                            ;; usage:  (dispatch [:time-color-change 34562])
  :time-color-change                                        ;; dispatched when the user enters a new colour into the UI text field
  (fn [db [_ new-color-value]]                              ;; -db event handlers given 2 parameters:  current application state and event (a vector)
    (assoc db :time-color new-color-value)))                ;; compute and return the new application state


(rf/reg-event-db                                            ;; usage:  (dispatch [:timer a-js-Date])
  :timer                                                    ;; every second an event of this kind will be dispatched
  (fn [db [_ new-time]]                                     ;; note how the 2nd parameter is destructured to obtain the data value
    (assoc db :time new-time)))                             ;; compute and return the new application state




