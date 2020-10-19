(ns schnaq.interface.views.meeting.calendar-invite
  (:require ["jquery" :as jquery]
            [cljs-time.core :as time]
            [cljs-time.format :as tformat]
            [goog.string :as gstring]
            [oops.core :refer [oget+ oset!]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.modals.modal :as modal]))

(def ^:private ical-template
  "BEGIN:VCALENDAR
VERSION:2.0
PRODID:%s//schnaq.com
METHOD:PUBLISH
BEGIN:VEVENT
UID:%s
LOCATION:%s
SUMMARY:schnaq: \"%s\"
DESCRIPTION:%s
CLASS:PUBLIC
DTSTART:%s
DTEND:%s
DTSTAMP:%s
END:VEVENT
END:VCALENDAR")

(defn- create-ics
  "Create ics calendar entry."
  [username title description schnaq-link start end]
  (let [unparser (partial tformat/unparse (tformat/formatters :basic-date-time-no-ms))]
    (gstring/format ical-template
                    username username schnaq-link title (or description "")
                    (unparser start) (unparser end) (unparser (time/now)))))

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

(defn- datetime-add-timezone-difference
  "Takes current time zone of the client, and adds / subtracts the difference to
  the UTC timezone to it.
  Necessary, because the datetime of jquery-datetimepicker is of UTC and does
  not respect the user's location / settings."
  [datetime]
  (let [[sign hours] (-> (time/default-time-zone) :offset)
        sign-fn (if (= :+ sign) time/minus time/plus)]
    (time/to-default-time-zone
      (sign-fn datetime (time/hours hours)))))

(defn- parse-datetime
  "Takes a datetime-string from jQuery DateTimePicker and creates a cljs-time
  object."
  [datetime-string]
  (let [from-jquery-datepicker (tformat/formatter "YYYY/MM/dd HH:mm")]
    (tformat/parse from-jquery-datepicker datetime-string)))


(defn modal []
  (reagent/create-class
    (let [start-date-id "calendar-start-invite"
          end-date-id "calendar-end-invite"]
      {:component-did-mount
       (fn [] (element->datetimepicker! (str "#" start-date-id))
         (element->datetimepicker! (str "#" end-date-id)))
       :reagent-render
       (fn []
         (let [{:meeting/keys [title description]} @(rf/subscribe [:meeting/selected])
               username @(rf/subscribe [:user/display-name])
               current-route @(rf/subscribe [:navigation/current-route])
               share-link (common/get-share-link current-route)]
           [modal/modal-template (labels :calendar-invitation/title)
            [:<>
             [:form.form
              {:on-submit
               (fn [e]
                 (js-wrap/prevent-default e)
                 (let [start (datetime-add-timezone-difference
                               (parse-datetime (oget+ e [:target :elements start-date-id :value])))
                       end (datetime-add-timezone-difference
                             (parse-datetime (oget+ e [:target :elements end-date-id :value])))]
                   (if (time/before? end start)
                     (js/alert (labels :calendar-invitation/date-error))
                     (let [ics (create-ics username title description share-link start end)
                           uri-content (str "data:application/octet-stream," (js/encodeURIComponent ics))
                           anchor (.createElement js/document "a")]
                       (oset! anchor [:href] uri-content)
                       (oset! anchor [:download] "calendar.ics")
                       (.click anchor)))))}
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
                :value (labels :calendar-invitation/download-button)}]]]]))})))
