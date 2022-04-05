(ns schnaq.interface.views.registration
  (:require [schnaq.interface.views.pages :as pages]))

(defn- start-registration []
  [:h1 "huhu"])

(defn start-registration-view
  "TODO"
  []
  [pages/fullscreen
   {:pages/title "Registrierung"}
   [start-registration]])
