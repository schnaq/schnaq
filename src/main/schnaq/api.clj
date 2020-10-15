(ns schnaq.api
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [compojure.core :refer [GET POST routes]]
            [compojure.route :as route]
            [dialog.discussion.database :as dialog-db]
            [dialog.engine.core :as dialog]
            [ghostwheel.core :refer [>defn- ?]]
            [org.httpkit.server :as server]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.util.http-response :refer [ok created not-found bad-request forbidden unauthorized]]
            [schnaq.config :as config]
            [schnaq.core :as schnaq-core]
            [schnaq.discussion :as discussion]
            [schnaq.emails :as emails]
            [schnaq.meeting.database :as db]
            [schnaq.meeting.processors :as processors]
            [schnaq.suggestions :as suggestions]
            [schnaq.toolbelt :as toolbelt]
            [schnaq.translations :refer [email-templates]]
            [taoensso.timbre :as log])
  (:import (java.util Base64 UUID))
  (:gen-class))

(s/def :http/status nat-int?)
(s/def :http/headers map?)
(s/def :ring/response (s/keys :req-un [:http/status :http/headers]))
(s/def :ring/body-params map?)
(s/def :ring/route-params map?)
(s/def :ring/request (s/keys :opt [:ring/body-params :ring/route-params]))

(>defn- valid-password?
  "Check if the password is a valid."
  [password]
  [string? :ret boolean?]
  (= config/admin-password password))

(>defn- valid-credentials?
  "Validate if share-hash and edit-hash match"
  [share-hash edit-hash]
  [string? string? :ret boolean?]
  (let [authenticate-meeting (db/meeting-by-hash-private share-hash)]
    (= edit-hash (:meeting/edit-hash authenticate-meeting))))

(>defn- valid-discussion-hash?
  "Check if share hash and discussion id match."
  [share-hash discussion-id]
  [string? int? :ret boolean?]
  (not (nil? (db/agenda-by-meeting-hash-and-discussion-id share-hash discussion-id))))

(defn- deny-access
  "Return a 403 Forbidden to unauthorized access."
  ([]
   (deny-access "You are not allowed to access this resource."))
  ([message]
   (forbidden {:error message})))

(defn- fetch-meetings
  "Fetches meetings from the db and preparse them for transit via JSON."
  []
  (->> (db/all-meetings)
       (map first)))

(defn- ping
  "Route to ping the API. Used in our monitoring system."
  [_]
  (ok {:text "🧙‍♂️"}))

(defn- all-meetings
  "Returns all meetings from the db."
  [_req]
  (ok (fetch-meetings)))

(defn- add-hashes-to-meeting
  "Enrich meeting by its hashes."
  [meeting share-hash edit-hash]
  (assoc meeting :meeting/share-hash share-hash
                 :meeting/edit-hash edit-hash))

