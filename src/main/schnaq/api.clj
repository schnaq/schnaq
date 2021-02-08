(ns schnaq.api
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [compojure.core :refer [GET POST routes wrap-routes]]
            [compojure.route :as route]
            [ghostwheel.core :refer [>defn- ?]]
            [org.httpkit.server :as server]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.util.http-response :refer [ok created bad-request unauthorized]]
            [schnaq.auth :as auth]
            [schnaq.config :as config]
            [schnaq.core :as schnaq-core]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.discussion :as discussion]
            [schnaq.emails :as emails]
            [schnaq.export :as export]
            [schnaq.media :as media]
            [schnaq.meeting.database :as db]
            [schnaq.meeting.meta-info :as meta-info]
            [schnaq.meeting.processors :as processors]
            [schnaq.toolbelt :as toolbelt]
            [schnaq.translations :refer [email-templates]]
            [schnaq.validator :as validator]
            [taoensso.timbre :as log])
  (:import (java.util Base64 UUID))
  (:gen-class))

(s/def :http/status nat-int?)
(s/def :http/headers map?)
(s/def :ring/response (s/keys :req-un [:http/status :http/headers]))
(s/def :ring/body-params map?)
(s/def :ring/route-params map?)
(s/def :ring/request (s/keys :opt [:ring/body-params :ring/route-params]))

(def ^:private invalid-rights-message "Sie haben nicht genügend Rechte, um diese Diskussion zu betrachten.")

(defn- ping
  "Route to ping the API. Used in our monitoring system."
  [_]
  (ok {:text "🧙‍♂️"}))

(defn- all-schnaqs
  "Returns all meetings from the db."
  [_req]
  (ok (discussion-db/all-discussions)))

(defn- add-schnaq
  "Adds a discussion to the database. Returns the newly-created discussion."
  [request]
  (let [{:keys [discussion nickname public-discussion?]} (:body-params request)
        discussion-data {:discussion/title (:discussion/title discussion)
                         :discussion/share-hash (.toString (UUID/randomUUID))
                         :discussion/edit-hash (.toString (UUID/randomUUID))
                         :discussion/author (db/add-user-if-not-exists nickname)}
        new-discussion-id (discussion-db/new-discussion discussion-data public-discussion?)]
    (if new-discussion-id
      (let [created-discussion (discussion-db/private-discussion-data new-discussion-id)]
        (log/info "Discussion created: " new-discussion-id " - "
                  (:discussion/title created-discussion) " – Public? " public-discussion?)
        (created "" {:new-discussion created-discussion}))
      (do
        (log/info "Did not create discussion from following data:\n" discussion-data)
        (bad-request "The input you provided could not be used to create a discussion.")))))

(defn- add-author
  "Adds an author to the database."
  [req]
  (let [author-name (:nickname (:body-params req))]
    (db/add-user-if-not-exists author-name)
    (ok {:text "POST successful"})))

(defn- discussion-by-hash
  "Returns a meeting, identified by its share-hash."
  [req]
  (let [hash (get-in req [:route-params :hash])]
    (if (validator/valid-discussion? hash)
      (ok (discussion-db/discussion-by-share-hash hash))
      (validator/deny-access))))

(defn- schnaqs-by-hashes
  "Bulk loading of discussions. May be used when users asks for all the schnaqs
  they have access to. If only one schnaq shall be loaded, compojure packs it
  into a single string:
  `{:params {:share-hashes \"4bdd505e-2fd7-4d35-bfea-5df260b82609\"}}`

  If multiple share-hashes are sent to the backend, compojure wraps them into a
  collection:
  `{:params {:share-hashes [\"bb328b5e-297d-4725-8c11-f1ed7db39109\"
                            \"4bdd505e-2fd7-4d35-bfea-5df260b82609\"]}}`"
  [req]
  (if-let [hashes (get-in req [:params :share-hashes])]
    (let [hashes-list (if (string? hashes) [hashes] hashes)]
      (ok {:discussions (discussion-db/valid-discussions-by-hashes hashes-list)}))
    (bad-request {:error "Schnaqs could not be loaded."})))

(defn- public-schnaqs
  "Return all public meetings."
  [_req]
  (ok {:discussions (discussion-db/public-discussions)}))

(defn- schnaq-by-hash-as-admin
  "If user is authenticated, a meeting with an edit-hash is returned for further
  processing in the frontend."
  [{:keys [body-params]}]
  (let [share-hash (:share-hash body-params)
        edit-hash (:edit-hash body-params)]
    (if (validator/valid-credentials? share-hash edit-hash)
      (ok {:discussion (discussion-db/discussion-by-share-hash-private share-hash)})
      (validator/deny-access "You provided the wrong hashes to access this schnaq."))))

