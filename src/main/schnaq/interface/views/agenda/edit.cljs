(ns schnaq.interface.views.agenda.edit
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels fa]]))

(rf/reg-sub
  :agenda.edit/agenda-description-update
  (fn [db [_ id]]
    (get-in db [:edit-meeting-updates :agenda-descriptions id])))

(rf/reg-event-db
  :agenda.edit/reset-editor-update-flag
  (fn [db _]
    (assoc db :edit-meeting-updates {})))