(defn- add-meeting
  "Adds a meeting and (optional) agendas to the database.
  Returns the newly-created meeting."
  [request]
  (let [{:keys [meeting nickname agendas]} (:body-params request)
        final-meeting (add-hashes-to-meeting meeting
                                             (.toString (UUID/randomUUID))
                                             (.toString (UUID/randomUUID)))
        author-id (db/add-user-if-not-exists nickname)
        meeting-id (db/add-meeting (assoc final-meeting :meeting/author author-id))
        created-meeting (db/meeting-private-data meeting-id)]
    (run! #(db/add-agenda-point (:title %) (:description %) meeting-id (:agenda/rank %)) agendas)
    (created "" {:new-meeting created-meeting})))

(defn- update-meeting!
  "Updates a meeting and its agendas."
  [{:keys [body-params]}]
  (let [nickname (:nickname body-params)
        user-id (db/add-user-if-not-exists nickname)
        meeting (:meeting body-params)
        ;; Do not let the user modify arbitrary db/id's
        meeting-id (-> meeting :meeting/share-hash db/meeting-by-hash :db/id)
        updated-meeting (assoc (select-keys meeting [:meeting/title :meeting/description]) :db/id meeting-id)
        updated-agendas (filter :agenda/discussion (:agendas body-params))
        new-agendas (remove :agenda/discussion (:agendas body-params))]
    (if (valid-credentials? (:meeting/share-hash meeting) (:meeting/edit-hash meeting))
      (do (db/update-meeting (assoc updated-meeting :meeting/author user-id))
          (doseq [agenda new-agendas]
            (db/add-agenda-point (:agenda/title agenda) (:agenda/description agenda)
                                 (:agenda/meeting agenda) (:agenda/rank agenda)))
          (doseq [agenda updated-agendas]
            (db/update-agenda agenda))
          (db/delete-agendas (:delete-agendas body-params) (:db/id meeting))
          (ok {:text "Your schnaq has been updated."}))
      (deny-access))))

(defn- update-single-agenda!
  "Update a single agenda, when the credentials are valid."
  [{:keys [body-params]}]
  (let [{:keys [agenda share-hash edit-hash]} body-params
        new-agenda (select-keys agenda [:db/id :agenda/title :agenda/description :agenda/rank])]
    (if (valid-credentials? share-hash edit-hash)
      (if-let [updated-agenda (suggestions/update-agenda! new-agenda share-hash)]
        (ok updated-agenda)
        (deny-access))
      (deny-access))))

(defn- delete-agenda!
  "Deletes a single agenda, when the credentials are valid."
  [{:keys [body-params]}]
  (let [{:keys [agenda-id share-hash edit-hash]} body-params]
    (if (valid-credentials? share-hash edit-hash)
      (do (db/delete-agendas [agenda-id] (:db/id (db/meeting-by-hash share-hash)))
          (ok {:message "Deletion executed."}))
      (deny-access))))

(defn- new-agenda!
  "Creates a single new agenda, when the credentials are valid."
  [{:keys [body-params]}]
  (let [{:keys [agenda share-hash edit-hash]} body-params]
    (if (valid-credentials? share-hash edit-hash)
      (if-let [new-agenda (suggestions/new-agenda! agenda share-hash)]
        (ok new-agenda)
        (deny-access))
      (deny-access))))

(defn- update-meeting-info!
  "Update a single agenda, when the credentials are right."
  [{:keys [body-params]}]
  (let [{:keys [meeting share-hash edit-hash nickname]} body-params
        author (db/add-user-if-not-exists nickname)
        new-meeting (assoc (select-keys meeting [:db/id :meeting/title :meeting/description])
                      :meeting/author author)]
    (if (valid-credentials? share-hash edit-hash)
      (if-let [updated-meeting (suggestions/update-meeting! new-meeting share-hash)]
        (ok updated-meeting)
        (deny-access))
      (deny-access))))

(defn- meeting-suggestions
  "Create suggestions for the change of a meeting."
  [request]
  (let [{:keys [meeting agendas delete-agendas nickname]} (:body-params request)
        user-id (db/add-user-if-not-exists nickname)
        updated-agendas (filter :agenda/discussion agendas)
        new-agendas (remove :agenda/discussion agendas)]
    (suggestions/new-meeting-suggestion meeting user-id)
    (suggestions/new-agenda-updates-suggestion updated-agendas user-id)
    (db/suggest-new-agendas! new-agendas user-id (:db/id meeting))
    (db/suggest-agenda-deletion! delete-agendas user-id)
    (created "" {:message "Successfully created suggestions!"})))

(defn- add-suggestion-feedback
  "Adds new feedback regarding a meeting."
  [{:keys [body-params]}]
  (let [{:keys [share-hash nickname feedback]} body-params
        user-id (db/add-user-if-not-exists nickname)
        meeting-id (:db/id (db/meeting-by-hash share-hash))]
    (if-not (string/blank? feedback)
      (ok {:message "Feedback created"
           :feedback/id (db/add-meeting-feedback feedback meeting-id user-id)})
      (bad-request {:error "You can not submit blank feedback."}))))

(>defn- load-meeting-suggestions
  "Return all suggestions for a given meeting by its share hash."
  [{:keys [route-params]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash edit-hash]} route-params]
    (if (valid-credentials? share-hash edit-hash)
      (let [agenda-suggestions (group-by :agenda.suggestion/type (db/all-agenda-suggestions share-hash))
            meeting-suggestions (db/all-meeting-suggestions share-hash)
            meeting-feedback (db/meeting-feedback-for share-hash)]
        (ok (assoc agenda-suggestions
              :meeting.suggestions/all meeting-suggestions
              :meeting.feedback/feedback meeting-feedback)))
      (deny-access))))

