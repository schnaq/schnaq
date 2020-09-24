(ns schnaq.interface.views.meeting.single
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.markdown-parser :as markdown-parser]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.base :as base]
            [reagent.core :as reagent]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))


(defn- tooltip-button
  [id content on-click-fn]
  (reagent/create-class
    {:component-did-mount
     (fn [_] (js-wrap/tooltip (str "#" id)))
     :component-will-unmount
     (fn [_]
       (js-wrap/tooltip (str "#" id) "disable")
       (js-wrap/tooltip (str "#" id) "dispose"))
     :reagent-render
     (fn [] [:button.btn.button-secondary.button-md.my-2
             {:on-click on-click-fn
              :id id
              :data-toggle "tooltip"
              :data-placement "bottom"
              :title (labels :agendas.button/navigate-to-suggestions)} content])}))

(defn control-buttons [share-hash]
  [:div.text-center
   [tooltip-button "request-change"
    [:i {:class (str "m-auto fas " (fa :eraser))}]
    #(rf/dispatch [:navigation/navigate :routes.meeting/suggestions
                   {:share-hash share-hash}])]])

(defn meeting-entry
  "Non wavy header with an optional back button.
  'title-on-click-function' is triggered when header is clicked
  'on-click-back-function' is triggered when back button is clicked,when no on-click-back-function is provided the back button will not be displayed"
  ([title subtitle share-hash on-click-back-function]
   ;; check if title is clickable and set properties accordingly
   [:div.meeting-header.header-meeting.shadow-straight
    [:div.row
     ;; arrow column
     [:div.col-md-3.back-arrow
      (when on-click-back-function
        [:p {:on-click on-click-back-function}              ;; the icon itself is not clickable
         [:i.arrow-icon {:class (str "m-auto fas " (fa :arrow-left))}]])]
     [:div.col-md-6
      ;[:div.container]
      [:h1 title]
      [:hr]
      ;; mark down
      [markdown-parser/markdown-to-html subtitle]]
     ;; button column
     [:div.col-md-3
      [control-buttons share-hash]]]]))

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
    [markdown-parser/markdown-to-html (:agenda/description agenda)]]])


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
  [meeting-entry
   (:meeting/title current-meeting)
   (:meeting/description current-meeting)
   (:meeting/share-hash current-meeting)
   (when-not toolbelt/production?                           ;; when in dev display back button
     (fn []
       (rf/dispatch [:navigation/navigate :routes/meetings])))])

(defn- single-meeting []
  (let [current-meeting @(rf/subscribe [:meeting/selected])]
    ;; meeting header
    [:div
     [base/meeting-header current-meeting]
     [meeting-title current-meeting]
     [:div.container.py-2
      [:div.meeting-single-rounded
       ;; list agendas
       [agenda-in-meeting-view current-meeting]]]]))

(defn single-meeting-view
  "Show a single meeting and all its Agendas."
  []
  [single-meeting])