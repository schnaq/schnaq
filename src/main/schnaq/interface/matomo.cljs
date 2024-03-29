(ns schnaq.interface.matomo
  "Helper functions to easily track matomo state changes."
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]))

(defn track-event
  "Creates an event and tracks it to matomo."
  ([category action event-name]
   (when-let [matomo js/window._paq]
     (.push matomo #js ["trackEvent" category action event-name])))
  ([category action event-name worth]
   (when-let [matomo js/window._paq]
     (.push matomo #js ["trackEvent" category action event-name worth]))))

(defn track-current-page
  "Tracks the current page its url and description as a site visit."
  []
  (when-let [matomo js/window._paq]
    (.push matomo #js ["setCustomUrl" (oget js/window :location :href)])
    (.push matomo #js ["setDocumentTitle" (oget js/window :document :title)])
    (.push matomo #js ["trackPageView"])))

(defn set-user-id
  "Sets the user-id so users that use different devices can be recognized."
  [user-id]
  (when-let [matomo js/window._paq]
    (.push matomo #js ["setUserId" user-id])))

(defn reset-user-id
  "After the user logged out, remove the user-id from matomo."
  []
  (when-let [matomo js/window._paq]
    (.push matomo #js ["resetUserId"])
    (.push matomo #js ["appendToTrackingUrl" "new_visit=1"])
    (.push matomo #js ["trackPageView"])
    (.push matomo #js ["appendToTrackingUrl" ""])))

(rf/reg-fx
 :matomo/track-event
 (fn [[category action event-name]]
   (track-event category action event-name)))
