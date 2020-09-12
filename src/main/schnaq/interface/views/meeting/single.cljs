(ns schnaq.interface.views.meeting.single
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.base :as base]))

(defn- agenda-entry [agenda meeting]
  [:div.card.meeting-entry
   {:on-click (fn []
                (rf/dispatch [:navigation/navigate :routes.discussion/start
                              {:id (-> agenda :agenda/discussion :db/id)
                               :share-hash (:meeting/share-hash meeting)}])
                (rf/dispatch [:agenda/choose agenda]))}
   ;; title
   [:div.meeting-entry-title
    [:h4 (:agenda/title agenda)]]
   ;; description
   [:div.meeting-entry-desc
    [:hr]
    [:h6 (:agenda/description agenda)]]])


(defn agenda-in-meeting-view
  "The view of an agenda which gets embedded inside a meeting view."
  [meeting]
  [:div
   (let [agendas @(rf/subscribe [:current-agendas])]
     (for [agenda agendas]
       [:div.py-3 {:key (:db/id agenda)}
        [agenda-entry agenda meeting]]))])

(defn- meeting-title [current-meeting]
  ;; meeting header
  [base/discussion-header
   (:meeting/title current-meeting)
   (:meeting/description current-meeting)
   nil                                                      ;; header should not be clickable in overview
   (when-not toolbelt/production?                           ;; when in dev display back button
     (fn []
       (rf/dispatch [:navigation/navigate :routes/meetings])))])

(defn- single-meeting []
  (let [current-meeting @(rf/subscribe [:meeting/selected])]
    ;; meeting header
    [:div
     [base/nav-header]
     [meeting-title current-meeting]
     [:div.container.py-2
      [:div.meeting-single-rounded
       ;; list agendas
       [agenda-in-meeting-view current-meeting]]
      [:div.text-center.pb-2
       [:button.btn.button-primary.button-md
        {:on-click #(rf/dispatch [:navigation/navigate :routes.meeting/suggestions
                                  {:share-hash (:meeting/share-hash current-meeting)}])}
        (labels :agendas.button/navigate-to-suggestions)]]]]))

(defn single-meeting-view
  "Show a single meeting and all its Agendas."
  []
  [single-meeting])