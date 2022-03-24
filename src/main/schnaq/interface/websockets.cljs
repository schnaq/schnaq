(ns schnaq.interface.websockets
  (:require [mount.core :refer [defstate] :as mount]
            [re-frame.core :as rf]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [taoensso.sente :as sente]))

(defmulti handle-message (fn [{:keys [id]} _] id))

(defmethod handle-message :message/add
  [_ msg-add-event]
  (rf/dispatch msg-add-event))

(defmethod handle-message :message/creation-errors
  [_ [_ response]]
  (rf/dispatch
   [:form/set-server-errors (:errors response)]))

;; ---------------------------------------------------------------------------
;; Default Handlers

(defmethod handle-message :chsk/handshake
  [{:keys [event]} _]
  (.log js/console "Connection Established:" (pr-str event)))

(defmethod handle-message :chsk/state
  [{:keys [event]} _]
  (.log js/console "State Changed:" (pr-str event)))

(defmethod handle-message :default
  [{:keys [event]} _]
  (.warn js/console "Unknown websocket message:" (pr-str event)))

;; ---------------------------------------------------------------------------
;; Router

(defn- receive-message!
  "Prepare message retrieval. Pass to multifunction."
  [{:keys [event] :as ws-message}]
  (handle-message ws-message event))

(defstate socket
  :start (sente/make-channel-socket!
          "/ws"
          nil
          {:host "localhost:3000"
           :type :auto
           :headers {"x-schnaq-csrf" "this content does not matter"}
           :wrap-recv-evs? false}))

(defstate channel-router
  :start (sente/start-chsk-router!
          (:ch-recv @socket)
          #'receive-message!)
  :stop (when-let [stop-fn @channel-router]
          (stop-fn)))

(defn send! [& args]
  (if-let [send-fn (:send-fn @socket)]
    (apply send-fn args)
    (throw (ex-info "Couldn't send message, channel isn't open!"
                    {:message (first args)}))))

;; -----------------------------------------------------------------------------

(rf/reg-fx
 :ws/send
 (fn [[event ?data ?reply-fn]]
   (send! [event ?data]
          1000
          ?reply-fn)))

(rf/reg-event-fx
 :ws.discussion.starting/update
 (fn [{:keys [db]}]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:fx [[:ws/send [:discussion.starting/update
                      {:share-hash share-hash
                       :display-name (toolbelt/current-display-name db)}
                      (fn [response]
                        (rf/dispatch [:schnaq.activation.load-from-backend/success response])
                        (rf/dispatch [:schnaq.polls.load-from-backend/success response])
                        (rf/dispatch [:discussion.query.conclusions/set-starting response]))]]]})))

(comment
  @socket

  (rf/dispatch [:ws.discussion.starting/update])
  (send! [:chsk/debug "harhar"] 1000
         (fn [response]
           (.log js/console (pr-str response))))

  (send! [:discussion/update {:share-hash "fbc512d7-89d1-4f6c-be6a-fc05a44b2976"}]
         1000
         (fn [response]
           (.log js/console (pr-str response))))

  (mount/start)
  (mount/stop)
  nil)
