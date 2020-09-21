(ns schnaq.interface.views.meeting.edit
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :as data]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.agenda.agenda :as agenda]
            [schnaq.interface.views.text-editor.view :as editor]))

(defn- new-meeting-helper
  "Creates a new meeting with the form from `create-meeting-form`."
  [title description]
  (rf/dispatch
    [:meeting.creation/new
     {:meeting/title title
      :meeting/description description
      :meeting/end-date (js/Date. (str "2016-05-28T13:37"))
      :meeting/start-date (js/Date.)}]))

(defn- submit-meeting-button []
  [:button.btn.button-primary (data/labels :meeting-create-header)])

(defn- meeting-title-input
  "The input and label for a new meeting-title"
  []
  [:<>
   [:input#meeting-title.form-control.form-title.form-border-bottom.mb-2
    {:type "text"
     :autoComplete "off"
     :required true
     :placeholder (data/labels :meeting-form-title-placeholder)}]])

(defn view []
  (let [number-of-forms @(rf/subscribe [:agenda/number-of-forms])
        description-storage-key :meeting.create/description]
    [:div.container.py-3
     [:form
      {:on-submit (fn [e]
                    (let [title (oget e [:target :elements :meeting-title :value])
                          description @(rf/subscribe [:mde/load-content description-storage-key])]
                      (js-wrap/prevent-default e)
                      (new-meeting-helper title description)))}
      [:div.agenda-meeting-container.shadow-straight.text-left.p-3
       [meeting-title-input]
       [editor/view-store-on-change description-storage-key]]
      [:div.agenda-container.text-center
       (for [agenda-num (range number-of-forms)]
         [:div {:key agenda-num}
          [agenda/new-agenda-form agenda-num]])
       [:div.agenda-line]
       [agenda/add-agenda-button number-of-forms :agenda/increase-form-num]
       [submit-meeting-button]]]]))