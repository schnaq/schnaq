(ns meetly.meeting.interface.routes
  (:require [re-frame.core :as rf]
            [meetly.meeting.interface.views :as views]))

;; It is important to note, that we navigate by not calling /meetings for example,
;; but by calling #/meetings. The anchor triggers reitit inside of re-frame instead
;; of hard reloading and dispatching to the web-server.
(def routes
  ["/"
   [""
    {:name :routes/home
     :view views/ui
     :link-text "Home"
     :controllers
     [{:start (fn []
                (println "Entering home page"))
       :stop (fn []
               (println "Leaving home page"))}]}]
   ["meetings"
    {:name :routes/meetings
     :view views/create-meeting-form
     :link-text "Meetings"
     :controllers
     [{:start (fn []
                (println "Entering meetings page"))
       :stop (fn []
               (println "Leaving meetings page"))}]}]])