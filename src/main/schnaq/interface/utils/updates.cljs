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
  "Define loop to periodically update discussions."
  []
  (loop-builder :updates.periodic.discussion/starting?
                #(rf/dispatch [:updates.periodic.discussion.starting/request])))

(defn- loop-periodic-activation-start!
  "Define loop to periodically update activations."
  []
  (loop-builder :updates.periodic/activation?
                #(rf/dispatch [:updates.periodic.activation/request])))

(defn- loop-periodic-poll!
  "Define loop to periodically update polls."
  []
  (loop-builder :updates.periodic.present/poll?
                #(rf/dispatch [:updates.periodic.present.poll/request])))

;; -----------------------------------------------------------------------------
;; Init

(defn init-periodic-updates
  "Initializing function to start the loops. Each looping function must be
  called here once to start the endless loop."
  []
  (loop-periodic-discussion-start!)
  (loop-update-graph!)
  (loop-periodic-activation-start!)
  (loop-periodic-poll!))

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
 :updates.periodic/activation?
 (fn [db _]
   (get-in db [:updates/periodic :activation] false)))

(rf/reg-event-db
 :updates.periodic/activation
 (fn [db [_ trigger?]]
   (assoc-in db [:updates/periodic :activation] trigger?)))

(rf/reg-sub
 :updates.periodic.present/poll?
 (fn [db _]
   (get-in db [:updates/periodic :present/poll] false)))

(rf/reg-event-db
 :updates.periodic.present/poll
 (fn [db [_ trigger?]]
   (assoc-in db [:updates/periodic :present/poll] trigger?)))

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
                       :display-name (toolbelt/current-display-name db)
                       :jwt (.-token (get-in db [:user :keycloak]))}
                      (fn [response]
                        (rf/dispatch [:schnaq.activation.load-from-backend/success response])
                        (rf/dispatch [:discussion.activations/focus response])
                        (rf/dispatch [:schnaq.polls.load-from-backend/success response])
                        (rf/dispatch [:discussion.query.conclusions/set-starting response])
                        (rf/dispatch [:schnaq.wordclouds.local.load/success response])
                        (rf/dispatch [:schnaq.wordcloud/from-backend response]))]]]})))

(rf/reg-event-db
 :discussion.activations/focus
 (fn [db [_ {:keys [activation-focus]}]]
   (let [old-focus (get-in db [:schnaq :selected :discussion/activation-focus])]
     (when (not= old-focus activation-focus)
       (toolbelt/new-activation-focus db activation-focus)))))

(rf/reg-event-fx
 :updates.periodic.activation/request
 (fn [{:keys [db]}]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:fx [[:ws/send [:discussion.activation/update
                      {:share-hash share-hash}
                      (fn [response]
                        (rf/dispatch [:schnaq.activation.load-from-backend/success (:body response)]))]]]})))

(rf/reg-event-fx
 :updates.periodic.discussion.graph/request
 (fn [{:keys [db]}]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:fx [[:ws/send [:discussion.graph/update
                      {:share-hash share-hash
                       :display-name (toolbelt/current-display-name db)}
                      (fn [response]
                        (rf/dispatch [:graph/set-current response]))]]]})))

(rf/reg-event-fx
 :updates.periodic.present.poll/request
 (fn [{:keys [db]}]
   (let [share-hash (get-in db [:current-route :parameters :path :share-hash])
         poll-id (get-in db [:current-route :parameters :path :entity-id])]
     {:fx [[:ws/send [:schnaq.poll/update
                      {:share-hash share-hash
                       :poll-id poll-id}
                      (fn [response]
                        (rf/dispatch [:schnaq.poll.load-from-query/success response]))]]]})))

(comment

  (rf/dispatch [:updates.periodic.discussion.starting/request])

  nil)