(defn- add-author
  "Adds an author to the database."
  [req]
  (let [author-name (:nickname (:body-params req))]
    (db/add-user-if-not-exists author-name)
    (ok {:text "POST successful"})))

(defn- meeting-by-hash
  "Returns a meeting, identified by its share-hash."
  [req]
  (let [hash (get-in req [:route-params :hash])]
    (ok (db/meeting-by-hash hash))))

(defn- meetings-by-hashes
  "Bulk loading of meetings. May be used when users asks for all the meetings
  they have access to. If only one meeting shall be loaded, compojure packs it
  into a single string:
  `{:params {:share-hashes \"4bdd505e-2fd7-4d35-bfea-5df260b82609\"}}`

  If multiple share-hashes are sent to the backend, compojure wraps them into a
  collection:
  `{:params {:share-hashes [\"bb328b5e-297d-4725-8c11-f1ed7db39109\"
                            \"4bdd505e-2fd7-4d35-bfea-5df260b82609\"]}}`"
  [req]
  (if-let [hashes (get-in req [:params :share-hashes])]
    (let [meetings (if (string? hashes)
                     [(db/meeting-by-hash hashes)]
                     (map db/meeting-by-hash hashes))]
      (if-not (or (nil? meetings) (= [nil] meetings))
        (ok {:meetings meetings})
        (not-found {:error "Meetings could not be found. Maybe you provided an invalid hash."})))
    (bad-request {:error "Meetings could not be loaded."})))

(defn- meeting-by-hash-as-admin
  "If user is authenticated, a meeting with an edit-hash is returned for further
  processing in the frontend."
  [{:keys [body-params]}]
  (let [share-hash (:share-hash body-params)
        edit-hash (:edit-hash body-params)]
    (if (valid-credentials? share-hash edit-hash)
      (ok {:meeting (add-hashes-to-meeting (db/meeting-by-hash share-hash)
                                           share-hash
                                           edit-hash)})
      (deny-access "You provided the wrong hashes to access this schnaq."))))

(defn- agendas-by-meeting-hash
  "Returns all agendas of a meeting, that matches the share-hash."
  [req]
  (let [meeting-hash (get-in req [:route-params :hash])
        agendas (db/agendas-by-meeting-hash meeting-hash)
        meta-info
        (into {}
              (map #(vector (:db/id %)
                            (db/number-of-statements-for-discussion (:db/id (:agenda/discussion %)))) agendas))]
    (ok {:agendas agendas
         :meta-info meta-info})))

(defn- agenda-by-meeting-hash-and-discussion-id
  "Returns the agenda tied to a certain discussion-id."
  [req]
  (let [discussion-id (Long/valueOf ^String (-> req :route-params :discussion-id))
        meeting-hash (-> req :route-params :meeting-hash)
        agenda-point (db/agenda-by-meeting-hash-and-discussion-id meeting-hash discussion-id)]
    (if agenda-point
      (ok agenda-point)
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
      (ok (discussion/sub-discussion-information statement-id (dialog-db/all-arguments-for-discussion discussion-id)))
      (bad-request {:error "The link you followed was invalid."}))))

