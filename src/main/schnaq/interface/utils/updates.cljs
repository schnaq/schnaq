(ns schnaq.interface.utils.updates
  "Coordinating asynchronous updates."
  (:require [cljs.core.async :refer [go <! timeout]]
            [com.fulcrologic.guardrails.core :refer [>defn-]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [periodic-update-time]]
            [schnaq.interface.utils.toolbelt :as toolbelt]))

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

(defn- loop-update-graph!
  "Define loop to periodically update graph."
  []
  (loop-builder :updates.periodic/graph?
                #(rf/dispatch [:updates.periodic.discussion.graph/request])))

(defn- loop-periodic-discussion-start!
  "Define loop to periodically update polls."
  []
  (loop-builder :updates.periodic.discussion/starting?
                #(rf/dispatch [:updates.periodic.discussion.starting/request])))

;; -----------------------------------------------------------------------------
;; Init

(defn init-periodic-updates
  "Initializing function to start the loops. Each looping function must be
  called here once to start the endless loop."
  []
  (loop-periodic-discussion-start!)
  (loop-update-graph!))

;; -----------------------------------------------------------------------------

(rf/reg-sub
 :updates.periodic.discussion/starting?
 (fn [db _]
   (get-in db [:updates/periodic :discussion/starting] false)))

(rf/reg-event-db
 :updates.periodic.discussion/starting
 (fn [db [_ trigger?]]
   (assoc-in db [:updates/periodic :discussion/starting] trigger?)))

(rf/reg-sub
 :updates.periodic/graph?
 (fn [db _]
   (get-in db [:updates/periodic :graph] false)))

(rf/reg-event-db
 :updates.periodic/graph
 (fn [db [_ trigger?]]
   (assoc-in db [:updates/periodic :graph] trigger?)))

(rf/reg-event-fx
 :updates.periodic.discussion.starting/request
 (fn [{:keys [db]}]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:fx [[:ws/send [:discussion.starting/update
                      {:share-hash share-hash
                       :display-name (toolbelt/current-display-name db)}
                      (fn [response]
                        (rf/dispatch [:schnaq.activation.load-from-backend/success response])
                        (rf/dispatch [:schnaq.polls.load-from-backend/success response])
                        (rf/dispatch [:discussion.query.conclusions/set-starting response]))]]]})))

(rf/reg-event-fx
 :updates.periodic.discussion.graph/request
 (fn [{:keys [db]}]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:fx [[:ws/send [:discussion.graph/update
                      {:share-hash share-hash
                       :display-name (toolbelt/current-display-name db)}
                      (fn [response]
                        (rf/dispatch [:graph/set-current response]))]]]})))

(comment

  (rf/dispatch [:updates.periodic.discussion.starting/request])

  nil)
