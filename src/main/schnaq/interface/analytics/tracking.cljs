(ns schnaq.interface.analytics.tracking
  "Helper functions to easily track events."
  (:require ["@vercel/analytics" :refer [track]]
            [re-frame.core :as rf]))

(defn track-event
  "Creates an event and tracks it."
  ([category action event-name]
   (track event-name {:category category :action action}))
  ([category action event-name _worth]
   (track event-name {:category category :action action})))

(rf/reg-fx
 :analytics/track-event
 (fn [[category action event-name]]
   (track-event category action event-name)))