(defn- start-discussion
  "Start a new discussion for an agenda point."
  [req]
  (let [discussion-id (Long/valueOf ^String (get-in req [:route-params :discussion-id]))
        username (get-in req [:query-params "username"])
        meeting-hash (get-in req [:query-params "meeting-hash"])]
    (if (valid-discussion-hash? meeting-hash discussion-id)
      (ok (->
            (dialog/start-discussion {:discussion/id discussion-id
                                      :user/nickname (db/canonical-username username)})
            processors/with-votes
            (processors/with-sub-discussion-information (dialog-db/all-arguments-for-discussion discussion-id))))
      (bad-request {:error "The link you followed was invalid"}))))

(defn- continue-discussion
  "Dispatches the wire-received events to the dialog.core backend."
  [{:keys [body-params]}]
  (let [[reaction args] (processors/with-canonical-usernames
                          (:payload body-params)
                          (:current-nickname body-params))
        meeting-hash (:meeting-hash body-params)
        discussion-id (:discussion-id body-params)]
    (if (valid-discussion-hash? meeting-hash discussion-id)
      (ok (->
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
            (ok {:operation :removed}))
        (do (add-vote-fn statement-id nickname)
            (if counter-vote
              (ok {:operation :switched})
              (ok {:operation :added})))))
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
  "Add new feedback from schnaqs frontend."
  [{:keys [body-params]}]
  (let [feedback (:feedback body-params)
        feedback-id (db/add-feedback! feedback)
        screenshot (:screenshot body-params)]
    (save-screenshot-if-provided! screenshot "resources/public/media/feedbacks/screenshots" feedback-id)
    (created "" {:feedback feedback})))

(defn- all-feedbacks
  "Returns all feedbacks from the db."
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (ok (db/all-feedbacks))
    (unauthorized)))

(>defn- send-invite-emails
  "Expects a list of recipients and the meeting which shall be send."
  [{:keys [body-params]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash edit-hash recipients share-link]} body-params]
    (if (valid-credentials? share-hash edit-hash)
      (ok (merge
            {:message "Emails sent successfully"}
            (emails/send-mails (email-templates :invitation/title)
                               (format (email-templates :invitation/body) share-link)
                               recipients)))
      (deny-access))))

(>defn- send-admin-center-link
  "Send URL to admin-center via mail to recipient."
  [{:keys [body-params]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash edit-hash recipient admin-center]} body-params
        meeting-title (:meeting/title (db/meeting-by-hash share-hash))]
    (if (valid-credentials? share-hash edit-hash)
      (ok (merge
            {:message "Emails sent successfully"}
            (emails/send-mails
              (format (email-templates :admin-center/title) meeting-title)
              (format (email-templates :admin-center/body) meeting-title admin-center)
              [recipient])))
      (deny-access))))

(>defn- request-demo
  "A user requests a demonstration via email."
  [{:keys [body-params]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [name email company phone]} body-params
        {:keys [failed-sendings]} (emails/send-mails
                                    (email-templates :demo-request/title)
                                    (format (email-templates :demo-request/body) name email company phone)
                                    ["info@dialogo.io"])]
    (if (empty? failed-sendings)
      (ok {:message "Demo requested."})
      (bad-request {:message "Ihre Anfrage konnte nicht bearbeitet werden, bitte versuchen Sie es erneut."}))))

;; -----------------------------------------------------------------------------
;; Discussion

(defn- starting-conclusions-with-processors
  "Returns starting conclusions for a discussion, with processors applied."
  [discussion-id]
  (-> (db/starting-conclusions-by-discussion discussion-id)
      processors/with-votes
      (processors/with-sub-discussion-information (dialog-db/all-arguments-for-discussion discussion-id))))

