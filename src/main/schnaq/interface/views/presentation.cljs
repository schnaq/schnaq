(ns schnaq.interface.views.presentation
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.common :refer [schnaq-logo]]))

(rf/reg-event-fx
 :view/present
 (fn []
   {}))

(defn- fullscreen
  "Full screen view of a component."
  []
  (let [{:poll/keys [title]} @(rf/subscribe [:present/poll])]
    [:div.container.pt-3
     [:div.display-6.text-center.pb-3
      "Gehe auf "
      [:u.fw-bolder "schnaq.com/hubbattle"]
      " und nimm am Ranking teil!"]
     [schnaq-logo {:style {:width "200px"}}]
     [:h1.pt-3 title]]))

;; -----------------------------------------------------------------------------

(defn view []
  [fullscreen])

;; -----------------------------------------------------------------------------

(rf/reg-sub
 :present/poll
 (fn [db]
   (get-in db [:present :poll])))
