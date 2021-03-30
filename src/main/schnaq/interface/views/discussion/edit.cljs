(ns schnaq.interface.views.discussion.edit
  (:require [re-frame.core :as rf]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))

(defn edit-card
  "The same as a statement-card, but currently being an editable input."
  [statement]
  (let [statement-html-id (str "statement-edit-" (:db/id statement))]
    [:form.card.statement-card.py-2.px-3
     [:div.form-group
      [:label {:for statement-html-id}
       "New statement text:"]
      [:textarea.form-control {:id statement-html-id
                               :rows 3
                               :placeholder (:statement/content statement)
                               :defaultValue (:statement/content statement)}]]
     [:div.text-right
      [:button.btn.btn-outline-primary.mr-1 "Submit"]
      [:button.btn.btn-outline-secondary
       {:on-click (fn [e]
                    (js-wrap/prevent-default e)
                    (rf/dispatch [:statement.edit/deactivate-edit (:db/id statement)]))}
       "Cancel"]]]))

(rf/reg-event-db
  :statement.edit/activate-edit
  (fn [db [_ statement-id]]
    (update-in db [:statements :currently-edited] #(if (nil? %)
                                                     #{statement-id}
                                                     (conj % statement-id)))))

(rf/reg-event-db
  :statement.edit/deactivate-edit
  (fn [db [_ statement-id]]
    (update-in db [:statements :currently-edited] disj statement-id)))

(rf/reg-sub
  :statement.edit/ongoing?
  (fn [db [_ statement-id]]
    (contains? (get-in db [:statements :currently-edited] #{}) statement-id)))