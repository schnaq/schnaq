(ns schnaq.interface.integrations.wetog.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
  :initialize/wetog-integration
  (fn [_ _]
    {:fx [[:dispatch [:how-to-visibility/from-localstorage-to-app-db]]
          ;; TODO Use the JWT from wetog here instead of keycoak
          [:dispatch [:schnaq.discussion-secrets/load-from-localstorage]]]}))