(ns schnaq.websockets
  (:require [clojure.spec.alpha :as s]
            [mount.core :refer [defstate] :as mount]
            [ring.middleware.keyword-params :as keyword-params]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [taoensso.timbre :as log]))

(defstate socket
  :start (sente/make-channel-socket!
          (get-sch-adapter)
          {:csrf-token-fn nil
           :user-id-fn (fn [ring-req]
                         (get-in ring-req [:parameters :query :client-id]))}))

(defn send! [uid message]
  (println "Sending message:" message)
  ((:send-fn socket) uid message))

(comment
  (mount/start)
  (mount/stop)

  @(:connected-uids socket)

  (send! (first (:any @(:connected-uids socket)))
         [:message/add "huhu"])

  nil)

;; -----------------------------------------------------------------------------

(defmulti handle-message (fn [{:keys [id]}] id))
(defmethod handle-message :chsk/ws-ping [_message])
(defmethod handle-message :chsk/uidport-open [_message])
(defmethod handle-message :default [{:keys [id]}]
  (log/info "Received unrecognized websocket event type:" id)
  {:error (str "Unrecognized websocket event type:" (pr-str id))
   :id id})

(defn receive-message! [{:keys [id ?reply-fn]
                         :as message}]
  (log/debug "Got message with id:" id)
  (let [reply-fn (or ?reply-fn (fn [_]))]
    (when-some [response (handle-message message)]
      (reply-fn response))))

;; -----------------------------------------------------------------------------

(defstate channel-router
  :start (sente/start-chsk-router!
          (:ch-recv socket)
          #'receive-message!)
  :stop (when-let [stop-fn channel-router]
          (stop-fn)))

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
