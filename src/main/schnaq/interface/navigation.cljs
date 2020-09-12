(ns schnaq.interface.navigation
  (:require [schnaq.interface.routes :as routes]
            [re-frame.core :as rf]
            [reitit.frontend.controllers :as reitit-front-controllers]
            [reitit.frontend :as reitit-front]
            [reitit.frontend.easy :as reitit-front-easy]))

(def router
  (reitit-front/router
    routes/routes))

(defn on-navigate [new-match]
  (if new-match
    (rf/dispatch [:navigation/navigated new-match])
    (rf/dispatch [:navigation/navigate :routes/cause-not-found])))

(defn init-routes! []
  (reitit-front-easy/start!
    router
    on-navigate
    {:use-fragment false}))

(rf/reg-sub
  :navigation/current-route
  (fn [db]
    (:current-route db)))

(rf/reg-sub
  :navigation/current-view
  :<- [:navigation/current-route]
  (fn [current-route]
    (get-in current-route [:data :name])))

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