(ns schnaq.interface.core
  (:require ["@fortawesome/fontawesome-free/js/all.js"]
            ["bootstrap"]
            ["jquery"]
            [day8.re-frame.http-fx]
            [goog.dom :as gdom]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [reagent.dom]
            [schnaq.database.specs]
            [schnaq.interface.analytics.core]
            [schnaq.interface.auth]
            [schnaq.interface.config :as config]
            [schnaq.interface.effects]
            [schnaq.interface.events]
            [schnaq.interface.navigation]
            [schnaq.interface.pages.about-us]
            [schnaq.interface.pages.privacy]
            [schnaq.interface.routes :as routes]
            [schnaq.interface.scheduler]
            [schnaq.interface.user]
            [schnaq.interface.utils.file-reader]
            [schnaq.interface.utils.language :as language]
            [schnaq.interface.utils.localstorage]
            [schnaq.interface.utils.time]
            [schnaq.interface.utils.updates :as updates]
            [schnaq.interface.views :as views]
            [schnaq.interface.views.admin.control-center]
            [schnaq.interface.views.common]
            [schnaq.interface.views.discussion.admin-center]
            [schnaq.interface.views.discussion.card-elements]
            [schnaq.interface.views.discussion.card-view]
            [schnaq.interface.views.discussion.conclusion-card]
            [schnaq.interface.views.discussion.history]
            [schnaq.interface.views.discussion.logic]
            [schnaq.interface.views.discussion.search]
            [schnaq.interface.views.errors]
            [schnaq.interface.views.feedback.admin]
            [schnaq.interface.views.feedback.collect]
            [schnaq.interface.views.feedback.survey]
            [schnaq.interface.views.graph.settings]
            [schnaq.interface.views.graph.view]
            [schnaq.interface.views.howto.how-to]
            [schnaq.interface.views.hub.common]
            [schnaq.interface.views.hub.overview]
            [schnaq.interface.views.hub.settings]
            [schnaq.interface.views.loading]
            [schnaq.interface.views.meeting.meetings]
            [schnaq.interface.views.meeting.overview]
            [schnaq.interface.views.modals.modal]
            [schnaq.interface.views.navbar.user-management]
            [schnaq.interface.views.navbar]
            [schnaq.interface.views.notifications]
            [schnaq.interface.views.pages]
            [schnaq.interface.views.schnaq.create]
            [schnaq.interface.views.schnaq.visited]
            [schnaq.interface.views.startpage.core]
            [schnaq.interface.views.user]
            [schnaq.interface.views.user.edit-account]
            [schnaq.interface.views.user.settings]
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
  (render))

(defn- say-hello
  "Add some logging to validate and verify the correct environment."
  []
  (log/info "Welcome to schnaq 🎉")
  (log/info (gstring/format "Build Hash: %s" config/build-hash))
  (log/info (gstring/format "API: %s" config/api-url))
  (log/info (gstring/format "Environment: %s" config/environment))
  (log/info (gstring/format "[Keycloak] Realm: %s, Client: %s" config/keycloak-realm config/keycloak-client)))

(defn init
  "Entrypoint into the application."
  []
  (routes/init-routes!)
  (rf/dispatch-sync [:initialize/schnaq])                   ;; put a value into application state
  (language/init-language)
  (render)                                                  ;; mount the application's ui into '<div id="app" />'
  (say-hello)
  (updates/init-periodic-updates))
