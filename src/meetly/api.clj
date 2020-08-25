(ns meetly.api
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [compojure.core :refer [GET POST routes]]
            [compojure.route :as route]
            [dialog.engine.core :as dialog]
            [dialog.discussion.database :as dialog-db]
            [ghostwheel.core :refer [>defn- ?]]
            [meetly.config :as config]
            [meetly.core :as meetly-core]
            [meetly.discussion :as discussion]
            [meetly.meeting.database :as db]
            [meetly.meeting.processors :as processors]
            [meetly.toolbelt :as toolbelt]
            [org.httpkit.server :as server]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.util.response :refer [response not-found bad-request status]]
            [taoensso.timbre :as log])
  (:import (java.util Base64 UUID))
  (:gen-class))

(>defn- valid-password?
  "Check if the password is a valid."
  [password]
  [string? :ret boolean?]
  (= config/admin-password password))

(>defn- valid-credentials?
  "Validate if share-hash and admin-hash match"
  [share-hash edit-hash]
  [string? string? :ret boolean?]
  (let [authenticate-meeting (db/meeting-by-hash-private share-hash)]
    (= edit-hash (:meeting/edit-hash authenticate-meeting))))

(>defn- valid-discussion-hash?
  "Check if share hash and discussion id match."
  [share-hash discussion-id]
  [string? int? :ret boolean?]
  (not (nil? (db/agenda-by-meeting-hash-and-discussion-id share-hash discussion-id))))

(defn- fetch-meetings
  "Fetches meetings from the db and preparse them for transit via JSON."
  []
  (->> (db/all-meetings)
       (map first)))

(defn- ping
  "Route to ping the API. Used in our monitoring system."
  [_]
  (response {:text "🧙‍♂️"}))

(defn- all-meetings
  "Returns all meetings from the db."
  [_req]
  (response (fetch-meetings)))

(defn- add-meeting
  "Adds a meeting to the database.
  Converts the epoch dates it receives into java Dates.
  Returns the id of the newly-created meeting as `:id-created`."
  [req]
  (let [meeting (-> req :body-params :meeting)
        final-meeting (assoc meeting :meeting/share-hash (.toString (UUID/randomUUID))
                                     :meeting/edit-hash (.toString (UUID/randomUUID)))
        nickname (-> req :body-params :nickname)
        author-id (db/add-user-if-not-exists nickname)
        meeting-id (db/add-meeting (assoc final-meeting :meeting/author author-id))
        created-meeting (db/meeting-private-data meeting-id)]
    (response {:new-meeting created-meeting})))

(defn- update-meeting!
  "Updates a meeting and its agendas."
  [{:keys [body-params]}]
  (let [nickname (:nickname body-params)
        user-id (db/add-user-if-not-exists nickname)
        meeting (:meeting body-params)
        updated-meeting (dissoc meeting :meeting/share-hash :meeting/edit-hash)
        updated-agendas (filter :agenda/discussion (:agendas body-params))
        new-agendas (remove :agenda/discussion (:agendas body-params))]
    (if (valid-credentials? (:meeting/share-hash meeting) (:meeting/edit-hash meeting))
      (do (db/update-meeting (assoc updated-meeting :meeting/author user-id))
          (doseq [agenda new-agendas]
            (db/add-agenda-point (:agenda/title agenda) (:agenda/description agenda) (:agenda/meeting agenda)))
          (doseq [agenda updated-agendas]
            (db/update-agenda agenda))
          (db/delete-agendas (:delete-agendas body-params) (:db/id meeting))
          (response {:text "Your Meetly has been updated."}))
      (bad-request {:error "You are not allowed to update this meeting."}))))

(defn- add-author
  "Adds an author to the database."
  [req]
  (let [author-name (:nickname (:body-params req))]
    (db/add-user-if-not-exists author-name)
    (response {:text "POST successful"})))

