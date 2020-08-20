(ns meetly.interface.core
  (:require ["@fortawesome/fontawesome-free/js/all.js"]
            ["bootstrap"]
            ["jquery"]
            [goog.dom :as gdom]
            [goog.string :as gstring]
            [meetly.interface.analytics.core]
            [meetly.interface.config :as config]
            [meetly.interface.effects]
            [meetly.interface.events]
            [meetly.interface.routes :as routes]
            [meetly.interface.subs]
            [meetly.interface.views :as views]
            [meetly.interface.views.agenda.agenda]
            [meetly.interface.views.agenda.edit]
            [meetly.interface.views.common]
            [meetly.interface.views.discussion.discussion]
            [meetly.interface.views.discussion.logic]
            [meetly.interface.views.discussion.view-elements]
            [meetly.interface.views.errors]
            [meetly.interface.views.feedback]
            [meetly.interface.views.meeting.after-create]
            [meetly.interface.views.meeting.meetings]
            [meetly.interface.views.meeting.overview]
            [meetly.interface.views.meeting.single]
            [meetly.interface.views.modals.modal]
            [meetly.interface.views.startpage]
            [meetly.interface.views.user]
            [re-frame.core :as rf]
            [reagent.dom]
            [reitit.frontend :as reitit-front]
            [reitit.frontend.easy :as reitit-front-easy]
            [taoensso.timbre :as log]
            [day8.re-frame.http-fx]))
;; NOTE: If you use subs and events in another module, you need to require it
;; Requiring other views is needed to have dynamic updates from shadow and re-frame
;; somewhere where it will be loaded like this core module.

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

(defn- say-hello
  "Add some logging to validate and verify the correct environment."
  []
  (log/info "Welcome to Meetly 🎉")
  (log/info (gstring/format "Build Hash: %s" config/build-hash))
  (log/info (gstring/format "API: %s" config/api-url))
  (log/info (gstring/format "Environment: %s" config/environment)))

(defn init
  "Entrypoint into the application."
  []
  (init-routes!)
  (rf/dispatch-sync [:initialise-db])                       ;; put a value into application state
  (render)                                                  ;; mount the application's ui into '<div id="app" />'
  (say-hello))