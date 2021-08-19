(ns schnaq.interface.scheduler
  "Scheduler, which can be called to trigger events after another event occurred."
  (:require [ghostwheel.core :refer [>defn]]
            [re-frame.core :as rf]))

(rf/reg-event-db
  ;; Add events which shall be loaded after the user logged in.
  ;; Add events as vectors: [:feedbacks/fetch] or [:event/name parameter]
  :scheduler.after/login
  (fn [db [_ event]]
    (assoc-in db [:scheduler :after/login]
              (conj (get-in db [:scheduler :after/login] []) event))))

(rf/reg-event-fx
  :scheduler.execute/after-login
  (fn [{:keys [db]} [_ _]]
    (let [after-login (get-in db [:scheduler :after/login])
          prepend-dispatch-to-events (vec (for [event after-login] [:dispatch event]))
          events-without-after-login (filterv (complement (into #{} after-login)) after-login)]
      {:fx prepend-dispatch-to-events
       :db (assoc-in db [:scheduler :after/login] events-without-after-login)})))

(rf/reg-sub
  :scheduler.login/has-events?
  (fn [db _]
    (seq (get-in db [:scheduler :after/login]))))

(>defn middleware
  "Use scheduler as middleware when a page is refreshed."
  [view]
  [vector? :ret vector?]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])
        has-events-after-login? @(rf/subscribe [:scheduler.login/has-events?])]
    (when (and has-events-after-login? authenticated?)
      (rf/dispatch [:scheduler.execute/after-login]))
    view))
