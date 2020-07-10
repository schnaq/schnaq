(ns meetly.meeting.interface.core
  (:require [day8.re-frame.http-fx]
            [reagent.dom]
            [re-frame.core :as rf]
            [meetly.meeting.interface.views :as views]
            [meetly.meeting.interface.subs]
            [meetly.meeting.interface.events]))
;; IMPORTANT: If you use subs and events in another module, you need to require it
;; somewhere where it will be loaded like this core module.

;; -- Entry Point -------------------------------------------------------------

(defn dispatch-timer-event
  []
  (let [now (js/Date.)]
    (rf/dispatch [:timer now])))                            ;; <-- dispatch used

;; Call the dispatching function every second.
;; `defonce` is like `def` but it ensures only one instance is ever
;; created in the face of figwheel hot-reloading of this file.
(defonce do-timer (js/setInterval dispatch-timer-event 1000))

(defn render
  []
  (reagent.dom/render [views/ui]
                      (js/document.getElementById "app")))

(defn ^:dev/after-load clear-cache-and-render!
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code. We force a UI update by clearing
  ;; the Reframe subscription cache.
  (rf/clear-subscription-cache!)
  (render))

(defn init
  []
  (rf/dispatch-sync [:initialise-db])                       ;; put a value into application state
  (render)                                                  ;; mount the application's ui into '<div id="app" />'
  )