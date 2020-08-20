(ns meetly.interface.subs
  (:require [re-frame.core :as rf]))

;; This sub helps change the view to the correct route
(rf/reg-sub
  :current-route
  (fn [db]
    (:current-route db)))