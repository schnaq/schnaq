(ns meetly.interface.core
  (:require [day8.re-frame.http-fx]
            [goog.dom :as gdom]
            [goog.string :as gstring]
            [reagent.dom]
            ["jquery"]
            ["bootstrap"]
            ["@fortawesome/fontawesome-free/js/all.js"]
            [re-frame.core :as rf]
            [reitit.frontend :as reitit-front]
            [reitit.frontend.easy :as reitit-front-easy]
            [meetly.interface.config :as config]
            [meetly.interface.views :as views]
    ;; Requiring other views is needed to have dynamic updates from shadow and re-frame
            [meetly.interface.views.startpage]
            [meetly.interface.views.agenda.agenda]
            [meetly.interface.views.agenda.edit]
            [meetly.interface.views.common]
            [meetly.interface.views.feedback]
            [meetly.interface.views.meeting.meetings]
            [meetly.interface.views.meeting.after-create]
            [meetly.interface.views.meeting.overview]
            [meetly.interface.views.meeting.single]
            [meetly.interface.views.discussion.discussion]
            [meetly.interface.views.discussion.view-elements]
            [meetly.interface.views.discussion.logic]
            [meetly.interface.subs]
            [meetly.interface.views.errors]
            [meetly.interface.events]
            [meetly.interface.effects]
            [meetly.interface.views.modals.modal]
            [meetly.interface.analytics.core]
    ;; IMPORTANT: If you use subs and events in another module, you need to require it
    ;; somewhere where it will be loaded like this core module.
            [meetly.interface.routes :as routes]
            [taoensso.timbre :as log]))

;; -- Entry Point -------------------------------------------------------------

(defn render
  []
  (reagent.dom/render [views/root]
                      (gdom/getElement "app")))

(def router
  (reitit-front/router
    routes/routes))

(defn on-navigate [new-match]
  (when new-match
    (rf/dispatch [:navigated new-match])))

(defn init-routes! []
  (reitit-front-easy/start!
    router
    on-navigate
    {:use-fragment true}))

(defn ^:dev/after-load clear-cache-and-render!
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code. We force a UI update by clearing
  ;; the Reframe subscription cache.
  (rf/clear-subscription-cache!)
  (init-routes!)
  (render))

(defn say-hello
  "Add some logging to validate and verify the correct environment."
  []
  (log/info "Welcome to Meetly 🎉")
  (log/info (gstring/format "API: \t\t\t%s" config/rest-api-url))
  (log/info (gstring/format "Environment: \t%s" config/environment)))

(defn init
  "Entrypoint into the application."
  []
  (init-routes!)
  (rf/dispatch-sync [:initialise-db])                       ;; put a value into application state
  (render)
  (say-hello))                                              ;; mount the application's ui into '<div id="app" />'