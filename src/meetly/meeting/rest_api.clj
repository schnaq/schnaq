(ns meetly.meeting.rest-api
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [org.httpkit.server :as server]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.response :refer [response]]
            [meetly.config :as config]
            [clojure.pprint :as pp]
            [clojure.data.json :as json]
            [meetly.meeting.database :as db]
            [meetly.meeting.dialog-connector :as dialogs])
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

(defn- normalize-meeting
  "Normalizes a single meeting for the wire."
  [meeting]
  (-> meeting
      (update :meeting/start-date date->epoch-str)
      (update :meeting/end-date date->epoch-str)
      json/write-str))

(defn- all-meetings
  "Returns all meetings from the db. Cleaned for the wire."
  [_req]
  (response (fetch-meetings)))

(defn- index-page
  "Returns an index page placeholder."
  [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (->
           (pp/pprint req)
           (str "Hello there, General Kenobi!"))})

(defn- add-meeting
  "Adds a meeting to the database.
  Converts the epoch dates it receives into java Dates.
  Returns the id of the newly-created meeting as `:id-created`."
  [req]
  (let [meeting (-> req :body :meeting)
        new-id (db/add-meeting (-> meeting
                                   (update :end-date epoch->date)
                                   (update :start-date epoch->date)))]
    (response {:text "Meeting Added"
               :id-created new-id})))

(defn- add-agendas
  "Adds a list of agendas to the database."
  [req]
  (let [agendas (-> req :body :agendas vals)
        meeting-id (-> req :body :meeting-id)]
    (doseq [agenda-point agendas]
      (db/add-agenda-point (:title agenda-point) (:description agenda-point)
                           meeting-id)))
  (response {:text "Agendas, sent over successfully"}))

(defn- meeting-by-hash
  "Returns a meeting, identified by its share-hash."
  [req]
  (let [hash (get-in req [:route-params :hash])]
    (response (normalize-meeting (db/meeting-by-hash hash)))))

(defn- agendas-by-meeting-hash
  "Returns all agendas of a meeting, that matches the share-hash."
  [req]
  (let [meeting-hash (get-in req [:route-params :hash])]
    (response {:agendas (db/agendas-by-meeting-hash meeting-hash)})))

(defn- all-starting-conclusions
  "Returns all conclusions belonging to starting-arguments of discussion with id `id`.
  Returns a list of conclusions over the wire."
  [req]
  (let [id (get-in req [:route-params :id])]
    (response {:conclusions (dialogs/starting-conclusions id)})))

(defn- agenda-by-discussion-id
  [req]
  (let [discussion-id (-> req :route-params :discussion-id)]
    (response {:agenda (db/agenda-by-discussion-id discussion-id)})))

(defroutes app-routes
           (GET "/" [] index-page)
           (GET "/meetings" [] all-meetings)
           (GET "/meeting/by-hash/:hash" [] meeting-by-hash)
           (POST "/meeting/add" [] add-meeting)
           (POST "/agendas/add" [] add-agendas)
           (GET "/agendas/by-meeting-hash/:hash" [] agendas-by-meeting-hash)
           (GET "/agenda/:discussion-id" [] agenda-by-discussion-id)
           (GET "/agenda/starting-conclusions/:id" [] all-starting-conclusions)
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
          wrap-json-response
          (wrap-defaults api-defaults))
      {:port port})
    ; Run the server without ring defaults
    ;(server/run-server #'app-routes {:port port})
    (println (str "Running web-server at http:/127.0.0.1:" port "/"))))