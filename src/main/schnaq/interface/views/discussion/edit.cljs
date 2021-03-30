(ns schnaq.interface.views.discussion.edit
  (:require [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))

;; TODO replace strings with labels

(defn edit-card
  "The same as a statement-card, but currently being an editable input."
  [statement]
  (let [statement-html-id (str "statement-edit-" (:db/id statement))]
    [:form.card.statement-card.py-2.px-3
     {:on-submit (fn [e]
                   (js-wrap/prevent-default e)
                   (rf/dispatch [:statement.edit/send (:db/id statement) statement-html-id
                                 (oget e [:target :elements])]))}
     [:div.form-group
      [:label {:for statement-html-id} "New statement text:"]
      [:textarea.form-control {:id statement-html-id
                               :rows 3
                               :placeholder (:statement/content statement)
                               :defaultValue (:statement/content statement)}]]
     [:div.text-right
      [:button.btn.btn-outline-primary.mr-1 {:type "submit"} "Submit"]
      [:button.btn.btn-outline-secondary
       {:on-click (fn [e]
                    (js-wrap/prevent-default e)
                    (rf/dispatch [:statement.edit/deactivate-edit (:db/id statement)]))}
       "Cancel"]]]))

(rf/reg-event-fx
  :statement.edit/send
  (fn [{:keys [db]} [_ statement-id html-selector form]]
    (let [share-hash (get-in db [:current-route :path-params :share-hash])]
      {:fx [(http/xhrio-request db :put "/discussion/statement/edit"
                                [:statement.edit.send/success form]
                                {:statement-id statement-id
                                 :share-hash share-hash
                                 :new-content (oget+ form [html-selector :value])}
                                ;; TODO failure case
                                [:statement.edit.send/failure])]})))

(defn- update-statement-in-list
  "Updates the content of a statement in a collection."
  [coll new-statement]
  (map #(if (= (:db/id new-statement) (:db/id %)) new-statement %) coll))

;; TODO jede menge warnings auf der Konsole
(rf/reg-event-fx
  :statement.edit.send/success
  (fn [{:keys [db]} [_ form response]]
    (let [updated-statement (:updated-statement response)]
      {:db (-> db
               (update-in [:discussion :conclusions :starting] #(update-statement-in-list % updated-statement))
               (update-in [:discussion :premises :current] #(update-statement-in-list % updated-statement)))
       :fx [[:form/clear form]
            [:dispatch [:statement.edit/deactivate-edit (:db/id updated-statement)]]]})))

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