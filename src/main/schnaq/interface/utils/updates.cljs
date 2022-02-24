(ns schnaq.interface.utils.updates
  "Coordinating asynchronous updates."
  (:require [cljs.core.async :refer [go <! timeout]]
            [com.fulcrologic.guardrails.core :refer [>defn-]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [periodic-update-time]]
            [taoensso.timbre :as log]))

(>defn- loop-builder
  "Build looping functions for several update methods and subscriptions."
  [subscription-key update-fn]
  [keyword? fn? :ret any?]
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

(defn- update-graph
  "Call events to update the graph."
  []
  (rf/dispatch [:graph/load-data-for-discussion]))

(defn- loop-update-graph!
  "Define loop to periodically update graph."
  []
  (loop-builder :updates.periodic/graph? update-graph))

(defn- update-polls
  "Call events to update polls."
  []
  (rf/dispatch [:schnaq.polls/load-from-backend]))

(defn- loop-update-polls!
  "Define loop to periodically update polls."
  []
  (loop-builder :updates.periodic/polls? update-polls))

(defn- update-activation
  "Call events to update activation."
  []
  (rf/dispatch [:schnaq.activation/load-from-backend]))

(defn- loop-update-activation!
  "Define loop to periodically update polls."
  []
  (loop-builder :updates.periodic/activation? update-activation))

;; -----------------------------------------------------------------------------
;; Init

(defn init-periodic-updates
  "Initializing function to start the loops. Each looping function must be
  called here once to start the endless loop."
  []
  (log/info "Preparing periodic updates of discussion entities...")
  #_#_#_#_(loop-update-starting-conclusions!)
  (loop-update-graph!)
  (loop-update-polls!)
  (loop-update-activation!))

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

(rf/reg-sub
 :updates.periodic/polls?
 (fn [db _]
   (get-in db [:updates/periodic :polls] false)))

(rf/reg-event-db
 :updates.periodic/polls
 (fn [db [_ trigger?]]
   (assoc-in db [:updates/periodic :polls] trigger?)))

(rf/reg-sub
 :updates.periodic/activation?
 (fn [db _]
   (get-in db [:updates/periodic :activation] false)))

(rf/reg-event-db
 :updates.periodic/activation
 (fn [db [_ trigger?]]
   (assoc-in db [:updates/periodic :activation] trigger?)))

(rf/reg-sub
 :updates.periodic/graph?
 (fn [db _]
   (get-in db [:updates/periodic :graph] false)))

(rf/reg-event-db
 :updates.periodic/graph
 (fn [db [_ trigger?]]
   (assoc-in db [:updates/periodic :graph] trigger?)))
