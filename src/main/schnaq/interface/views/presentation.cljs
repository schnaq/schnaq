(ns schnaq.interface.views.presentation
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :view/present
 (fn []
   {}))

(defn- fullscreen
  "Full screen view of a component."
  []
  [:div
   ]
  [:h1 "HALLOQQQQ"])

;; -----------------------------------------------------------------------------

(defn view []
  [fullscreen])
