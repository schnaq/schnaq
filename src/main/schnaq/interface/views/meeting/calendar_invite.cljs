(ns schnaq.interface.views.meeting.calendar-invite
  (:require ["jquery" :as jquery]
            ["jquery-datetimepicker"]
            [goog.string :as gstring]
            [oops.core :refer [oget+]]
            [re-frame.core :as re-frame]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.modals.modal :as modal]
            [reagent.core :as reagent]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [clojure.string :as string]))

(def ^:private allowed-times
  "Create allowed times for the datepicker."
  (for [hours ["00" "01" "02" "03" "04" "05" "06" "07" "08" "09" "10" "11" "12"
               "13" "14" "15" "16" "17" "18" "19" "20" "21" "22" "23"]
        minutes ["00" "10" "20" "30" "40" "50"]]
    (gstring/format "%s:%s" hours minutes)))

(defn- parse-date
  "Takes a datetime-string from jQuery DateTimePicker and converts it to edn."
  [datetime-string]
  (let [[date time] (string/split datetime-string #" ")
        [year month day] (map js/parseInt (string/split date #"/"))
        [hour minute] (map js/parseInt (string/split time #":"))]
    {:year year :month month :day day
     :hour hour :minute minute}))

(defn modal []
  (reagent/create-class
    (let [datepicker-id "datetime-calendar-invite"]
      {:component-did-mount
       (fn [] (.datetimepicker
                (jquery (str "#" datepicker-id))
                (clj->js {:allowTimes allowed-times})))
       :reagent-render
       (fn []
         [modal/modal-template (labels :calendar-invitation/title)
          [:<>
           [:form.form {:on-submit (fn [e]
                                     (js-wrap/prevent-default e)
                                     (prn (oget+ e [:target :elements datepicker-id :value])))}
            [:div.form-group
             [:label {:for datepicker-id} "Startzeit wählen"]
             [:input.form-control
              {:id datepicker-id :type "text" :aria-describedby datepicker-id
               :required true}]]
            [:input.btn.btn-outline-primary.mt-1.mt-sm-0
             {:type "submit"
              :value "Abschicken"}]]]])})))
