(ns schnaq.interface.views.meeting.single
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.markdown-parser :as markdown-parser]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))


(defn- tooltip-button
  [tooltip-location tooltip content on-click-fn]
  (reagent/create-class
    {:component-did-mount
     (fn [comp] (js-wrap/tooltip (rdom/dom-node comp)))
     :component-will-unmount
     (fn [comp]
       (js-wrap/tooltip (rdom/dom-node comp) "disable")
       (js-wrap/tooltip (rdom/dom-node comp) "dispose"))
     :reagent-render
     (fn [] [:button.button-secondary-b-1.button-md.my-2.mx-3
             {:on-click on-click-fn
              :data-toggle "tooltip"
              :data-placement tooltip-location
              :title tooltip} content])}))

(defn control-buttons []
  (let [share-hash (get-in @(rf/subscribe [:navigation/current-route])
                           [:parameters :path :share-hash])
        admin-access-map @(rf/subscribe [:meetings/load-admin-access])
        edit-hash (get admin-access-map share-hash)]
    [:div.text-center
     ;; admin panel button
     (when edit-hash
       [tooltip-button "bottom"
        (labels :meeting/admin-center-tooltip)
        [:i {:class (str "m-auto fas " (fa :cog))}]
        #(rf/dispatch [:navigation/navigate
                       :routes.meeting/admin-center
                       {:share-hash share-hash :edit-hash edit-hash}])])
     ;; suggestion button
     [tooltip-button "bottom"
      (labels :agendas.button/navigate-to-suggestions)
      [:i {:class (str "m-auto fas " (fa :eraser))}]
      #(rf/dispatch [:navigation/navigate :routes.meeting/suggestions
                     {:share-hash share-hash}])]]))

(defn meeting-entry
  "Non wavy header with an optional back button.
  'title-on-click-function' is triggered when header is clicked
  'on-click-back-function' is triggered when back button is clicked,when no on-click-back-function is provided the back button will not be displayed"
  [title subtitle on-click-back-function]
  [:div.row.meeting-header.shadow-straight.m-0
   ;; arrow column
   [:div.col-md-1.back-arrow
    (when on-click-back-function
      [:p {:on-click on-click-back-function}
       [:i.arrow-icon {:class (str "m-auto fas " (fa :arrow-left))}]])]
   [:div.col-md-10
    [:div.container.px-4
     [:h1 title]
     [:hr]
     ;; mark down
     [markdown-parser/markdown-to-html subtitle]]]
   ;; button column
   [:div.col-md-1
    [control-buttons]]])

(defn- agenda-entry [agenda meeting]
  [:div.card.meeting-entry-no-hover
   ;; title
   [:div.meeting-entry-title
    [:h4 (:agenda/title agenda)]]
   ;; description
   [:div.meeting-entry-desc
    [:hr]
    [markdown-parser/markdown-to-html (:agenda/description agenda)]]
   [:div
    [:button.button-secondary-b-1.button-md
     {:title (labels :discussion/discuss-tooltip)
      :on-click (fn []
                  (rf/dispatch [:navigation/navigate :routes.discussion/start
                                {:id (-> agenda :agenda/discussion :db/id)
                                 :share-hash (:meeting/share-hash meeting)}])
                  (rf/dispatch [:agenda/choose agenda]))}
     [:span.pr-2 (labels :discussion/discuss)]
     [:i {:class (str "m-auto fas " (fa :comment))}]]]])


(defn agenda-in-meeting-view
  "The view of an agenda which gets embedded inside a meeting view."
  [meeting]
  [:<>
   (let [agendas @(rf/subscribe [:current-agendas])]
     (for [agenda agendas]
       [:div.py-3 {:key (:db/id agenda)}
        [agenda-entry agenda meeting]]))])

(defn- meeting-title [current-meeting]
  ;; meeting header
  [meeting-entry
   (:meeting/title current-meeting)
   (:meeting/description current-meeting)
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