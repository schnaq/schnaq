(ns meetly.meeting.interface.views
  (:require [reagent.dom]
            [meetly.meeting.interface.text.display-data :as data]
            [meetly.meeting.interface.views.base :as base]
            [reitit.frontend.easy as reitfe]
            [re-frame.core :as rf]))

(defn navigation-button
  "Navigates you via frontend-routing to the desired `route`."
  [route label]
  [:input
   {:on-click #(rf/dispatch [:navigate route])
    :type "button"
    :value label
    :style {:margin-bottom "1em"}}])

(defn development-startpage
  "This is the startpage during development. We can treat it a little bit similar
  to workspaces or devcards. Just use reitit to navigate to the subsystem you are
  working on from here."
  []
  [:div
   [:h2 "Examples"]
   (navigation-button :routes/clock "--> Re-Frame Clock example")
   [:h2 "Meetings-Related views"]
   (navigation-button :routes/meetings "--> Show Meetings View")
   (navigation-button :routes/meetings.create "--> Create Meetly View")
   (navigation-button :routes/meetings.agenda "--> Create Agendas View")
   [:h2 "Startpage"]
   (navigation-button :routes/startpage "--> Startpage")])


(defn- header []
  ;; collapsable navbar
  [:nav.navbar.navbar-expand-lg.py-3.navbar-light.bg-light
   ;; logo
   [:div.container
    [:a.navbar-brand {:href "#/startpage"}
     [:img.d-inline-block.align-middle.mr-2 {:src (data/img-path :logo) :width "150" :alt ""}]]
    ;; hamburger
    [:button.navbar-toggler
     {:type "button" :data-toggle "collapse" :data-target "#navbarSupportedContent"
      :aria-controls "navbarSupportedContent" :aria-expanded "false" :aria-label "Toggle navigation"}
     [:span.navbar-toggler-icon]]
    ;; menu items
    [:div
     {:id "navbarSupportedContent"
      :class "collapse navbar-collapse"}
     [:ul.navbar-nav.ml-auto
      ;[:li.nav-item.active [:a.nav-link {:href "#"} "Home" [:span.sr-only "(current)"]]]
      [:li.nav-item [:a.nav-link {:href (reitfe/href :routes/startpage)} (data/labels :nav-startpage)]]
      [:li.nav-item [:a.nav-link {:href (reitfe/href :routes/clock)} (data/labels :nav-example)]]
      [:li.nav-item [:a.nav-link {:href (reitfe/href :routes/meetings)} (data/labels :nav-meeting)]]
      [:li.nav-item [:a.nav-link {:href (reitfe/href :routes/meetings.create)} (data/labels :nav-meeting-create)]]
      [:li.nav-item [:a.nav-link {:href (reitfe/href :routes/meetings.agenda)} (data/labels :nav-meeting-agenda)]]
      [:li.nav-item [:a.nav-link {:href (reitfe/href :routes/home)} (data/labels :nav-overview)]]]]]])

(defn- base-page
  []
  (let [current-route @(rf/subscribe [:current-route])
        errors @(rf/subscribe [:error-occurred])
        ajax-error (:ajax errors)]
    [:div#display-content
     [header]
     (when ajax-error
       [:h1 "Error: " ajax-error])
     (when current-route
       [(-> current-route :data :view)])
     ]))


(defn- footer []
  [base/footer])


(defn root []
  [:div#root
   [base-page]
   [footer]])
