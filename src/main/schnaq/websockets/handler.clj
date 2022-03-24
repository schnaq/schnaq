(ns schnaq.websockets.handler
  "Create a websocket-backend, where clients can connect to for real-time
  messaging."
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn =>]]
            [mount.core :refer [defstate] :as mount]
            [ring.middleware.keyword-params :as keyword-params]
            [schnaq.database.specs]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [taoensso.timbre :as log]))

(declare socket) ;; To avoid clj-kondo false positive

;; Prepare incoming websocket connection
(defstate socket
  :start (sente/make-channel-socket!
          (get-sch-adapter)
          {:csrf-token-fn nil
           :user-id-fn (fn [ring-req]
                         (get-in ring-req [:parameters :query :client-id]))}))

(>defn send!
  "Send a single message to a client referenced by its uid."
  [uid message]
  [:ws.message/uid :ws.message/?data => any?]
  (println "Sending message:" message)
  ((:send-fn socket) uid message))

;; -----------------------------------------------------------------------------
;; React to events received from websocket clients

(defmulti handle-message (fn [{:keys [id]}] id))
(defmethod handle-message :chsk/ws-ping [_message])
(defmethod handle-message :chsk/uidport-open [_message])

(defmethod handle-message :chsk/uidport-close [{:keys [uid]}]
  (log/debug "Client closed connection" uid))

(defmethod handle-message :chsk/debug [{:keys [id ?data]}]
  (log/debug "Received a debug message" id ?data)
  [id ?data])

(defmethod handle-message :default [{:keys [id]}]
  (log/debug "Received unrecognized websocket event type:" id)
  {:error (str "Unrecognized websocket event type:" (pr-str id))
   :id id})

(defn receive-message!
  "Process the message in `handle-message` and send a response if `?reply-fn` is
  specified."
  [{:keys [?reply-fn] :as message}]
  (let [reply-fn (or ?reply-fn (fn [_]))]
    (when-some [response (handle-message message)]
      (reply-fn response))))

;; Receive and dispatch incoming websocket messages
(defstate channel-router
  :start (sente/start-chsk-router!
          (:ch-recv socket)
          #'receive-message!)
  :stop (when-let [stop-fn channel-router]
          (stop-fn)))

;; -----------------------------------------------------------------------------

(s/def ::client-id string?)

(defn websocket-routes []
  ["/ws"
   {:swagger {:tags ["websockets"]}
    :middleware [keyword-params/wrap-keyword-params]
    :parameters {:query (s/keys :opt-un [::client-id])}
    :get {:handler (:ajax-get-or-ws-handshake-fn socket)
          :name :api.ws/get}
    :post {:handler (:ajax-post-fn socket)
           :name :api.ws/post}}])

(comment
  (mount/start)
  (mount/stop)

  @(:connected-uids socket)

  (send! (first (:any @(:connected-uids socket)))
         [:message/add "huhu"])

  nil)
