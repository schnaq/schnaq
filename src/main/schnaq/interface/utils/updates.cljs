(ns schnaq.interface.utils.updates
  "Coordinating asynchronous updates."
  (:require [cljs.core.async :refer [<! go-loop timeout]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [periodic-update-time]]
            [schnaq.interface.utils.toolbelt :as toolbelt]))

(rf/reg-fx
 :updates.periodic/loop
 (fn []
   (go-loop []
     (<! (timeout periodic-update-time))
     (rf/dispatch [:updates.periodic.discussion.graph/request])
     (rf/dispatch [:updates.periodic.discussion.starting/request])
     (rf/dispatch [:updates.periodic.activation/request])
     (rf/dispatch [:updates.periodic.present.poll/request])
     (recur))))

;; -----------------------------------------------------------------------------

(rf/reg-event-db
 :updates/periodic
 (fn [db [_ key value]]
   (assoc-in db [:updates/periodic key] value)))

(rf/reg-event-fx
 :updates.periodic.discussion.starting/request
 (fn [{:keys [db]}]
   (when (get-in db [:updates/periodic :discussion/starting])
     (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
           keycloak-object (get-in db [:user :keycloak])]
       {:fx [[:ws/send [:discussion.starting/update
                        (cond-> {:share-hash share-hash
                                 :display-name (toolbelt/current-display-name db)}
                          keycloak-object (assoc :jwt (.-token (get-in db [:user :keycloak]))))
                        (fn [response]
                          (rf/dispatch [:schnaq.activation.load-from-backend/success response])
                          (rf/dispatch [:discussion.activations/focus response])
                          (rf/dispatch [:schnaq.polls.load-from-backend/success response])
                          (rf/dispatch [:discussion.query.conclusions/set-starting response])
                          (rf/dispatch [:schnaq.wordclouds.local.load/success response])
                          (rf/dispatch [:schnaq.wordcloud/from-backend response]))]]]}))))

(rf/reg-event-db
 :discussion.activations/focus
 (fn [db [_ {:keys [activation-focus]}]]
   (let [old-focus (get-in db [:schnaq :selected :discussion/activation-focus])]
     (when (not= old-focus activation-focus)
       (toolbelt/new-activation-focus db activation-focus)))))

(rf/reg-event-fx
 :updates.periodic.activation/request
 (fn [{:keys [db]}]
   (when (get-in db [:updates/periodic :activation])
     (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
       {:fx [[:ws/send [:discussion.activation/update
                        {:share-hash share-hash}
                        (fn [response]
                          (rf/dispatch [:schnaq.activation.load-from-backend/success (:body response)]))]]]}))))

(rf/reg-event-fx
 :updates.periodic.discussion.graph/request
 (fn [{:keys [db]}]
   (when (get-in db [:updates/periodic :graph])
     (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
       {:fx [[:ws/send [:discussion.graph/update
                        {:share-hash share-hash
                         :display-name (toolbelt/current-display-name db)}
                        (fn [response]
                          (rf/dispatch [:graph/set-current response]))]]]}))))

(rf/reg-event-fx
 :updates.periodic.present.poll/request
 (fn [{:keys [db]}]
   (when (get-in db [:updates/periodic :present/poll])
     (let [share-hash (get-in db [:current-route :parameters :path :share-hash])
           poll-id (get-in db [:current-route :parameters :path :entity-id])]
       {:fx [[:ws/send [:schnaq.poll/update
                        {:share-hash share-hash
                         :poll-id poll-id}
                        (fn [response]
                          (rf/dispatch [:schnaq.poll.load-from-query/success response]))]]]}))))
