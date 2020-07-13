(ns meetly.meeting.interface.routes
  (:require [meetly.meeting.interface.views :as views]
            [meetly.meeting.interface.views.startpage :as startpage-views]
            [meetly.meeting.interface.views.agenda :as agenda-views]))

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
      :view views/meetings-list
      :link-text "Meetings"
      :controllers []}]
    ["/create"
     {:name :routes/meetings.create
      :view views/create-meeting-form
      :link-text "Create Meeting"}]
    ["/agenda"
     {:name :routes/meetings.agenda
      :view agenda-views/agenda-view
      :link-text "Add Agenda"}]]
   ["clock"
    {:name :routes/clock
     :view views/re-frame-example-view
     :link-text "Clock Re-Frame Example"
     :controllers []}]
   ["startpage"
    {:name :routes/startpage
     :view startpage-views/startpage-view
     :link-text "Meetly"
     :controllers []}]])