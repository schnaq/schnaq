(ns meetly.meeting.interface.events
  (:require [re-frame.core :as rf]
            [meetly.meeting.interface.db :as meetly-db]))

;; -- Domino 2 - Event Handlers -----------------------------------------------
(rf/reg-event-fx                                            ;; part of the re-frame API
  :initialise-db                                            ;; event id being handled
  ;; the event handler (function) being registered
  (fn [_ _]                                                 ;; take 2 values from coeffects. Ignore event vector itself.
    {:db meetly-db/default-db}))

(rf/reg-event-db
  :init-from-backend
  (fn [db [_ all-meetings]]
    (assoc db :meetings all-meetings)))


(rf/reg-event-db                                            ;; usage:  (dispatch [:time-color-change 34562])
  :time-color-change                                        ;; dispatched when the user enters a new colour into the UI text field
  (fn [db [_ new-color-value]]                              ;; -db event handlers given 2 parameters:  current application state and event (a vector)
    (assoc db :time-color new-color-value)))                ;; compute and return the new application state


(rf/reg-event-db                                            ;; usage:  (dispatch [:timer a-js-Date])
  :timer                                                    ;; every second an event of this kind will be dispatched
  (fn [db [_ new-time]]                                     ;; note how the 2nd parameter is destructured to obtain the data value
    (assoc db :time new-time)))                             ;; compute and return the new application state

(rf/reg-event-db
  :new-meeting
  (fn [db [_ meeting-title]]
    (update db :meetings conj meeting-title)))


