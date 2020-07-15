(ns meetly.meeting.interface.events
  (:require [ajax.core :as ajax]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitit-front-easy]
            [reitit.frontend.controllers :as reitit-front-controllers]
            [meetly.meeting.interface.db :as meetly-db]))

;; Starts the ball rolling on changing to another view
(rf/reg-event-fx
  :navigate
  (fn [_cofx [_ & route]]
    {:navigate! route}))

(rf/reg-fx
  :navigate!
  (fn [route]
    (apply reitit-front-easy/push-state route)))

(rf/reg-event-db
  :navigated
  (fn [db [_ new-match]]
    (let [old-match (:current-route db)
          controllers (reitit-front-controllers/apply-controllers (:controllers old-match) new-match)]
      (assoc db :current-route (assoc new-match :controllers controllers)))))

;; Non routing events

(rf/reg-event-fx
  :initialise-db
  (fn [_ _]
    {:db meetly-db/default-db
     :http-xhrio {:method :get
                  :uri "http://localhost:3000/meetings"
                  :timeout 10000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:init-from-backend]
                  :on-failure [:ajax-failure]}}))

(rf/reg-event-db
  :ajax-failure
  (fn [db [_ failure]]
    (assoc db :error {:ajax failure})))

(rf/reg-event-db
  :init-from-backend
  (fn [db [_ all-meetings]]
    (assoc db :meetings all-meetings)))

(rf/reg-event-db
  :set-username
  (fn [db [_ username]]
    (assoc-in db [:user :name] username)))

(rf/reg-event-db
  :hide-name-input
  (fn [db _]
    (assoc-in db [:controls :username-input :show?] false)))

(rf/reg-event-db
  :show-name-input
  (fn [db _]
    (assoc-in db [:controls :username-input :show?] true)))

