(ns schnaq.interface.views.discussion.edit
  (:require [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))

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
      [:label {:for statement-html-id} (labels :statement.edit/label)]
      [:textarea.form-control {:name statement-html-id
                               :rows 3
                               :placeholder (:statement/content statement)
                               :defaultValue (:statement/content statement)}]]
     [:div.text-right
      [:button.btn.btn-outline-secondary
       {:on-click (fn [e]
                    (js-wrap/prevent-default e)
                    (rf/dispatch [:statement.edit/deactivate-edit (:db/id statement)]))}
       (labels :statement.edit.button/cancel)]
      [:button.btn.btn-outline-primary.ml-1 {:type "submit"} (labels :statement.edit.button/submit)]]]))

(rf/reg-event-fx
  :statement.edit/send
  (fn [{:keys [db]} [_ statement-id html-selector form]]
    (let [share-hash (get-in db [:current-route :path-params :share-hash])]
      {:fx [(http/xhrio-request db :put "/discussion/statement/edit"
                                [:statement.edit.send/success form]
                                {:statement-id statement-id
                                 :share-hash share-hash
                                 :new-content (oget+ form [html-selector :value])}
                                [:statement.edit.send/failure])]})))

(defn- update-statement-in-list
  "Updates the content of a statement in a collection."
  [coll new-statement]
  ;; Merge instead of overwriting, to preserve meta information
  (map #(if (= (:db/id new-statement) (:db/id %)) (merge % new-statement) %) coll))

(rf/reg-event-fx
  :statement.edit.send/success
  (fn [{:keys [db]} [_ form response]]
    (let [updated-statement (:updated-statement response)]
      {:db (-> db
               (update-in [:discussion :conclusions :starting] #(update-statement-in-list % updated-statement))
               (update-in [:discussion :premises :current] #(update-statement-in-list % updated-statement)))
       :fx [[:form/clear form]
            [:dispatch [:statement.edit/deactivate-edit (:db/id updated-statement)]]]})))

(rf/reg-event-fx
  :statement.edit.send/failure
  (fn [_ _]
    {:fx [[:dispatch [:notification/add
                      #:notification{:title (labels :statement.edit.send.failure/title)
                                     :body (labels :statement.edit.send.failure/body)
                                     :context :danger}]]]}))

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

(rf/reg-event-db
  :statement.edit/reset-edits
  (fn [db _]
    (assoc-in db [:statements :currently-edited] #{})))

(rf/reg-sub
  :statement.edit/ongoing?
  (fn [db [_ statement-id]]
    (contains? (get-in db [:statements :currently-edited] #{}) statement-id)))