(defn- delete-statements!
  "Deletes the passed list of statements if the admin-rights are fitting.
  Important: Needs to check whether the statement-id really belongs to the discussion with
  the passed edit-hash."
  [{:keys [body-params]}]
  (let [{:keys [share-hash edit-hash statement-ids]} body-params]
    (if (validator/valid-credentials? share-hash edit-hash)
      ;; could optimize with a collection query here
      (if (every? #(discussion-db/check-valid-statement-id-for-discussion % share-hash) statement-ids)
        (do (discussion-db/delete-statements! statement-ids)
            (ok {:deleted-statements statement-ids}))
        (bad-request {:error "You are trying to delete statements, without the appropriate rights"}))
      (validator/deny-access "You do not have the rights to access this action."))))

(defn- delete-schnaq!
  "Sets the state of a schnaq to delete. Should be only available to superusers (admins)."
  [{:keys [body-params]}]
  (let [{:keys [share-hash]} body-params]
    (if (discussion-db/delete-discussion share-hash)
      (ok {:share-hash share-hash})
      (bad-request {:error "An error occurred, while deleting the schnaq."}))))

;; -----------------------------------------------------------------------------
;; Votes

(defn- toggle-vote-statement
  "Toggle up- or downvote of statement."
  [{:keys [share-hash statement-id nickname]} add-vote-fn remove-vote-fn check-vote-fn counter-check-vote-fn]
  (if (validator/valid-discussion-and-statement? statement-id share-hash)
    (let [nickname (db/canonical-username nickname)
          vote (check-vote-fn statement-id nickname)
          counter-vote (counter-check-vote-fn statement-id nickname)]
      (log/debug "Triggered Vote on Statement by " nickname)
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
    (log/info "Schnaq Feedback created")
    (created "" {:feedback feedback})))

(defn- all-feedbacks
  "Returns all feedbacks from the db."
  [{:keys [body-params]}]
  (if (validator/valid-password? (:password body-params))
    (ok (db/all-feedbacks))
    (unauthorized)))

(>defn- send-invite-emails
  "Expects a list of recipients and the meeting which shall be send."
  [{:keys [body-params]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash edit-hash recipients share-link]} body-params
        meeting-title (:discussion/title (discussion-db/discussion-by-share-hash share-hash))]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/debug "Invite Emails for some meeting sent")
          (ok (merge
                {:message "Emails sent successfully"}
                (emails/send-mails
                  (format (email-templates :invitation/title) meeting-title)
                  (format (email-templates :invitation/body) meeting-title share-link)
                  recipients))))
      (validator/deny-access))))

(>defn- send-admin-center-link
  "Send URL to admin-center via mail to recipient."
  [{:keys [body-params]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash edit-hash recipient admin-center]} body-params
        meeting-title (:discussion/title (discussion-db/discussion-by-share-hash share-hash))]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/debug "Send admin link for meeting " meeting-title " via E-Mail")
          (ok (merge
                {:message "Emails sent successfully"}
                (emails/send-mails
                  (format (email-templates :admin-center/title) meeting-title)
                  (format (email-templates :admin-center/body) meeting-title admin-center)
                  [recipient]))))
      (validator/deny-access))))

;; -----------------------------------------------------------------------------
;; Discussion

(defn- with-statement-meta
  "Returns a data structure, where all statements have been enhanced with meta-information."
  [data share-hash]
  (-> data
      processors/hide-deleted-statement-content
      processors/with-votes
      (processors/with-sub-discussion-information (discussion-db/all-arguments-for-discussion share-hash))))

(defn- starting-conclusions-with-processors
  "Returns starting conclusions for a discussion, with processors applied."
  [share-hash]
  (with-statement-meta (discussion-db/starting-statements share-hash) share-hash))

(defn- get-starting-conclusions
  "Return all starting-conclusions of a certain discussion if share-hash fits."
  [{:keys [body-params]}]
  (let [{:keys [share-hash]} body-params]
    (if (validator/valid-discussion? share-hash)
      (ok {:starting-conclusions (starting-conclusions-with-processors share-hash)})
      (validator/deny-access invalid-rights-message))))

(defn- get-statements-for-conclusion
  "Return all premises and fitting undercut-premises for a given statement."
  [{:keys [body-params]}]
  (let [{:keys [share-hash selected-statement]} body-params]
    (if (validator/valid-discussion? share-hash)
      (ok (with-statement-meta
            {:premises (discussion/premises-for-conclusion-id (:db/id selected-statement))
             :undercuts (discussion/premises-undercutting-argument-with-premise-id (:db/id selected-statement))}
            share-hash))
      (validator/deny-access invalid-rights-message))))

