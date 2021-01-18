(ns schnaq.interface.views.agenda.agenda
  (:require [ajax.core :as ajax]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]))

(rf/reg-event-fx
  :agenda/load-chosen
  ;; TODO possible del
  (fn [{:keys [db]} [_ share-hash discussion-id]]
    (when-not (-> db :agenda :chosen)
      {:fx [[:http-xhrio {:method :get
                          :uri (gstring/format "%s/agenda/%s/%s" (:rest-backend config) share-hash discussion-id)
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:agenda/set-response-as-chosen]
                          :on-failure [:agenda.error/not-available]}]]})))

(rf/reg-event-fx
  :agenda.error/not-available
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:error :ajax] "Agenda could not be loaded, please refresh the App.")
     :fx [[:dispatch [:navigation/navigate :routes/meetings]]]}))

(rf/reg-sub
  :agenda.meta/statement-num
  ;; todo del
  (fn [db _]
    (get-in db [:agendas :meta :statement-num])))

(rf/reg-event-db
  :agenda/clear-current
  (fn [db _]
    (assoc-in db [:agendas :current] [])))

(rf/reg-event-db
  :agenda/increase-form-num
  (fn [db _]
    (let [all-temp-agendas (vals (get-in db [:agenda :creating :all]))
          all-ranks (conj (map :agenda/rank all-temp-agendas) 0)
          biggest-rank (apply max all-ranks)]
      (assoc-in db [:agenda :creating :all (random-uuid)] {:agenda/rank (inc biggest-rank)}))))

(rf/reg-event-db
  :agenda/reset-temporary-entries
  (fn [db _]
    (assoc-in db [:agenda :creating :all] {})))

(rf/reg-event-db
  ;; TODO del
  :agenda/choose
  (fn [db [_ agenda]]
    (assoc-in db [:agenda :chosen] agenda)))

;; #### Subs ####

(rf/reg-sub
  ;; TODO del
  :current-agendas
  (fn [db _]
    (sort-by :agenda/rank (get-in db [:agendas :current]))))
