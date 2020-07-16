(ns meetly.meeting.interface.subs
  (:require [re-frame.core :as rf]))

;; This sub helps change the view to the correct route
(rf/reg-sub
  :current-route
  (fn [db]
    (:current-route db)))

(rf/reg-sub
  :error-occurred
  (fn [db]
    (:error db)))

(rf/reg-sub
  :username
  (fn [db]
    (get-in db [:user :name] "Anonymous")))

(rf/reg-sub
  :show-username-input?
  (fn [db]
    (get-in db [:controls :username-input :show?] true)))