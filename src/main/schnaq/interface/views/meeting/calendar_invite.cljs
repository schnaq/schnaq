(ns schnaq.interface.views.meeting.calendar-invite
  (:require [reagent-forms.core :refer [bind-fields]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.modals.modal :as modal]
            [re-frame.core :as re-frame]))


; Functions that will be called by each individual form field with an id and a value
(def events
  {:get (fn [path] @(re-frame/subscribe [:value path]))
   :save! (fn [path value] (re-frame/dispatch [:set-value path value]))
   :update! (fn [path save-fn value]
              ; save-fn should accept two arguments: old-value, new-value
              (re-frame/dispatch [:update-value save-fn path value]))
   :calendar-invite (fn [] @(re-frame/subscribe [:calendar-invite]))})

(defn modal []
  (let [input-id "participant-email-addresses"]
    [modal/modal-template (labels :calendar-invitation/title)
     [bind-fields
      [:form.form

       {:on-submit
        (fn [_e] (println "foo"))}

       [:div.input-group.date.datepicker.clickable
        {:field :datepicker
         :id :date
         :date-format (fn [date]
                        (str (.getDate date) "."
                             (inc (.getMonth date)) "."
                             (.getFullYear date)))
         :save-fn (fn [current-date {:keys [year month day]}]
                    (if current-date
                      (doto (js/Date.)
                        (.setFullYear year)
                        (.setMonth (dec month))
                        (.setDate day)
                        (.setHours (.getHours current-date))
                        (.setMinutes (.getMinutes current-date)))
                      (js/Date. year (dec month) day)))
         :auto-close? true}]

       [:label.m-1 {:for input-id} (labels :meeting.admin/addresses-label)]
       [:textarea.form-control.m-1.input-rounded
        {:id input-id
         :name "participant-addresses" :wrap "soft" :rows 3
         :auto-complete "off"
         :required true
         :placeholder (labels :meeting.admin/addresses-placeholder)}]

       [:button.btn.btn-outline-primary
        (labels :meeting.admin/send-invites-button-text)]
       ] events]]))

;; re-frame events
(re-frame/reg-event-db
  :init
  (fn [_ _]
    {:calendar-invite {}}))

(re-frame/reg-sub
  :calendar-invite
  (fn [db _]
    (:calendar-invite db)))

(re-frame/reg-sub
  :value
  :<- [:calendar-invite]
  (fn [calendar-invite [_ path]]
    (get-in calendar-invite path)))

(re-frame/reg-event-db
  :set-value
  (fn [db [_ path value]]
    (assoc-in db (into [:calendar-invite] path) value)))

(re-frame/reg-event-db
  :update-value
  (fn [db [_ f path value]]
    (update-in db (into [:calendar-invite] path) f value)))
