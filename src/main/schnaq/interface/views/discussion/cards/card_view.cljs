(ns schnaq.interface.views.discussion.cards.card-view
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [img-path]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.discussion.cards.card-elements :as elements]
            [schnaq.interface.views.navbar :as navbar]))

(defn- card-meeting-header
  "Overview header for a meeting with a name input"
  []
  [:nav.navbar.navbar-expand-lg.py-3.navbar-dark.context-header.shadow-straight-light
   ;; schnaq logo
   [:a.navbar-brand.mr-auto {:href (reitfe/href :routes/startpage)}
    [:img.d-inline-block.align-middle.mr-2
     {:src (img-path :logo-white) :width "150" :alt "schnaq logo"}]]
   ;; name input
   [:div.float-right
    [navbar/username-bar-view-light]]])

(defn- discussion-start-view
  "The first step after starting a discussion."
  []
  (let [{:meeting/keys [edit-hash title] :as current-meeting} @(rf/subscribe [:meeting/selected])
        current-starting @(rf/subscribe [:discussion.conclusions/starting])
        settings-content [elements/settings-element current-meeting edit-hash]
        input-desktop [elements/input-starting-statement-form "input-statement-id-desktop"]
        input-mobile [elements/input-starting-statement-form "input-statement-id-mobile"]]
    [:<>
     [card-meeting-header current-meeting]
     [:div.container-fluid.px-0
      [toolbelt/desktop-mobile-switch
       [elements/discussion-view-desktop
        current-meeting title input-desktop
        settings-content current-starting nil]
       [elements/discussion-view-mobile
        current-meeting title input-mobile
        settings-content current-starting]]]]))

(defn- selected-conclusion-view
  "The first step after starting a discussion."
  []
  (let [current-meeting @(rf/subscribe [:meeting/selected])
        current-premises @(rf/subscribe [:discussion.premises/current])
        history @(rf/subscribe [:discussion-history])
        current-conclusion (last history)
        title (:statement/content current-conclusion)
        info-content [elements/info-content-conclusion
                      current-conclusion (:meeting/edit-hash current-meeting)]
        input-desktop [elements/input-conclusion-form "input-statement-id-desktop"]
        input-mobile [elements/input-conclusion-form "input-statement-id-mobile"]]
    [:<>
     [base/meeting-header current-meeting]
     [:div.container-fluid.px-0
      [toolbelt/desktop-mobile-switch
       [elements/discussion-view-desktop
        current-meeting title input-desktop
        info-content current-premises history]
       [elements/discussion-view-mobile
        current-meeting title input-mobile
        info-content current-premises]]]]))

(defn discussion-start-view-entrypoint []
  [discussion-start-view])

(defn selected-conclusion []
  [selected-conclusion-view])