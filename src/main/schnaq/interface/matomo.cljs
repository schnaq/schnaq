(ns schnaq.interface.matomo
  "Helper functions to easily track matomo state changes.")

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
