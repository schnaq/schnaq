(ns schnaq.interface.websockets
  (:require [cljs.reader :as edn]
            [mount.core :refer [defstate] :as mount]
            [re-frame.core :as rf]
            [taoensso.sente :as sente]))

(defonce channel (atom nil))
(defn connect! [url receive-handler]
  (if-let [chan (js/WebSocket. url)]
    (do
      (.log js/console "Connected!")
      (set! (.-onmessage chan) #(->> %
                                     .-data
                                     edn/read-string
                                     receive-handler))
      (reset! channel chan))
    (throw (ex-info "Websocket Connection Failed!"
                    {:url url}))))

(defmulti handle-message
  (fn [{:keys [id]} _]
    id))
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
  (.log js/console "Connection Established: " (pr-str event)))
(defmethod handle-message :chsk/state
  [{:keys [event]} _]
  (.log js/console "State Changed: " (pr-str event)))
(defmethod handle-message :default
  [{:keys [event]} _]
  (.warn js/console "Unknown websocket message: " (pr-str event)))
;; ---------------------------------------------------------------------------
;; Router
(defn receive-message!
  [{:keys [id event] :as ws-message}]
  (do
    (.log js/console "Event Received: " (pr-str event))
    (handle-message ws-message event)))

(defstate socket
  :start (sente/make-channel-socket!
          "/ws"
          nil
          {:host "localhost:3000"
           :type :auto
           :wrap-recv-evs? false}))

(defstate channel-router
  :start (sente/start-chsk-router!
          (:ch-recv @socket)
          #'receive-message!)
  :stop (when-let [stop-fn @channel-router]
          (stop-fn)))

(defn send! [message]
  (if-let [send-fn (:send-fn @socket)]
    (send-fn message)
    (throw (ex-info "Couldn't send message, channel isn't open!"
                    {:message message}))))

(rf/reg-event-fx
 :message/send!
 (fn [{:keys [db]} [_ fields]]
   (send! fields)
   {:db (dissoc db :form/server-errors)}))

(defn handle-response! [response]
  (if-let [errors (:errors response)]
    (rf/dispatch [:form/set-server-errors errors])
    (do
      (rf/dispatch [:message/add response])
      (rf/dispatch [:form/clear-fields response]))))

(defn init! []
  (mount/start)
  #_(connect! "ws://localhost:3000/ws" handle-response!))

(comment

  (init!)

  nil)
