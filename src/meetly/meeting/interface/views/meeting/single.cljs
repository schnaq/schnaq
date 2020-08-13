(ns meetly.meeting.interface.views.meeting.single
  (:require [re-frame.core :as rf]
            [meetly.meeting.interface.config :refer [config]]
            [meetly.meeting.interface.views.base :as base]))


(defn- agenda-entry [agenda meeting]
  [:div.card.meeting-entry
   {:on-click (fn []
                (rf/dispatch [:navigate :routes/meetings.discussion.start
                              {:id (-> agenda :agenda/discussion :db/id)
                               :share-hash (:meeting/share-hash meeting)}])
                (rf/dispatch [:choose-agenda agenda]))}
   ;; title
   [:div.meeting-entry-title
    [:h4 (:agenda/title agenda)]]
   ;; description
   [:div.meeting-entry-desc
    [:hr]
    [:h6 (:agenda/description agenda)]]])


(defn agenda-in-meeting-view
  "The view of an agenda which gets embedded inside a meeting view."
  []
  [:div
   (let [agendas @(rf/subscribe [:current-agendas])
         meeting @(rf/subscribe [:selected-meeting])]
     (for [agenda agendas]
       [:div.py-3 {:key (:db/id agenda)}
        [agenda-entry agenda meeting]]))])

(defn- meeting-title [current-meeting]
  ;; meeting header
  (base/discussion-header
    (:meeting/title current-meeting)
    (:meeting/description current-meeting)
    nil                                                     ;; header should not be clickable in overview
    (when (not= "production" (:environment config))         ;; when in dev display back button
      #(rf/dispatch [:navigate :routes/meetings]))))

(defn- single-meeting []
  (let [current-meeting @(rf/subscribe [:selected-meeting])]
    ;; meeting header
    [:div
     [meeting-title current-meeting]
     [:div.container.py-2
      [:div.meeting-single-rounded
       ;; list agendas
       [agenda-in-meeting-view]]]]))

(defn single-meeting-view
  "Show a single meeting and all its Agendas."
  []
  [:div
   [single-meeting]])