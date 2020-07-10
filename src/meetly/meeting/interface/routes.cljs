(ns meetly.meeting.interface.routes
  (:require [re-frame.core :as rf]
            [meetly.meeting.interface.views :as views]))


;; TODO the views here are not existing and the rest is just copied as of this writing
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
   ["files"
    {:name :routes/files
     :view views/create-meeting-form
     :link-text "Files"
     :controllers
     [{:start (fn []
                (println "Entering files page")
                (rf/dispatch [::events/fetch-files]))
       :stop (fn []
               (println "Leaving files page"))}]}]])