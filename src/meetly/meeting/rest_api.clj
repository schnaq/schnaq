(ns meetly.meeting.rest-api
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [org.httpkit.server :as server]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.cors :refer [wrap-cors]]
            [meetly.config :as config]
            [clojure.pprint :as pp]
            [clojure.data.json :as json]
            [meetly.meeting.database :as db]))

(defn- date->epoch-str
  "Converts java.util.Date to epoch string"
  [date]
  (-> date .getTime str))

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

(defroutes app-routes
           (GET "/" [] hello-name)
           (GET "/meetings" [] all-meetings)
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
          (wrap-defaults site-defaults))
      {:port port})
    ; Run the server without ring defaults
    ;(server/run-server #'app-routes {:port port})
    (println (str "Running webserver at http:/127.0.0.1:" port "/"))))