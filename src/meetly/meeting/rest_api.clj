(ns meetly.meeting.rest-api
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [org.httpkit.server :as server]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.response :refer [response]]
            [meetly.config :as config]
            [clojure.pprint :as pp]
            [clojure.data.json :as json]
            [meetly.meeting.database :as db])
  (:import (java.util Date)))

(defn- date->epoch-str
  "Converts java.util.Date to epoch string"
  [date]
  (-> date .getTime str))

(defn- epoch->date
  "Converts an unix-timestamp to a java.util.Date"
  [epoch]
  (new Date epoch))

(defn- fetch-meetings
  "Fetches meetings from the db and preparse them for transit via JSON."
  []
  (->> (db/all-meetings)
       (map first)
       (map #(update % :meeting/start-date date->epoch-str))
       (map #(update % :meeting/end-date date->epoch-str))
       json/write-str))

(defn all-meetings [_req]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body (fetch-meetings)})

; request-example
(defn request-example [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (->>
           (pp/pprint req)
           (str "Request Object: " req))})

(defn hello-name [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (->
           (pp/pprint req)
           (str "Hello " (:name (:params req))))})

(defn add-meeting [req]
  (let [meeting (-> req :body :meeting)]
    (db/add-meeting (-> meeting
                        (update :end-date epoch->date)
                        (update :start-date epoch->date)))
    (response (str "Good job, meeting added!" meeting))))

;; TODO STUB
;; Write every agenda point to db and assign the correct meeting to it.
(defn add-agendas [req]
  (response :error))

(defroutes app-routes
           (GET "/" [] hello-name)
           (GET "/meetings" [] all-meetings)
           (POST "/meeting/add" [] add-meeting)
           (POST "/agendas/add" [] add-agendas)
           (route/not-found "Error, page not found!"))


(defn -main
  "This is our main entry point for the REST API Server"
  []
  (let [port (:port config/rest-api)]
    ; Run the server with Ring.defaults middleware
    (server/run-server
      (-> #'app-routes
          (wrap-cors :access-control-allow-origin [#".*"]
                     :access-control-allow-methods [:get :put :post :delete])
          (wrap-json-body {:keywords? true :bigdecimals? true})
          (wrap-defaults api-defaults))
      {:port port})
    ; Run the server without ring defaults
    ;(server/run-server #'app-routes {:port port})
    (println (str "Running web-server at http:/127.0.0.1:" port "/"))))