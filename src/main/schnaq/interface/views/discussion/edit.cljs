(ns schnaq.interface.views.discussion.edit
  (:require [re-frame.core :as rf]))

(defn edit-card
  "The same as a statement-card, but currently being an editable input."
  [statement]
  [:form.card.card-rounded
   [:label "Currently being edited"]
   [:input {:type "text"
            :defaultValue (:statement/content statement)}]
   [:button.btn.btn-outline-primary "Submit"]
   [:button.btn.btn-outline-secondary "Cancel"]])

(rf/reg-event-db
  :statement.edit/activate-edit
  (fn [db [_ statement-id]]
    (update-in db [:statements :currently-edited] #(if (nil? %)
                                                     #{statement-id}
                                                     (conj % statement-id)))))

(rf/reg-sub
  :statement.edit/ongoing?
  (fn [db [_ statement-id]]
    (contains? (get-in db [:statements :currently-edited] #{}) statement-id)))