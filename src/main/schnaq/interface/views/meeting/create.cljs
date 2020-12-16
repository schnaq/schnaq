(ns schnaq.interface.views.meeting.create
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.agenda.agenda :as agenda]))

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

(defn- create-agenda-title-attributes [suffix]
  {:type "text"
   :name (str "title-" suffix)
   :auto-complete "off"
   :required true
   :placeholder (labels :agenda/point)
   :id (str "title-" suffix)
   :on-key-up #(agenda/new-agenda-local :title (oget % [:target :value]) suffix)})

(defn- agendas [all-agendas]
  ;; TODO löschkandidat
  [:<>
   (for [[suffix agenda] all-agendas]
     (let [delete-agenda-fn #(rf/dispatch [:agenda/delete-temporary suffix])
           update-description-fn (fn [value] (agenda/new-agenda-local :description value suffix))]
       [:div {:key suffix}
        [agenda/agenda-form delete-agenda-fn agenda update-description-fn (create-agenda-title-attributes suffix)]]))])
