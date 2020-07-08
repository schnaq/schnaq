(ns meetly.meeting.interface.subs
  (:require [re-frame.core :as rf]))

;; -- Domino 4 - Query  -------------------------------------------------------

(rf/reg-sub
  :time
  (fn [db _]                                                ;; db is current app state. 2nd unused param is query vector
    (:time db)))                                            ;; return a query computation over the application state

(rf/reg-sub
  :time-color
  (fn [db _]
    (:time-color db)))
