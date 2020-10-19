(ns schnaq.interface.views.meeting.calendar-invite
  (:require ["jquery" :as jquery]
            [cljs-time.core :as time]
            [clojure.string :as string]
            [goog.string :as gstring]
            [oops.core :refer [oget+]]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.modals.modal :as modal]))

(def ^:private allowed-times
  "Create allowed times for the datepicker."
  (for [hours ["00" "01" "02" "03" "04" "05" "06" "07" "08" "09" "10" "11" "12"
               "13" "14" "15" "16" "17" "18" "19" "20" "21" "22" "23"]
        minutes ["00" "10" "20" "30" "40" "50"]]
    (gstring/format "%s:%s" hours minutes)))

(defn- element->datetimepicker!
  "Converts a dom element to a jquery datetimepicker."
  [id]
  (.datetimepicker
    (jquery id)
    (clj->js {:allowTimes allowed-times})))

(defn- parse-datetime
  "Takes a datetime-string from jQuery DateTimePicker and converts it to edn."
  [datetime-string]
  (let [[date time] (string/split datetime-string #" ")
        [year month day] (map js/parseInt (string/split date #"/"))
        [hour minute] (map js/parseInt (string/split time #":"))]
    (when (and year month day hour minute)
      (time/date-time year month day hour minute))))

(defn modal []
  (reagent/create-class
    (let [start-date-id "calendar-start-invite"
          end-date-id "calendar-end-invite"]
      {:component-did-mount
       (fn [] (element->datetimepicker! (str "#" start-date-id))
         (element->datetimepicker! (str "#" end-date-id)))
       :reagent-render
       (fn []
         [modal/modal-template (labels :calendar-invitation/title)
          [:<>
           [:form.form {:on-submit (fn [e]
                                     (js-wrap/prevent-default e)
                                     (prn (oget+ e [:target :elements start-date-id :value])))}
            [:div.form-group
             [:label {:for start-date-id} "Startzeit wählen"]
             [:input.form-control
              {:id start-date-id :type "text" :aria-describedby start-date-id
               :required true :auto-complete "off"}]]
            [:div.form-group
             [:label {:for end-date-id} "Endzeit wählen"]
             [:input.form-control
              {:id end-date-id :type "text" :aria-describedby end-date-id
               :required true :auto-complete "off"}]]
            [:input.btn.btn-outline-primary.mt-1.mt-sm-0
             {:type "submit"
              :value "Abschicken"}]]]])})))