(defn- add-agendas
  "Adds a list of agendas to the database."
  [{:keys [body-params]}]
  (let [agendas (:agendas body-params)
        meeting-id (:meeting-id body-params)
        meeting-hash (:meeting-hash body-params)]
    (if (and (s/valid? :meeting/share-hash meeting-hash)
             (s/valid? int? meeting-id)
             (= meeting-id (:db/id (db/meeting-by-hash meeting-hash))))
      (do (doseq [agenda-point agendas]
            (db/add-agenda-point (:title agenda-point) (:description agenda-point)
                                 meeting-id))
          (response {:text "Agendas sent over successfully"}))
      (bad-request {:error "Agenda could not be added"}))))

(defn- meeting-by-hash
  "Returns a meeting, identified by its share-hash."
  [req]
  (let [hash (get-in req [:route-params :hash])]
    (response (db/meeting-by-hash hash))))

(defn- agendas-by-meeting-hash
  "Returns all agendas of a meeting, that matches the share-hash."
  [req]
  (let [meeting-hash (get-in req [:route-params :hash])]
    (response (db/agendas-by-meeting-hash meeting-hash))))

(defn- agenda-by-meeting-hash-and-discussion-id
  "Returns the agenda tied to a certain discussion-id."
  [req]
  (let [discussion-id (Long/valueOf ^String (-> req :route-params :discussion-id))
        meeting-hash (-> req :route-params :meeting-hash)
        agenda-point (db/agenda-by-meeting-hash-and-discussion-id meeting-hash discussion-id)]
    (if agenda-point
      (response agenda-point)
      (not-found {:error
                  (format "No Agenda with discussion-id %s in the DB or the queried discussion does not belong to the meeting %s."
                          discussion-id meeting-hash)}))))

(defn- statement-infos
  "Returns additional information regarding a statement. Currently queries every time anew.
  This could be made way more efficient with a cache or an optimized graph traversal for the whole
  discussion. It is not needed yet though."
  [req]
  (let [discussion-id (Long/valueOf ^String (get-in req [:query-params "discussion-id"]))
        meeting-hash (get-in req [:query-params "meeting-hash"])
        statement-id (Long/valueOf ^String (get-in req [:query-params "statement-id"]))]
    (if (valid-discussion-hash? meeting-hash discussion-id)
      (response (discussion/sub-discussion-information statement-id (dialog-db/all-arguments-for-discussion discussion-id)))
      (bad-request {:error "The link you followed was invalid."}))))

(defn- start-discussion
  "Start a new discussion for an agenda point."
  [req]
  (let [discussion-id (Long/valueOf ^String (get-in req [:route-params :discussion-id]))
        username (get-in req [:query-params "username"])
        meeting-hash (get-in req [:query-params "meeting-hash"])]
    (if (valid-discussion-hash? meeting-hash discussion-id)
      (response (->
                  (dialog/start-discussion {:discussion/id discussion-id
                                            :user/nickname (db/canonical-username username)})
                  processors/with-votes
                  (processors/with-sub-discussion-information (dialog-db/all-arguments-for-discussion discussion-id))))
      (bad-request {:error "The link you followed was invalid"}))))


(defn- continue-discussion
  "Dispatches the wire-received events to the dialog.core backend."
  [{:keys [body-params]}]
  (let [[reaction args] (processors/with-canonical-usernames (:payload body-params))
        meeting-hash (:meeting-hash body-params)
        discussion-id (:discussion-id body-params)]
    (if (valid-discussion-hash? meeting-hash discussion-id)
      (response (->
                  (dialog/continue-discussion reaction args)
                  processors/with-votes
                  (processors/with-sub-discussion-information (dialog-db/all-arguments-for-discussion discussion-id))))
      (bad-request {:error "The link you followed was invalid"}))))


;; -----------------------------------------------------------------------------
;; Votes

(defn- toggle-vote-statement
  "Toggle up- or downvote of statement."
  [{:keys [meeting-hash statement-id nickname]} add-vote-fn remove-vote-fn check-vote-fn counter-check-vote-fn]
  (if (db/check-valid-statement-id-and-meeting statement-id meeting-hash)
    (let [nickname (db/canonical-username nickname)
          vote (check-vote-fn statement-id nickname)
          counter-vote (counter-check-vote-fn statement-id nickname)]
      (if vote
        (do (remove-vote-fn statement-id nickname)
            (response {:operation :removed}))
        (do (add-vote-fn statement-id nickname)
            (if counter-vote
              (response {:operation :switched})
              (response {:operation :added})))))
    (bad-request {:error "Vote could not be registered"})))