(defn- get-starting-conclusions
  "Return all starting-conclusions of a certain discussion if share-hash fits."
  [{:keys [body-params]}]
  (let [{:keys [share-hash discussion-id]} body-params]
    (if (valid-discussion-hash? share-hash discussion-id)
      (ok {:starting-conclusions (starting-conclusions-with-processors discussion-id)})
      (deny-access "Sie haben ungenügende Rechte um diese Diskussion zu betrachten."))))

(defn- add-starting-argument!
  "Adds a new starting argument to a discussion. Returns the list of starting-conclusions."
  [{:keys [body-params]}]
  (let [{:keys [share-hash discussion-id premises conclusion nickname]} body-params
        author-id (db/author-id-by-nickname nickname)]
    (if (valid-discussion-hash? share-hash discussion-id)
      (do (db/add-new-starting-argument! discussion-id author-id conclusion premises)
          (ok {:starting-conclusions (starting-conclusions-with-processors discussion-id)}))
      (deny-access "Sie haben nicht genügend Rechte um ein Argument in dieser Diskussion einzutragen."))))

;; -----------------------------------------------------------------------------
;; Analytics

(defn- number-of-meetings
  "Returns the number of all meetings."
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (ok {:meetings-num (db/number-of-meetings)})
    (deny-access)))

(defn- number-of-usernames
  "Returns the number of all meetings."
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (ok {:usernames-num (db/number-of-usernames)})
    (deny-access)))

(defn- agendas-per-meeting
  "Returns the average numbers of meetings"
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (ok {:average-agendas (float (db/average-number-of-agendas))})
    (deny-access)))

(defn- number-of-statements
  "Returns the number of statements"
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (ok {:statements-num (db/number-of-statements)})
    (deny-access)))

(defn- number-of-active-users
  "Returns the number of statements"
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (ok {:active-users-num (db/number-of-active-discussion-users)})
    (deny-access)))

(defn- statement-lengths-stats
  "Returns statistics about the statement length."
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (ok {:statement-length-stats (db/statement-length-stats)})
    (deny-access)))

(defn- argument-type-stats
  "Returns statistics about the statement length."
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (ok {:argument-type-stats (db/argument-type-stats)})
    (deny-access)))

(defn- all-stats
  "Returns all statistics at once."
  [{:keys [body-params]}]
  (if (valid-password? (:password body-params))
    (let [timestamp-since (toolbelt/now-minus-days (Integer/parseInt (:days-since body-params)))]
      (ok {:stats {:meetings-num (db/number-of-meetings timestamp-since)
                   :usernames-num (db/number-of-usernames timestamp-since)
                   :average-agendas (float (db/average-number-of-agendas timestamp-since))
                   :statements-num (db/number-of-statements timestamp-since)
                   :active-users-num (db/number-of-active-discussion-users timestamp-since)
                   :statement-length-stats (db/statement-length-stats timestamp-since)
                   :argument-type-stats (db/argument-type-stats timestamp-since)}}))
    (deny-access)))

(defn- check-credentials
  "Checks whether share-hash and edit-hash match."
  [{:keys [body-params]}]
  (let [share-hash (:share-hash body-params)
        edit-hash (:edit-hash body-params)]
    (ok {:valid-credentials? (valid-credentials? share-hash edit-hash)})))

(defn- graph-data-for-agenda
  "Delivers the graph-data needed to draw the graph in the frontend."
  [{:keys [body-params]}]
  (let [share-hash (:share-hash body-params)
        discussion-id (:discussion-id body-params)
        statements (db/all-statements-for-discussion discussion-id)
        starting-arguments (dialog-db/starting-arguments-by-discussion discussion-id)]
    (if (valid-discussion-hash? share-hash discussion-id)
      (ok {:graph {:nodes (discussion/nodes-for-agenda statements starting-arguments discussion-id share-hash)
                   :edges (discussion/links-for-agenda statements starting-arguments discussion-id)}})
      (bad-request {:error "Invalid meeting hash. You are not allowed to view this data."}))))

