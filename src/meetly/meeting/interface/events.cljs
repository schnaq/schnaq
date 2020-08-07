(ns meetly.meeting.interface.events
  (:require [ajax.core :as ajax]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitit-front-easy]
            [reitit.frontend.controllers :as reitit-front-controllers]
            [clojure.string :as clj-string]
            [meetly.meeting.interface.db :as meetly-db]
            [meetly.meeting.interface.views.modals.modal :as modal]
            [meetly.meeting.interface.utils.localstorage :as ls]
            [meetly.meeting.interface.config :refer [config]]))

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
    (let [init-fx {:db meetly-db/default-db
                   :http-xhrio {:method :get
                                :uri (str (:rest-backend config) "/meetings")
                                :timeout 10000
                                :response-format (ajax/transit-response-format)
                                :on-success [:init-from-backend]
                                :on-failure [:ajax-failure]}}]
      (if-let [name (ls/get-item :username)]
        ;; When the localstorage is filled, then just set the name to db.
        (assoc-in init-fx [:db :user :name] name)
        ;; Otherwise ask user for name
        (assoc init-fx :dispatch-n [[:set-username "Anonymous"]
                                    [:modal {:show? true
                                             :child [modal/enter-name-modal]}]])))))

(rf/reg-event-fx
  :set-username
  (fn [{:keys [db]} [_ username]]
    ;; only update when string contains
    (when (not (clj-string/blank? username))
      (let [fx {:http-xhrio {:method :post
                             :uri (str (:rest-backend config) "/author/add")
                             :params {:nickname username}
                             :format (ajax/transit-request-format)
                             :response-format (ajax/transit-response-format)
                             :on-success [:hide-name-input]
                             :on-failure [:ajax-failure]}
                :db (assoc-in db [:user :name] username)}]
        (if (= "Anonymous" username)
          fx
          (assoc fx :write-localstorage [:username username]))))))

(rf/reg-event-db
  :ajax-failure
  (fn [db [_ failure]]
    (assoc db :error {:ajax failure})))

(rf/reg-event-db
  :init-from-backend
  (fn [db [_ all-meetings]]
    (assoc db :meetings all-meetings)))

(rf/reg-event-db
  :hide-name-input
  (fn [db _]
    (assoc-in db [:controls :username-input :show?] false)))

(rf/reg-event-db
  :show-name-input
  (fn [db _]
    (assoc-in db [:controls :username-input :show?] true)))

(rf/reg-event-fx
  :handle-reload-on-discussion-loop
  (fn [{:keys [db]} [_ agenda-id share-hash]]
    (when (empty? (get-in db [:discussion :options :steps]))
      {:dispatch [:navigate
                  :routes/meetings.discussion.start
                  {:id agenda-id
                   :share-hash share-hash}]})))