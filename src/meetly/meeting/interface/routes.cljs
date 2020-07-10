(ns meetly.meeting.interface.routes
  (:require [re-frame.core :as rf]))


;; TODO the views here are not existing and the rest is just copied as of this writing
(def routes
  ["/"
   [""
    {:name :routes/home
     :view home/page
     :link-text "Home"
     :controllers
     [{:start (fn []
                (println "Entering home page"))
       :stop (fn []
               (println "Leaving home page"))}]}]
   ["files"
    {:name :routes/files
     :view files/files-page
     :link-text "Files"
     :controllers
     [{:start (fn []
                (println "Entering files page")
                (rf/dispatch [::events/fetch-files]))
       :stop (fn []
               (println "Leaving files page"))}]}]
   ["files/:id"
    {:name :routes/file
     :view files/file-page
     :link-text "Files"
     :coercion reitit.coercion.malli/coercion
     :params {:path [:map [:id string?]]}
     :controllers
     [{:parameters {:path [:id]}
       :start (fn [{:keys [path]}]
                (let [file-id (:id path)]
                  (println "Entering files/:id page for id" file-id)
                  (rf/dispatch [::events/fetch-files])
                  (rf/dispatch [::events/set-active-file-id file-id])))
       :stop (fn []
               (println "Leaving files page"))}]}]])