;; -----------------------------------------------------------------------------
;; Routes

(def ^:private not-found-msg
  "Error, page not found!")

(def ^:private common-routes
  "Common routes for all modes."
  (routes
    (GET "/agenda/:meeting-hash/:discussion-id" [] agenda-by-meeting-hash-and-discussion-id)
    (GET "/agendas/by-meeting-hash/:hash" [] agendas-by-meeting-hash)
    (GET "/meeting/by-hash/:hash" [] meeting-by-hash)
    (GET "/meeting/suggestions/:share-hash/:edit-hash" [] load-meeting-suggestions)
    (GET "/meetings/by-hashes" [] meetings-by-hashes)
    (GET "/ping" [] ping)
    (GET "/start-discussion/:discussion-id" [] start-discussion)
    (GET "/statement-infos" [] statement-infos)
    (POST "/agenda/delete" [] delete-agenda!)
    (POST "/agenda/new" [] new-agenda!)
    (POST "/agenda/update" [] update-single-agenda!)
    (POST "/author/add" [] add-author)
    (POST "/continue-discussion" [] continue-discussion)
    (POST "/credentials/validate" [] check-credentials)
    (POST "/discussion/conclusions/starting" [] get-starting-conclusions)
    (POST "/discussion/arguments/starting/add" [] add-starting-argument!)
    (POST "/emails/request-demo" [] request-demo)
    (POST "/emails/send-admin-center-link" [] send-admin-center-link)
    (POST "/emails/send-invites" [] send-invite-emails)
    (POST "/feedback/add" [] add-feedback)
    (POST "/feedbacks" [] all-feedbacks)
    (POST "/graph/discussion" [] graph-data-for-agenda)
    (POST "/meeting/add" [] add-meeting)
    (POST "/meeting/by-hash-as-admin" [] meeting-by-hash-as-admin)
    (POST "/meeting/info/update" [] update-meeting-info!)
    (POST "/meeting/feedback" [] add-suggestion-feedback)
    (POST "/meeting/suggestions" [] meeting-suggestions)
    (POST "/meeting/update" [] update-meeting!)
    (POST "/votes/down/toggle" [] toggle-downvote-statement)
    (POST "/votes/up/toggle" [] toggle-upvote-statement)
    ;; Analytics routes
    (POST "/analytics" [] all-stats)
    (POST "/analytics/active-users" [] number-of-active-users)
    (POST "/analytics/agendas-per-meeting" [] agendas-per-meeting)
    (POST "/analytics/argument-types" [] argument-type-stats)
    (POST "/analytics/meetings" [] number-of-meetings)
    (POST "/analytics/statement-lengths" [] statement-lengths-stats)
    (POST "/analytics/statements" [] number-of-statements)
    (POST "/analytics/usernames" [] number-of-usernames)))


(def ^:private development-routes
  "Exclusive Routes only available outside of production."
  (routes
    (GET "/meetings" [] all-meetings)))

(def ^:private app-routes
  "Building routes for app."
  (if schnaq-core/production-mode?
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
  (log/info "Welcome to Schnaq's Backend 🧙")
  (log/info (format "Build Hash: %s" config/build-hash))
  (log/info (format "Environment: %s" config/env-mode))
  (log/info (format "Port: %s" (:port config/api)))
  (log/info (format "Database Name: %s" config/db-name)))

(def allowed-origin
  "Regular expression, which defines the allowed origins for API requests."
  #"^((https?:\/\/)?(.*\.)?(schnaq\.com))($|\/.*$)")

(defn -main
  "This is our main entry point for the REST API Server."
  [& _args]
  (let [port (:port config/api)
        allowed-origins [allowed-origin]
        allowed-origins' (if schnaq-core/production-mode? allowed-origins (conj allowed-origins #".*"))]
    ; Run the server with Ring.defaults middleware
    (schnaq-core/-main)
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
