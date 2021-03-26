(ns schnaq.interface.navigation
  (:require [re-frame.core :as rf]
            [reitit.frontend.controllers :as reitit-front-controllers]
            [reitit.frontend.easy :as reitit-front-easy]))

(rf/reg-sub
  :navigation/current-route
  (fn [db]
    (:current-route db)))

(rf/reg-sub
  :navigation/current-route-name
  (fn [db]
    (get-in db [:current-route :data :name])))

(rf/reg-event-fx
  :navigation/navigate
  (fn [_cofx [_ & route]]
    {:fx [[:navigation/navigate! route]]}))

(rf/reg-fx
  :navigation/navigate!
  (fn [route]
    (apply reitit-front-easy/push-state route)))

(rf/reg-event-db
  :navigation/navigated
  (fn [db [_ new-match]]
    (let [old-match (:current-route db)
          controllers (reitit-front-controllers/apply-controllers (:controllers old-match) new-match)]
      (assoc db :current-route (assoc new-match :controllers controllers)))))