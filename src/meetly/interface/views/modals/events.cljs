(ns meetly.interface.views.modals.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
  :modal
  (fn [db [_ data]]
    (assoc-in db [:modal] data)))