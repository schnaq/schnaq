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
     (rf/dispatch [:updates.periodic/check])
     (recur))))

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :updates.periodic/check
 (fn [{:keys [db]}]
   (let [updates-starting? (get-in db [:updates/periodic :discussion/starting])
         update-activation? (get-in db [:updates/periodic :activation])
         update-graph? (get-in db [:updates/periodic :graph])
         present-poll? (get-in db [:updates/periodic :present/poll])
         events (cond-> []
                  updates-starting? (conj [:dispatch [:updates.periodic.discussion.starting/request]])
                  update-activation? (conj [:dispatch [:updates.periodic.activation/request]])
                  update-graph? (conj [:dispatch [:updates.periodic.discussion.graph/request]])
                  present-poll? (conj [:dispatch [:updates.periodic.present.poll/request]]))]
     {:fx events})))

(rf/reg-event-db
 :updates/periodic
 (fn [db [_ key value]]
   (assoc-in db [:updates/periodic key] value)))

(rf/reg-event-fx
 :updates.periodic.discussion.starting/request
 (fn [{:keys [db]}]
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
