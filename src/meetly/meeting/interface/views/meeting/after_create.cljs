(ns meetly.meeting.interface.views.meeting.after-create
  (:require [re-frame.core :as rf]
            [meetly.meeting.interface.views.meeting.single :as meeting]))

(defn after-meeting-creation-view
  "This view is presented to the user after they have created a new meeting. They should
  see the share-link and should be able to copy it easily."
  []
  (let [current-meeting @(rf/subscribe [:selected-meeting])]
    [:div
     [meeting/meeting-title current-meeting]
     [:div.container.py-2
      [:div.meeting-single-rounded
       ;; list agendas
       [:h1 "Glückwunsch Meister! Geh jezze den Link teilen, ja?"]
       [:h3 "Oder diskutier mit dir selbst..."]]]]))