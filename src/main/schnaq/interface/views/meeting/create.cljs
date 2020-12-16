(ns schnaq.interface.views.meeting.create
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]))

(defn new-meeting-helper
  "Creates a new meeting with the form from `create-meeting-form`."
  [title description type]
  (rf/dispatch
    [:meeting.creation/new
     {:meeting/title title
      :meeting/description description
      :meeting/type type
      :meeting/end-date (js/Date. (str "2016-05-28T13:37"))
      :meeting/start-date (js/Date.)}]))

(defn meeting-title-input
  "The input and label for a new meeting-title"
  []
  [:<>
   [:input#meeting-title.form-control.form-title.form-border-bottom.mb-2
    {:type "text"
     :autoComplete "off"
     :required true
     :placeholder (labels :meeting-form-title-placeholder)}]])
