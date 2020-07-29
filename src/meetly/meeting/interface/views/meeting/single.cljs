(ns meetly.meeting.interface.views.meeting.single
  (:require [re-frame.core :as rf]
            [meetly.meeting.interface.text.display-data :as data]))


(defn- agenda-entry [agenda]
  [:div.card.meeting-entry.clickable
   {:on-click (fn []
                (rf/dispatch [:navigate :routes/meetings.discussion.start
                              {:id (-> agenda :agenda/discussion-id :db/id)}])
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
   (let [agendas @(rf/subscribe [:current-agendas])]
     (for [agenda agendas]
       [:div.py-3 {:key (:meeting/title (:db/id agenda))}
        [agenda-entry agenda]]))])

(defn- meeting-title [current-meeting]
  ;; meeting header
  [:div.meeting-header.header-custom.shadow-custom
   [:div.row
    [:div.col-lg-1.back-arrow
     [:i.arrow-icon {:class (str "m-auto fas " (data/fa :arrow-left))
                     :on-click #(rf/dispatch [:navigate :routes/meetings])}]]
    [:div.col-lg-8.container
     [:h2 (:meeting/title current-meeting)]
     [:h6 (:meeting/description current-meeting)]]]])

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

;; #### Events ####

(rf/reg-sub
  :selected-meeting
  (fn [db _]
    (get-in db [:meeting :selected])))