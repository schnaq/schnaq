(ns schnaq.interface.views.meeting.admin-buttons
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.meeting.calendar-invite :as calendar-invite]))

(defn tooltip-button
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

(defn admin-center
  "Button to access admin menu."
  [share-hash edit-hash]
  [tooltip-button "bottom"
   (labels :meeting/admin-center-tooltip)
   [:i {:class (str "m-auto fas " (fa :cog))}]
   #(rf/dispatch [:navigation/navigate
                  :routes.meeting/admin-center
                  {:share-hash share-hash :edit-hash edit-hash}])])

(defn edit
  "Button to enter edit-mode."
  [share-hash edit-hash]
  [tooltip-button "bottom"
   (labels :meetings/edit-schnaq-button)
   [:i {:class (str "m-auto fas " (fa :eraser))}]
   #(rf/dispatch [:navigation/navigate
                  :routes.meeting/edit
                  {:share-hash share-hash :edit-hash edit-hash}])])

(defn calendar-invite
  "Button for calendar invitations."
  []
  [tooltip-button "bottom"
   (labels :meetings/share-calendar-invite)
   [:i {:class (str "m-auto fas " (fa :calendar))}]
   #(rf/dispatch [:modal {:show? true :large? false
                          :child [calendar-invite/modal]}])])

(defn provide-suggestion
  "Button to add suggestions."
  [share-hash]
  [tooltip-button "bottom"
   (labels :agendas.button/navigate-to-suggestions)
   [:i {:class (str "m-auto fas " (fa :comment-alt))}]
   #(rf/dispatch [:navigation/navigate :routes.meeting/suggestions
                  {:share-hash share-hash}])])