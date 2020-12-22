(ns schnaq.interface.views.meeting.overview
  (:require [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.markdown-parser :as markdown-parser]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.pages :as pages]))

(defn- no-meetings-found
  "Show error message when no meetings were loaded."
  []
  [:div.alert.alert-primary.text-center
   [:p.lead
    "🙈 "
    (labels :schnaqs.not-found/alert-lead)]
   [:p (labels :schnaqs.not-found/alert-body)]
   [:div.btn.btn-outline-primary
    {:on-click #(rf/dispatch [:navigation/navigate :routes.brainstorm/create])}
    (labels :nav.schnaqs/create-brainstorm)]])


;; -----------------------------------------------------------------------------

(defn- meeting-entry
  "Displays a single meeting element of the meeting list"
  [meeting]
  ;; clickable div
  [:div.meeting-entry
   {:on-click (fn []
                (rf/dispatch [:navigation/navigate :routes.meeting/show
                              {:share-hash (:meeting/share-hash meeting)}])
                (rf/dispatch [:meeting/select-current meeting]))}
   ;; title and arrow
   [:div.meeting-entry-title
    [:div.row
     [:div.col-lg-11.px-3
      [:h3 (:meeting/title meeting)]]
     [:div.col-lg-1
      [:i.arrow-icon {:class (str "m-auto fas " (fa :arrow-right))}]]]]
   ;; description / body
   [:div.meeting-entry-desc
    [:hr]
    [:small.text-right.float-right.py-3
     (when-let [nickname (-> meeting :meeting/author :author/nickname)]
       [common/avatar-with-nickname nickname 50])]
    [markdown-parser/markdown-to-html (:meeting/description meeting)]]])

(defn- meetings-list-view
  "Shows a list of all meetings."
  [subscription-key]
  [:div.meetings-list
   (let [meetings @(rf/subscribe [subscription-key])]
     (if (empty? meetings)
       [no-meetings-found]
       (for [meeting meetings]
         [:div.py-3 {:key (:db/id meeting)}
          [meeting-entry meeting]])))])

(>defn- meeting-view
  "Shows the page for an overview of meetings. Takes a subscription-key which
  must be a keyword referring to a subscription, which returns a collection of
  meetings."
  [subscription-key]
  [keyword? :ret vector?]
  [pages/with-nav-and-header
   {:page/heading (labels :meetings/header)
    :page/subheading (labels :meetings/subheader)}
   [:div.container.py-4
    [meetings-list-view subscription-key]]])

(defn meeting-view-entry
  "Render all meetings."
  []
  [meeting-view :meetings/all])

(defn meeting-view-visited
  "Render visited meetings."
  []
  [meeting-view :meetings.visited/all])

;; #### Subs ####

(rf/reg-sub
  :meetings/all
  (fn [db _]
    (get-in db [:meetings :all])))
