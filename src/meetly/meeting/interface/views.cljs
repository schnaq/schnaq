(ns meetly.meeting.interface.views
  (:require [reagent.dom]
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
     [:img.d-inline-block.align-middle.mr-2 {:src "imgs/logo.png" :width "140" :alt ""}]]
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
      [:a.nav-link {:href "#/startpage"} "Home" [:span.sr-only "(current)"]]
      [:li.nav-item [:a.nav-link {:href "#/clock"} "Examples"]]
      [:li.nav-item [:a.nav-link {:href "#/meetings/"} "Show Meetings"]]
      [:li.nav-item [:a.nav-link {:href "#/meetings/create"} "Create Meeting"]]
      [:li.nav-item [:a.nav-link {:href "#/meetings/agenda"} "Create Agenda"]]
      [:li.nav-item [:a.nav-link {:href "#"} "Overview"]]]]]])

(defn- base-page
  []
  (let [current-route @(rf/subscribe [:current-route])
        errors @(rf/subscribe [:error-occurred])
        ajax-error (:ajax errors)]
    [:div

     [header]
     [:div.container

      (when ajax-error
        [:h1 "Error: " ajax-error])
      (when current-route
        [(-> current-route :data :view)])]]))

(defn root []
  [:div#root
   [base-page]])
