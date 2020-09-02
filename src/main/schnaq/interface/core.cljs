(ns schnaq.interface.core
  (:require ["@fortawesome/fontawesome-free/js/all.js"]
            ["bootstrap"]
            ["jquery"]
            [day8.re-frame.http-fx]
            [goog.dom :as gdom]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [reagent.dom]
            [schnaq.interface.analytics.core]
            [schnaq.interface.config :as config]
            [schnaq.interface.effects]
            [schnaq.interface.events]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.routes]
            [schnaq.interface.views :as views]
            [schnaq.interface.views.agenda.agenda]
            [schnaq.interface.views.agenda.edit]
            [schnaq.interface.views.common]
            [schnaq.interface.views.discussion.discussion]
            [schnaq.interface.views.discussion.logic]
            [schnaq.interface.views.discussion.view-elements]
            [schnaq.interface.views.errors]
            [schnaq.interface.views.feedback]
            [schnaq.interface.views.graph.view]
            [schnaq.interface.views.meeting.after-create]
            [schnaq.interface.views.meeting.meetings]
            [schnaq.interface.views.meeting.overview]
            [schnaq.interface.views.meeting.single]
            [schnaq.interface.views.modals.modal]
            [schnaq.interface.views.notifications]
            [schnaq.interface.views.startpage]
            [schnaq.interface.views.user]
            [taoensso.timbre :as log]))
;; NOTE: If you use subs and events in another module, you need to require it
;; Requiring other views is needed to have dynamic updates from shadow and re-frame
;; somewhere where it will be loaded like this core module.

;; -- Entry Point -------------------------------------------------------------

(defn render
  []
  (reagent.dom/render [views/root]
                      (gdom/getElement "app")))

(defn ^:dev/after-load clear-cache-and-render!
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code. We force a UI update by clearing
  ;; the Reframe subscription cache.
  (rf/clear-subscription-cache!)
  (navigation/init-routes!)
  (render))

(defn- say-hello
  "Add some logging to validate and verify the correct environment."
  []
  (log/info "Welcome to schnaq 🎉")
  (log/info (gstring/format "Build Hash: %s" config/build-hash))
  (log/info (gstring/format "API: %s" config/api-url))
  (log/info (gstring/format "Environment: %s" config/environment)))

(defn init
  "Entrypoint into the application."
  []
  (navigation/init-routes!)
  (rf/dispatch-sync [:initialise-db])                       ;; put a value into application state
  (render)                                                  ;; mount the application's ui into '<div id="app" />'
  (say-hello))