(defn- get-statement-info
  "Return the sought after conclusion (by id) and the following premises / undercuts."
  [{:keys [body-params]}]
  (let [{:keys [share-hash statement-id]} body-params]
    (if (validator/valid-discussion-and-statement? statement-id share-hash)
      (ok (with-statement-meta
            {:conclusion (discussion-db/get-statement statement-id)
             :premises (discussion/premises-for-conclusion-id statement-id)
             :undercuts (discussion/premises-undercutting-argument-with-premise-id statement-id)}
            share-hash))
      (validator/deny-access invalid-rights-message))))

(defn- add-starting-statement!
  "Adds a new starting argument to a discussion. Returns the list of starting-conclusions."
  [{:keys [body-params]}]
  (let [{:keys [share-hash statement nickname]} body-params
        user-id (db/user-by-nickname nickname)]
    (if (validator/valid-discussion? share-hash)
      (do (discussion-db/add-starting-statement! share-hash user-id statement)
          (log/info "Starting statement added for discussion" share-hash)
          (ok {:starting-conclusions (starting-conclusions-with-processors share-hash)}))
      (validator/deny-access invalid-rights-message))))

(defn- react-to-any-statement!
  "Adds a support or attack regarding a certain statement."
  [{:keys [body-params]}]
  (let [{:keys [share-hash conclusion-id nickname premise reaction]} body-params
        user-id (db/user-by-nickname nickname)]
    (if (validator/valid-discussion-and-statement? conclusion-id share-hash)
      (do (log/info "Statement added as reaction to statement" conclusion-id)
          (ok (with-statement-meta
                {:new-argument
                 (if (= :attack reaction)
                   (discussion-db/attack-statement! share-hash user-id conclusion-id premise)
                   (discussion-db/support-statement! share-hash user-id conclusion-id premise))}
                share-hash)))
      (validator/deny-access invalid-rights-message))))

;; -----------------------------------------------------------------------------
;; Analytics

(defn- number-of-meetings
  "Returns the number of all meetings."
  [{:keys [body-params]}]
  (if (validator/valid-password? (:password body-params))
    (ok {:meetings-num (db/number-of-meetings)})
    (validator/deny-access)))

(defn- last-meeting-date
  "Returns the date of the last meeting created."
  [{:keys [body-params]}]
  (if (validator/valid-password? (:password body-params))
    ;; todo disabled until analytics rework
    (ok {:last-created 12341234})
    (validator/deny-access)))

(defn- number-of-usernames
  "Returns the number of all meetings."
  [{:keys [body-params]}]
  (if (validator/valid-password? (:password body-params))
    (ok {:usernames-num (db/number-of-usernames)})
    (validator/deny-access)))

(defn- agendas-per-meeting
  "Returns the average numbers of meetings"
  [{:keys [body-params]}]
  (if (validator/valid-password? (:password body-params))
    (ok {:average-agendas (float (db/average-number-of-agendas))})
    (validator/deny-access)))

(defn- number-of-statements
  "Returns the number of statements"
  [{:keys [body-params]}]
  (if (validator/valid-password? (:password body-params))
    (ok {:statements-num (db/number-of-statements)})
    (validator/deny-access)))

(defn- number-of-active-users
  "Returns the number of statements"
  [{:keys [body-params]}]
  (if (validator/valid-password? (:password body-params))
    (ok {:active-users-num (db/number-of-active-discussion-users)})
    (validator/deny-access)))

(defn- statement-lengths-stats
  "Returns statistics about the statement length."
  [{:keys [body-params]}]
  (if (validator/valid-password? (:password body-params))
    (ok {:statement-length-stats (db/statement-length-stats)})
    (validator/deny-access)))

(defn- argument-type-stats
  "Returns statistics about the statement length."
  [{:keys [body-params]}]
  (if (validator/valid-password? (:password body-params))
    (ok {:argument-type-stats (db/argument-type-stats)})
    (validator/deny-access)))

(defn- all-stats
  "Returns all statistics at once."
  [{:keys [body-params]}]
  (if (validator/valid-password? (:password body-params))
    (let [timestamp-since (toolbelt/now-minus-days (Integer/parseInt (:days-since body-params)))]
      (ok {:stats {:meetings-num (db/number-of-meetings timestamp-since)
                   :usernames-num (db/number-of-usernames timestamp-since)
                   :average-agendas (float (db/average-number-of-agendas timestamp-since))
                   :statements-num (db/number-of-statements timestamp-since)
                   :active-users-num (db/number-of-active-discussion-users timestamp-since)
                   :statement-length-stats (db/statement-length-stats timestamp-since)
                   :argument-type-stats (db/argument-type-stats timestamp-since)}}))
    (validator/deny-access)))

