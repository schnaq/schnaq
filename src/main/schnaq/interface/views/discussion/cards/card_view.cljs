(ns schnaq.interface.views.discussion.cards.card-view
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [img-path labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.brainstorm.tools :as btools]
            [schnaq.interface.views.discussion.cards.card-elements :as elements]
            [schnaq.interface.views.meeting.admin-buttons :as admin-buttons]
            [schnaq.interface.views.navbar :as navbar]))

(defn- card-meeting-header
  "Overview header for a meeting with a name input"
  [{:meeting/keys [title share-hash] :as meeting}]
  (let [admin-access-map @(rf/subscribe [:meetings/load-admin-access])
        edit-hash (get admin-access-map share-hash)
        history @(rf/subscribe [:discussion-history])]
    [:nav.navbar.navbar-expand-lg.py-3.navbar-dark.context-header.shadow-straight-light
     ;; schnaq logo
     [:a.navbar-brand.mr-auto {:href (reitfe/href :routes/startpage)}
      [:img.d-inline-block.align-middle.mr-2
       {:src (img-path :logo-white) :width "150" :alt "schnaq logo"}]]
     ;; hamburger
     [:button.navbar-toggler
      {:type "button" :data-toggle "collapse" :data-target "#schnaq-navbar"
       :aria-controls "schnaq-navbar" :aria-expanded "false" :aria-label "Toggle navigation"
       :data-html2canvas-ignore true}
      [:span.navbar-toggler-icon]]
     ;; menu items
     [:div {:id "schnaq-navbar" :class "collapse navbar-collapse"}
      ;; clickable title
      [:div.mr-auto.clickable-no-hover
       {:on-click
        (fn []
          (rf/dispatch [:navigation/navigate :routes.meeting/show
                        {:share-hash share-hash}])
          (rf/dispatch [:meeting/select-current meeting]))}
       [toolbelt/desktop-mobile-switch
        [:h3.mx-5 title]
        [:h3.mx-5.display-6 title]]]
      [admin-buttons/txt-export share-hash title]
      (when (and edit-hash (btools/is-brainstorm? meeting))
        [admin-buttons/admin-center share-hash edit-hash])
      ;; name input
      [navbar/username-bar-view-light]
      [:div.d-md-none.mobile-history
       [:hr]
       [:h6.text-center.py-1 (labels :common/history)]
       [elements/history-view meeting history]]]]))

(rf/reg-sub
  :discussion.conclusions/starting
  (fn [db _]
    (get-in db [:discussion :conclusions :starting] [])))

(defn- discussion-start-view
  "The first step after starting a discussion."
  []
  (let [{:meeting/keys [title] :as current-meeting} @(rf/subscribe [:meeting/selected])
        current-starting @(rf/subscribe [:discussion.conclusions/starting])
        input-desktop [elements/input-starting-statement-form "input-statement-id-desktop"]
        input-mobile [elements/input-starting-statement-form "input-statement-id-mobile"]]
    [:<>
     [card-meeting-header current-meeting]
     [:div.container-fluid.px-0
      [toolbelt/desktop-mobile-switch
       [elements/discussion-view-desktop
        current-meeting title input-desktop
        nil current-starting nil]
       [elements/discussion-view-mobile
        current-meeting title input-mobile
        nil current-starting]]]]))

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
     [card-meeting-header current-meeting]
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