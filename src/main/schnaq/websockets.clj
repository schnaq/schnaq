(ns schnaq.websockets
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [?]]
            [ring.util.http-response :refer [ok]]
            [mount.core :refer [defstate] :as mount]
            [ring.middleware.keyword-params :as keyword-params]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [taoensso.timbre :as log]
            [schnaq.api.middlewares :as middlewares]))

(defstate socket
  :start (sente/make-channel-socket!
          (get-sch-adapter)
          {:csrf-token-fn nil
           :user-id-fn (fn [ring-req]
                         (get-in ring-req [:parameters :query :client-id]))}))

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

(s/def ::client-id string?)

(defn websocket-routes []
  ["/ws"
   {:swagger {:tags ["websockets"]}
    :middleware [keyword-params/wrap-keyword-params]
    :get {:handler (:ajax-get-or-ws-handshake-fn socket)
          :parameters {:query (s/keys :opt-un [::client-id])}}
    :post {:handler (:ajax-post-fn socket)}}])