(defn- check-credentials
  "Checks whether share-hash and edit-hash match."
  [{:keys [body-params]}]
  (let [share-hash (:share-hash body-params)
        edit-hash (:edit-hash body-params)]
    (ok {:valid-credentials? (validator/valid-credentials? share-hash edit-hash)})))

(defn- graph-data-for-agenda
  "Delivers the graph-data needed to draw the graph in the frontend."
  [{:keys [body-params]}]
  (let [share-hash (:share-hash body-params)]
    (if (validator/valid-discussion? share-hash)
      (let [statements (discussion-db/all-statements-for-graph share-hash)
            starting-statements (discussion-db/starting-statements share-hash)
            edges (discussion/links-for-starting statements starting-statements share-hash)
            controversy-vals (discussion/calculate-controversy edges)]
        (ok {:graph {:nodes (discussion/nodes-for-agenda statements starting-statements share-hash)
                     :edges edges
                     :controversy-values controversy-vals}}))
      (bad-request {:error "Invalid meeting hash. You are not allowed to view this data."}))))

(defn- export-txt-data
  "Exports the discussion data as a string."
  [{:keys [params]}]
  (let [{:keys [share-hash]} params]
    (if (validator/valid-discussion? share-hash)
      (do (log/info "User is generating a txt export for discussion" share-hash)
          (ok {:string-representation (export/generate-text-export share-hash)}))
      (validator/deny-access invalid-rights-message))))

;; -----------------------------------------------------------------------------
;; Routes
;; About applying middlewares: We need to chain `wrap-routes` calls, because
;; compojure can't handle natively more than one custom middleware. reitit has a
;; vector of middlewares, where these functions can simply put into.
;; See more on wrap-routes: https://github.com/weavejester/compojure/issues/192

(def ^:private not-found-msg
  "Error, page not found!")

(def ^:private common-routes
  "Common routes for all modes."
  (routes
    (GET "/export/txt" [] export-txt-data)
    (GET "/ping" [] ping)
    (GET "/schnaq/by-hash/:hash" [] discussion-by-hash)
    (GET "/schnaq/meta-info/by-hash/:hash" [] meta-info/get-discussion-meta-info)
    (GET "/schnaqs/by-hashes" [] schnaqs-by-hashes)
    (GET "/schnaqs/public" [] public-schnaqs)
    (-> (POST "/admin/schnaq/delete" [] delete-schnaq!)
        (wrap-routes auth/is-admin-middleware)
        (wrap-routes auth/auth-middleware)
        (wrap-routes auth/wrap-jwt-authentication))
    (POST "/admin/statements/delete" [] delete-statements!)
    (POST "/author/add" [] add-author)
    (POST "/credentials/validate" [] check-credentials)
    (POST "/discussion/conclusions/starting" [] get-starting-conclusions)
    (POST "/discussion/react-to/statement" [] react-to-any-statement!)
    (POST "/discussion/statement/info" [] get-statement-info)
    (POST "/discussion/statements/for-conclusion" [] get-statements-for-conclusion)
    (POST "/discussion/statements/starting/add" [] add-starting-statement!)
    (POST "/emails/send-admin-center-link" [] send-admin-center-link)
    (POST "/emails/send-invites" [] send-invite-emails)
    (POST "/feedback/add" [] add-feedback)
    (POST "/feedbacks" [] all-feedbacks)
    (POST "/graph/discussion" [] graph-data-for-agenda)
    (POST "/header-image/image" [] media/set-preview-image)
    (POST "/schnaq/add" [] add-schnaq)
    (POST "/schnaq/by-hash-as-admin" [] schnaq-by-hash-as-admin)
    (POST "/votes/down/toggle" [] toggle-downvote-statement)
    (POST "/votes/up/toggle" [] toggle-upvote-statement)
    ;; Analytics routes
    (POST "/analytics" [] all-stats)
    (POST "/analytics/active-users" [] number-of-active-users)
    (POST "/analytics/agendas-per-meeting" [] agendas-per-meeting)
    (POST "/analytics/argument-types" [] argument-type-stats)
    (POST "/analytics/meetings" [] number-of-meetings)
    (POST "/analytics/last-meetings" [] last-meeting-date)
    (POST "/analytics/statement-lengths" [] statement-lengths-stats)
    (POST "/analytics/statements" [] number-of-statements)
    (POST "/analytics/usernames" [] number-of-usernames)))

(def ^:private development-routes
  "Exclusive Routes only available outside of production."
  (routes
    (GET "/schnaqs" [] all-schnaqs)))

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
    ; Run the server with Ring.defaults middle-ware
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
