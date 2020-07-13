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
  :meeting-added
  (fn [db [_ meeting response]]
    (assoc db :meeting/added
              (assoc meeting :id (:id-created response)))))

(rf/reg-event-db
  :increase-agenda-forms
  (fn [db _]
    (update-in db [:agenda :number-of-forms] inc)))

(rf/reg-event-db
  :agenda/update-title
  (fn [db [_ content suffix]]
    (assoc-in db [:agenda :all suffix :title] content)))

(rf/reg-event-db
  :agenda/update-description
  (fn [db [_ content suffix]]
    (assoc-in db [:agenda :all suffix :description] content)))

(rf/reg-event-db
  :reset-temporary-agenda
  (fn [db _]
    (assoc db :agenda {:number-of-forms 1 :all {}})))

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

(rf/reg-event-fx
  :new-meeting
  (fn [{:keys [db]} [_ meeting]]
    {:db (update db :meetings conj meeting)
     :http-xhrio {:method :post
                  :uri "http://localhost:3000/meeting/add"
                  :params {:meeting meeting}
                  :format (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:meeting-added meeting]
                  :on-failure [:ajax-failure]}}))

(rf/reg-event-fx
  :send-agendas
  (fn [{:keys [db]} _]
    {:http-xhrio {:method :post
                  :uri "http://localhost:3000/agendas/add"
                  :params {:agendas (:agenda db)
                           :meeting-id (-> db :meeting/added :id)}
                  :format (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:reset-temporary-agenda]
                  :on-failure [:ajax-failure]}}))