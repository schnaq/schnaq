(ns meetly.meeting.interface.effects
  (:require [re-frame.core :as rf]
            [meetly.meeting.interface.localstorage :as ls]))

(rf/reg-fx
  :write-localstorage
  (fn [[key value]]
    (ls/set-item! key value)))