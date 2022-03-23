(ns schnaq.websockets
  (:require [mount.core :refer [defstate] :as mount]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.middleware.params :as middleware.params]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [taoensso.timbre :as log]))

(defstate socket
  :start (sente/make-channel-socket!
          (get-sch-adapter)
          {:user-id-fn (fn [ring-req]
                         (get-in ring-req [:params :client-id]))}))

(defn send! [uid message]
  (println "Sending message: " message)
  ((:send-fn socket) uid message))

;; -----------------------------------------------------------------------------

(defmulti handle-message (fn [{:keys [id]}]
                           id))
(defmethod handle-message :default
  [{:keys [id]}]
  (log/debug "Received unrecognized websocket event type: " id))

(defmethod handle-message :message/create!
  [{:keys [?data uid] :as message}]
  (let [response (try
                   #_(msg/save-message! ?data)
                   (assoc ?data :timestamp (java.util.Date.))
                   (catch Exception e
                     (let [{id :guestbook/error-id
                            errors :errors} (ex-data e)]
                       (case id
                         :validation
                         {:errors errors}
                         {:errors
                          {:server-error ["Failed to save message!"]}}))))]
    (if (:errors response)
      (send! uid [:message/creation-errors response])
      (doseq [uid (:any @(:connected-uids socket))]
        (send! uid [:message/add response])))))

(defn receive-message! [{:keys [id] :as message}]
  (log/debug "Got message with id: " id)
  (handle-message message))

;; -----------------------------------------------------------------------------

(defstate channel-router
  :start (sente/start-chsk-router!
          (:ch-recv socket)
          #'receive-message!)
  :stop (when-let [stop-fn channel-router]
          (stop-fn)))

(comment

  (mount/start)

  nil)

(defn websocket-routes []
  ["/ws"
   {:swagger {:tags ["websockets"]}
    :middleware [keyword-params/wrap-keyword-params
                 middleware.params/wrap-params]
    :parameters {:query {:client-id string?}}
    :get {:handler (:ajax-get-or-ws-handshake-fn socket)}
    :post {:handler (:ajax-post-fn socket)}}])
