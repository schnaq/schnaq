(ns schnaq.interface.utils.updates
  "Coordinating asynchronous updates."
  (:require [cljs.core.async :refer [go <! timeout]]
            [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [periodic-update-time]]
            [taoensso.timbre :as log]))

(>defn- loop-builder
  "Build looping functions for several update methods and subscriptions."
  [subscription-key update-fn]
  [keyword? fn? :ret nil?]
  (go (while true
        (<! (timeout periodic-update-time))
        (when @(rf/subscribe [subscription-key])
          (update-fn)))))


;; -----------------------------------------------------------------------------
;; Looping functions

(defn- update-starting-conclusions
  "Function to trigger updates of starting-conclusions."
  []
  (rf/dispatch [:discussion.query.conclusions/starting]))

(defn- loop-update-starting-conclusions!
  "Periodically request starting conclusions."
  []
  (loop-builder
    :updates.periodic/starting-conclusions? update-starting-conclusions))


;; -----------------------------------------------------------------------------
;; Init

(defn init-periodic-updates
  "Initializing function to start the loops. Each looping function must be
  called here once to start the endless loop."
  []
  (log/info "Preparing periodic updates of discussion entities...")
  (loop-update-starting-conclusions!))


;; -----------------------------------------------------------------------------
;; Events

(rf/reg-sub
  :updates.periodic/starting-conclusions?
  (fn [db _]
    (get-in db [:updates/periodic :conclusions/starting?] false)))

(rf/reg-event-db
  :updates.periodic/starting-conclusions
  (fn [db [_ trigger?]]
    (assoc-in db [:updates/periodic :conclusions/starting?] trigger?)))