(defn- toggle-upvote-statement
  "Upvote if no upvote has been made, otherwise remove upvote for statement."
  [{:keys [body-params]}]
  (toggle-vote-statement
    body-params db/upvote-statement! db/remove-upvote!
    db/did-user-upvote-statement db/did-user-downvote-statement))

(defn- toggle-downvote-statement
  "Upvote if no upvote has been made, otherwise remove upvote for statement."
  [{:keys [body-params]}]
  (toggle-vote-statement
    body-params db/downvote-statement! db/remove-downvote!
    db/did-user-downvote-statement db/did-user-upvote-statement))


;; -----------------------------------------------------------------------------
;; Feedback

(>defn- save-screenshot-if-provided!
  "Stores a base64 encoded file to disk."
  [screenshot directory file-name]
  [(? string?) string? (s/or :number number? :string string?)
   :ret nil?]
  (when screenshot
    (let [[_header image] (string/split screenshot #",")
          #^bytes decodedBytes (.decode (Base64/getDecoder) ^String image)
          path (toolbelt/create-directory! directory)
          location (format "%s/%s.png" path file-name)]
      (with-open [w (io/output-stream location)]
        (.write w decodedBytes)))))

(defn- add-feedback
  "Add new feedback from meetly's frontend."
  [{:keys [body-params]}]
  (let [feedback (:feedback body-params)
        feedback-id (db/add-feedback! feedback)
        screenshot (:screenshot body-params)]
    (save-screenshot-if-provided! screenshot "public/feedbacks/screenshots" feedback-id)
    (response {:text "Feedback successfully created."})))

(defn- all-feedbacks
  "Returns all feedbacks from the db."
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (response (->> (db/all-feedbacks)
                   (map first)))
    (status 401)))


;; -----------------------------------------------------------------------------
;; Analytics

(defn- number-of-meetings
  "Returns the number of all meetings."
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (response {:meetings-num (db/number-of-meetings)})
    (bad-request {:message "You are not allowed to use this resource"})))

(defn- number-of-usernames
  "Returns the number of all meetings."
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (response {:usernames-num (db/number-of-usernames)})
    (bad-request {:message "You are not allowed to use this resource"})))

(defn- agendas-per-meeting
  "Returns the average numbers of meetings"
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (response {:average-agendas (float (db/average-number-of-agendas))})
    (bad-request {:message "You are not allowed to use this resource"})))

(defn- number-of-statements
  "Returns the number of statements"
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (response {:statements-num (db/number-of-statements)})
    (bad-request {:message "You are not allowed to use this resource"})))

(defn- number-of-active-users
  "Returns the number of statements"
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (response {:active-users-num (db/number-of-active-users)})
    (bad-request {:message "You are not allowed to use this resource"})))

(defn- statement-lengths-stats
  "Returns statistics about the statement length."
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (response {:statement-length-stats (db/statement-length-stats)})
    (bad-request {:message "You are not allowed to use this resource"})))

(defn- argument-type-stats
  "Returns statistics about the statement length."
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (response {:argument-type-stats (db/argument-type-stats)})
    (bad-request {:message "You are not allowed to use this resource"})))

(defn- check-credentials
  "Checks whether share-hash and edit-hash match."
  [{:keys [body-params]}]
  (let [share-hash (:share-hash body-params)
        edit-hash (:edit-hash body-params)]
    (response {:valid-credentials? (valid-credentials? share-hash edit-hash)})))

(defn- graph-data-for-agenda
  "Delivers the graph-data needed to draw the graph in the frontend."
  [{:keys [body-params]}]
  (let [share-hash (:share-hash body-params)
        discussion-id (:discussion-id body-params)
        statements (db/all-statements-for-discussion discussion-id)
        starting-arguments (dialog-db/starting-arguments-by-discussion discussion-id)]
    (if (valid-discussion-hash? share-hash discussion-id)
      (response {:graph {:nodes (discussion/nodes-for-agenda statements starting-arguments discussion-id share-hash)
                         :links (discussion/links-for-agenda statements starting-arguments discussion-id)}})
      (bad-request {:error "Invalid meeting hash. You are not allowed to view this data."}))))


