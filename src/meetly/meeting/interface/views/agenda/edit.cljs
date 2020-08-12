(ns meetly.meeting.interface.views.agenda.edit
  (:require [meetly.meeting.interface.views.agenda.agenda :as agenda]
            [meetly.meeting.interface.views.base :as base]
            [re-frame.core :as rf]
            [meetly.meeting.interface.text.display-data :refer [labels fa]]))


(defn- agenda-view [agenda]
  (let [db-id (:db/id agenda)]
    [:div
     [:div.agenda-line]
     [:div.add-agenda-div.agenda-point
      [:div.row
       [:div.col-11
        ;; title
        [:input.form-control.agenda-form-title.form-title
         {:type "text"
          :name "title"
          :auto-complete "off"
          :required true
          :placeholder (labels :agenda-point)
          :default-value (:agenda/title agenda)
          :id (str "title-" db-id)}]]
       [:div.col-1
        [:div.pr-4.pt-3.link-pointer
         [:i {:class (str "m-auto fas fa-lg " (fa :delete-icon))}]]]]
      ;; description
      [:textarea.form-control.agenda-form-round
       {:name "description"
        :placeholder (labels :agenda-desc-for)
        :default-value (:agenda/description agenda)
        :id (str "description-" db-id)}]]]))

(defn edit-view []
  (let [edit-information @(rf/subscribe [:agenda/current-edit-info])
        selected-meeting (:meeting edit-information)
        meeting-agendas (:agendas edit-information)]
    [:div#create-agenda
     (println meeting-agendas)
     [base/nav-header]
     [agenda/header]
     [:div.container.px-5.py-3.text-center
      [:div.agenda-meeting-title
       [:h2 (:meeting/title selected-meeting)]
       [:br]
       [:h4 (:meeting/description selected-meeting)]]
      [:div.container
       [:div.agenda-container
        [:form {:id "agendas-add-form"
                :on-submit (fn [e]
                             (.preventDefault e)
                             (rf/dispatch [:todo]))}
         (for [agenda meeting-agendas]
           [:div {:key (:db/id agenda)}
            [agenda-view agenda]])
         [:div.agenda-line]
         [agenda/add-agenda-button]
         [:br]
         [:br]
         [:br]
         [agenda/submit-agenda-button]]]]]])

  )

(rf/reg-event-fx
  :agenda/load-for-edit
  (fn [_ [_ hash]]
    (agenda/load-agenda-fn hash :agenda/load-for-edit-success)))

(rf/reg-event-db
  :agenda/load-for-edit-success
  (fn [db [_ agendas]]
    (-> db
        (assoc-in [:edit-meeting :agendas] agendas)
        (assoc-in [:edit-meeting :meeting] (get-in db [:meeting :selected])))))

(rf/reg-sub
  :agenda/current-edit-info
  (fn [db _]
    (:edit-meeting db)))