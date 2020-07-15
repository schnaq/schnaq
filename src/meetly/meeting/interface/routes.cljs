(ns meetly.meeting.interface.routes
  (:require [meetly.meeting.interface.views :as views]
            [meetly.meeting.interface.views.startpage :as startpage-views]
            [meetly.meeting.interface.views.agenda :as agenda-views]
            [meetly.meeting.interface.views.clock :as clock-views]
            [meetly.meeting.interface.views.meetings :as meeting-views]
            [re-frame.core :as rf]))

;; It is important to note, that we navigate by not calling /meetings for example,
;; but by calling #/meetings. The anchor triggers reitit inside of re-frame instead
;; of hard reloading and dispatching to the web-server.

;; The controllers can be used to execute things at the start and the end of applying
;; the new route.
(def routes
  ["/"
   [""
    {:name :routes/home
     :view views/development-startpage
     :link-text "Home"
     :controllers []}]
   ["meetings"
    ["/"
     {:name :routes/meetings
      :view meeting-views/meetings-list-view
      :link-text "Meetings"
      :controllers []}]
    ["/view/:share-hash"
     {:name :routes/meetings.show
      :view meeting-views/single-meeting-view
      :link-text "Show Meeting"
      :parameters {:path {:share-hash string?}}
      :controllers [{:parameters {:path [:share-hash]}
                     :start (fn [{:keys [path]}]
                              (let [hash (:share-hash path)]
                                (rf/dispatch [:load-meeting-by-share-hash hash])
                                (rf/dispatch [:load-agendas hash])))
                     :stop (fn []
                             (rf/dispatch [:clear-current-agendas]))}]}]
    ["/create"
     {:name :routes/meetings.create
      :view meeting-views/create-meeting-form-view
      :link-text "Create Meeting"}]
    ["/agenda"
     {:name :routes/meetings.agenda
      :view agenda-views/agenda-view
      :link-text "Add Agenda"}]]
   ["clock"
    {:name :routes/clock
     :view clock-views/re-frame-example-view
     :link-text "Clock Re-Frame Example"
     :controllers []}]
   ["startpage"
    {:name :routes/startpage
     :view startpage-views/startpage-view
     :link-text "Meetly"
     :controllers []}]])