;; -----------------------------------------------------------------------------
;; Routes

(def ^:private not-found-msg
  "Error, page not found!")

(def ^:private common-routes
  "Common routes for all modes."
  (routes
    (GET "/ping" [] ping)
    (GET "/meeting/by-hash/:hash" [] meeting-by-hash)
    (POST "/meeting/add" [] add-meeting)
    (POST "/meeting/update" [] update-meeting!)
    (POST "/agendas/add" [] add-agendas)
    (POST "/author/add" [] add-author)
    (GET "/agendas/by-meeting-hash/:hash" [] agendas-by-meeting-hash)
    (GET "/agenda/:meeting-hash/:discussion-id" [] agenda-by-meeting-hash-and-discussion-id)
    (GET "/start-discussion/:discussion-id" [] start-discussion)
    (GET "/statement-infos" [] statement-infos)
    (POST "/continue-discussion" [] continue-discussion)
    (POST "/votes/up/toggle" [] toggle-upvote-statement)
    (POST "/votes/down/toggle" [] toggle-downvote-statement)
    (POST "/feedback/add" [] add-feedback)
    (POST "/feedbacks" [] all-feedbacks)
    (POST "/credentials/validate" [] check-credentials)
    (POST "/graph/discussion" [] graph-data-for-agenda)
    ;; Analytics routes
    (POST "/analytics/meetings" [] number-of-meetings)
    (POST "/analytics/usernames" [] number-of-usernames)
    (POST "/analytics/agendas-per-meeting" [] agendas-per-meeting)
    (POST "/analytics/statements" [] number-of-statements)
    (POST "/analytics/active-users" [] number-of-active-users)
    (POST "/analytics/statement-lengths" [] statement-lengths-stats)
    (POST "/analytics/argument-types" [] argument-type-stats)))

(def ^:private development-routes
  "Exclusive Routes only available outside of production."
  (routes
    (GET "/meetings" [] all-meetings)))

(def ^:private app-routes
  "Building routes for app."
  (if meetly-core/production-mode?
    (routes common-routes
            (route/not-found not-found-msg))
    (routes common-routes
            development-routes
            (route/not-found not-found-msg))))


;; -----------------------------------------------------------------------------
;; General

(defonce current-server (atom nil))

(defn- stop-server []
  (when-not (nil? @current-server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@current-server :timeout 100)
    (reset! current-server nil)))

(defn- say-hello
  "Print some debug information to the console when the system is loaded."
  []
  (log/info "Welcome to Meetly's Backend 🧙")
  (log/info (format "Build Hash: %s" config/build-hash))
  (log/info (format "Environment: %s" config/env-mode))
  (log/info (format "Port: %s" (:port config/api)))
  (log/info (format "Database Name: %s" config/db-name)))

(defn -main
  "This is our main entry point for the REST API Server."
  [& _args]
  (let [port (:port config/api)
        allowed-origins [#".*\.dialogo\.io"]
        allowed-origins' (if meetly-core/production-mode? allowed-origins (conj allowed-origins #".*"))]
    ; Run the server with Ring.defaults middleware
    (meetly-core/-main)
    (reset! current-server
            (server/run-server
              (-> #'app-routes
                  (wrap-cors :access-control-allow-origin allowed-origins'
                             :access-control-allow-methods [:get :put :post :delete])
                  (wrap-restful-format :formats [:transit-json :transit-msgpack :json-kw :edn :msgpack-kw :yaml-kw :yaml-in-html])
                  (wrap-defaults api-defaults))
              {:port port}))
    (say-hello)
    (log/info (format "Running web-server at http://127.0.0.1:%s/" port))
    (log/info (format "Allowed Origin: %s" allowed-origins'))))

(comment
  "Start the server from here"
  (-main)
  (stop-server)
  :end)