(ns schnaq.interface.views.discussion.card-view
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [img-path labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.card-elements :as elements]
            [schnaq.interface.views.discussion.input :as input]
            [schnaq.interface.views.meeting.admin-buttons :as admin-buttons]
            [schnaq.interface.views.navbar.user-management :as um]))

(defn- card-meeting-header
  "Overview header for a meeting with a name input"
  [{:discussion/keys [title share-hash] :as discussion}]
  (let [admin-access-map @(rf/subscribe [:meetings/load-admin-access])
        edit-hash (get admin-access-map share-hash)
        history @(rf/subscribe [:discussion-history])
        feed @(rf/subscribe [:feed/get-current])
        feed-route (case feed
                     :personal :routes.meetings/my-schnaqs
                     :routes/public-discussions)]
    [:nav.navbar.navbar-expand-lg.py-3.navbar-dark.context-header.shadow-straight-light
     ;; schnaq logo
     [:a.navbar-brand.mr-auto {:href (reitfe/href feed-route)}
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
      ;; click-able title
      [:div.mr-auto.clickable-no-hover
       {:on-click
        (fn []
          (rf/dispatch [:navigation/navigate :routes.schnaq/start {:share-hash share-hash}])
          (rf/dispatch [:schnaq/select-current discussion]))}
       [toolbelt/desktop-mobile-switch
        [:h3.mx-5 title]
        [:h3.mx-5.display-6 title]]]
      [admin-buttons/txt-export share-hash title]
      (when edit-hash
        [admin-buttons/admin-center share-hash edit-hash])
      ;; name input
      [um/user-handling-dropdown "btn-outline-light"]
      [:div.d-md-none
       [:hr]
       [:h6.text-left (labels :common/history)]
       [:div.row.px-3
        [elements/history-view-mobile history]]]]]))

(defn- discussion-start-view
  "The first step after starting a discussion."
  [{:discussion/keys [title] :as current-discussion}]
  (let [current-starting @(rf/subscribe [:discussion.conclusions/starting])
        input-form [input/input-form "statement-text"]]
    [:<>
     [toolbelt/desktop-mobile-switch
      [elements/discussion-view-desktop
       current-discussion title input-form nil current-starting nil]
      [elements/discussion-view-mobile
       current-discussion title input-form nil current-starting]]]))

(defn- selected-conclusion-view
  "The first step after starting a discussion."
  [current-discussion]
  (let [current-premises @(rf/subscribe [:discussion.premises/current])
        history @(rf/subscribe [:discussion-history])
        current-conclusion (last history)
        title (:statement/content current-conclusion)
        info-content [elements/info-content-conclusion
                      current-conclusion (:discussion/edit-hash current-discussion)]
        input-form [input/input-form "premise-text"]]
    [:<>
     [toolbelt/desktop-mobile-switch
      [elements/discussion-view-desktop
       current-discussion title input-form info-content current-premises history]
      [elements/discussion-view-mobile
       current-discussion title input-form info-content current-premises]]]))

(rf/reg-sub
  :discussion.premises/current
  (fn [db _]
    (get-in db [:discussion :premises :current] [])))

(rf/reg-sub
  :discussion.conclusions/starting
  (fn [db _]
    (get-in db [:discussion :conclusions :starting] [])))

;; -----------------------------------------------------------------------------

(defn- derive-view []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])
        current-route-name @(rf/subscribe [:navigation/current-route-name])]
    [:<>
     [card-meeting-header current-discussion]
     (if (= :routes.schnaq/start current-route-name)
       [discussion-start-view current-discussion]
       [selected-conclusion-view current-discussion])]))

(defn view []
  [derive-view])