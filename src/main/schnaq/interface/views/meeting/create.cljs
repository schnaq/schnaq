(ns schnaq.interface.views.meeting.create
  (:require [schnaq.interface.text.display-data :refer [labels]]))

(defn meeting-title-input
  "The input and label for a new meeting-title"
  []
  [:<>
   [:input#meeting-title.form-control.form-title.form-border-bottom.mb-2
    {:type "text"
     :autoComplete "off"
     :required true
     :placeholder (labels :meeting-form-title-placeholder)}]])
