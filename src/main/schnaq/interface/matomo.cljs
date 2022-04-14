(ns schnaq.interface.matomo
  "Helper functions to easily track matomo state changes."
  (:require [oops.core :refer [oget]]))

(defn track-event
  "Creates an event and tracks it to matomo."
  ([category]
   (.push js/window._paq #js ["trackEvent" category]))
  ([category subcategory]
   (.push js/window._paq #js ["trackEvent" category subcategory]))
  ([category subcategory tag]
   (.push js/window._paq #js ["trackEvent" category subcategory tag]))
  ([category subcategory tag worth]
   (.push js/window._paq #js ["trackEvent" category subcategory tag worth])))

(defn track-current-page
  "Tracks the current page its url and description as a site visit."
  []
  (let [matomo js/window._paq]
    (.push matomo #js ["setCustomUrl" (oget js/window :location :href)])
    (.push matomo #js ["setDocumentTitle" (oget js/window :document :title)])
    (.push matomo #js ["trackPageView"])))

(defn set-user-id
  "Sets the user-id so users that use different devices can be recognized."
  [user-id]
  (.push js/window._paq #js ["setUserId" user-id]))

(defn reset-user-id
  "After the user logged out, remove the user-id from matomo."
  []
  (let [matomo js/window._paq]
    (.push matomo #js ["resetUserId"])
    (.push matomo #js ["appendToTrackingUrl" "new_visit=1"])
    (.push matomo #js ["trackPageView"])
    (.push matomo #js ["appendToTrackingUrl" ""])))
