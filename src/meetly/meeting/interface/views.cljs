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

(defn main-page
  []
  (let [current-route @(rf/subscribe [:current-route])
        errors @(rf/subscribe [:error-occurred])
        ajax-error (:ajax errors)]
    [:div
     (when ajax-error
       [:h1 "Error: " ajax-error])
     (when current-route
       [(-> current-route :data :view)])]))

(defn root []
  [:div#root
   [main-page]])
