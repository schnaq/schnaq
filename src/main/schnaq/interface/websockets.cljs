(ns schnaq.interface.websockets
  (:require [mount.core :refer [defstate] :as mount]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [taoensso.sente :as sente]
            [taoensso.timbre :as log]))

(declare socket)

;; ---------------------------------------------------------------------------
;; Default Handlers

(defmulti handle-message (fn [{:keys [id]} _] id))

(defmethod handle-message :chsk/handshake
  [_ _]
  (log/info "Websocket connection established"))

;; State changed event
(defmethod handle-message :chsk/state [_ _])

(defmethod handle-message :default
  [{:keys [event]} _]
  (log/debug "Unknown websocket message:" (pr-str event)))

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
          {:host (.-host (js/URL. shared-config/api-url))
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

(def ^:private websocket-timeout
  1000)

(rf/reg-fx
 :ws/send
 (fn [[event ?data ?reply-fn]]
   (send! [event ?data]
          websocket-timeout
          ?reply-fn)))

(comment
  @socket

  (send! [:chsk/debug "harhar"] websocket-timeout
         (fn [response]
           (.log js/console (pr-str response))))

  (mount/start)
  (mount/stop)
  nil)
