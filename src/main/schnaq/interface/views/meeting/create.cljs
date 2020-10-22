(ns schnaq.interface.views.meeting.create
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.agenda.agenda :as agenda]
            [schnaq.interface.views.text-editor.view :as editor]))

(defn- new-meeting-helper
  "Creates a new meeting with the form from `create-meeting-form`."
  [title description type]
  (rf/dispatch
    [:meeting.creation/new
     {:meeting/title title
      :meeting/description description
      :meeting/type type
      :meeting/end-date (js/Date. (str "2016-05-28T13:37"))
      :meeting/start-date (js/Date.)}]))

(defn- submit-meeting-button []
  [:button.btn.button-primary (labels :meeting-create-header)])

(defn- meeting-title-input
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
  [:<>
   (for [[suffix agenda] all-agendas]
     (let [delete-agenda-fn #(rf/dispatch [:agenda/delete-temporary suffix])
           update-description-fn (fn [value] (agenda/new-agenda-local :description value suffix))]
       [:div {:key suffix}
        [agenda/agenda-form delete-agenda-fn agenda update-description-fn (create-agenda-title-attributes suffix)]]))])


;; -----------------------------------------------------------------------------

(defn view []
  (let [temporary-agendas @(rf/subscribe [:agendas.temporary/all])
        description-storage-key :meeting.create/description]
    [:div.container.py-3
     [:form
      {:on-submit (fn [e]
                    (let [title (oget e [:target :elements :meeting-title :value])
                          description @(rf/subscribe [:mde/load-content description-storage-key])]
                      (js-wrap/prevent-default e)
                      (new-meeting-helper title description :meeting.type/meeting)))}
      [:div.agenda-meeting-container.shadow-straight.text-left.p-3
       [meeting-title-input]
       [editor/view-store-on-change description-storage-key]]
      [:div.agenda-container.text-center
       [agendas temporary-agendas]
       [:div.agenda-line]
       [agenda/add-agenda-button (count temporary-agendas) :agenda/increase-form-num]
       [submit-meeting-button]]]]))