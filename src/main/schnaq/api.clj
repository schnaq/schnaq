(ns schnaq.api
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [compojure.core :refer [GET POST PUT DELETE routes wrap-routes]]
            [compojure.route :as route]
            [ghostwheel.core :refer [>defn-]]
            [org.httpkit.server :as server]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.util.http-response :refer [ok created bad-request]]
            [schnaq.api.analytics :as analytics]
            [schnaq.api.hub :as hub]
            [schnaq.api.user :as user-api]
            [schnaq.auth :as auth]
            [schnaq.config :as config]
            [schnaq.config.keycloak :as keycloak-config]
            [schnaq.core :as schnaq-core]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.hub :as hub-db]
            [schnaq.database.main :as db]
            [schnaq.database.reaction :as reaction-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.discussion :as discussion]
            [schnaq.emails :as emails]
            [schnaq.export :as export]
            [schnaq.media :as media]
            [schnaq.processors :as processors]
            [schnaq.s3 :as s3]
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

(defn- extract-user
  "Returns a user-id, either from nickname if anonymous user or from identity, if jwt token is present."
  [nickname identity]
  (let [nickname (user-db/user-by-nickname nickname)
        registered-user (:db/id (db/fast-pull [:user.registered/keycloak-id (:sub identity)]))]
    (or registered-user nickname)))

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
  [{:keys [body-params identity]}]
  (let [{:keys [discussion nickname public-discussion? hub-exclusive? origin]} body-params
        keycloak-id (:sub identity)
        authorized-for-hub (some #(= % origin) (:groups identity))
        author (if keycloak-id
                 [:user.registered/keycloak-id keycloak-id]
                 (user-db/add-user-if-not-exists nickname))
        discussion-data (cond-> {:discussion/title (:discussion/title discussion)
                                 :discussion/share-hash (.toString (UUID/randomUUID))
                                 :discussion/edit-hash (.toString (UUID/randomUUID))
                                 :discussion/author author}
                                (and hub-exclusive? authorized-for-hub)
                                (assoc :discussion/hub-origin [:hub/keycloak-name origin]))
        new-discussion-id (discussion-db/new-discussion discussion-data public-discussion?)]
    (if new-discussion-id
      (let [created-discussion (discussion-db/private-discussion-data new-discussion-id)]
        (when (and hub-exclusive? origin authorized-for-hub)
          (hub-db/add-discussions-to-hub [:hub/keycloak-name origin] [new-discussion-id]))
        (log/info "Discussion created: " new-discussion-id " - "
                  (:discussion/title created-discussion) " – Public? " public-discussion?
                  "Exclusive?" hub-exclusive? "for" origin)
        (created "" {:new-discussion created-discussion}))
      (do
        (log/info "Did not create discussion from following data:\n" discussion-data)
        (bad-request "The input you provided could not be used to create a discussion.")))))

(defn- add-author
  "Adds an author to the database."
  [req]
  (let [author-name (:nickname (:body-params req))]
    (user-db/add-user-if-not-exists author-name)
    (ok {:text "POST successful"})))

(defn- discussion-by-hash
  "Returns a meeting, identified by its share-hash."
  [{:keys [params identity]}]
  (let [hash (:hash params)
        keycloak-id (:sub identity)]
    (if (validator/valid-discussion? hash)
      (ok (processors/add-meta-info-to-schnaq
            (if (and keycloak-id (validator/user-schnaq-admin? hash keycloak-id))
              (discussion-db/discussion-by-share-hash-private hash)
              (discussion-db/discussion-by-share-hash hash))))
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
      (ok {:discussions
           (map processors/add-meta-info-to-schnaq
                (discussion-db/valid-discussions-by-hashes hashes-list))}))
    (bad-request {:error "Schnaqs could not be loaded."})))

(defn- public-schnaqs
  "Return all public meetings."
  [_req]
  (ok {:discussions (map processors/add-meta-info-to-schnaq (discussion-db/public-discussions))}))

(defn- schnaq-by-hash-as-admin
  "If user is authenticated, a meeting with an edit-hash is returned for further
  processing in the frontend."
  [{:keys [body-params]}]
  (let [share-hash (:share-hash body-params)
        edit-hash (:edit-hash body-params)]
    (if (validator/valid-credentials? share-hash edit-hash)
      (ok {:discussion (discussion-db/discussion-by-share-hash-private share-hash)})
      (validator/deny-access "You provided the wrong hashes to access this schnaq."))))

(defn- make-discussion-read-only!
  "Makes a discussion read-only if discussion-admin credentials are there."
  [{:keys [body-params]}]
  (let [{:keys [share-hash edit-hash]} body-params]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/info "Setting discussion to read-only: " share-hash)
          (discussion-db/set-discussion-read-only share-hash)
          (ok {:share-hash share-hash}))
      (validator/deny-access "You do not have the rights to access this action."))))

(defn- make-discussion-writeable!
  "Makes a discussion writeable if discussion-admin credentials are there."
  [{:keys [body-params]}]
  (let [{:keys [share-hash edit-hash]} body-params]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/info "Removing read-only from discussion: " share-hash)
          (discussion-db/remove-read-only share-hash)
          (ok {:share-hash share-hash}))
      (validator/deny-access "You do not have the rights to access this action."))))

(defn- disable-pro-con! [{:keys [body-params]}]
  (let [{:keys [disable-pro-con? share-hash edit-hash]} body-params]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/info "Setting \"disable-pro-con option\" to" disable-pro-con? "for schnaq:" share-hash)
          (discussion-db/set-disable-pro-con share-hash disable-pro-con?)
          (ok {:share-hash share-hash}))
      (validator/deny-access "You do not have the rights to access this action."))))

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
  [{:keys [share-hash statement-id nickname]} identity
   add-vote-fn remove-vote-fn check-vote-fn counter-check-vote-fn]
  (if (validator/valid-discussion-and-statement? statement-id share-hash)
    (let [user-id (extract-user nickname identity)
          vote (check-vote-fn statement-id user-id)
          counter-vote (counter-check-vote-fn statement-id user-id)]
      (log/debug "Triggered Vote on Statement by " user-id)
      (if vote
        (do (remove-vote-fn statement-id user-id)
            (ok {:operation :removed}))
        (do (add-vote-fn statement-id user-id)
            (if counter-vote
              (ok {:operation :switched})
              (ok {:operation :added})))))
    (bad-request {:error "Vote could not be registered"})))

(defn- toggle-upvote-statement
  "Upvote if no upvote has been made, otherwise remove upvote for statement."
  [{:keys [body-params identity]}]
  (toggle-vote-statement
    body-params identity reaction-db/upvote-statement! reaction-db/remove-upvote!
    reaction-db/did-user-upvote-statement reaction-db/did-user-downvote-statement))

(defn- toggle-downvote-statement
  "Upvote if no upvote has been made, otherwise remove upvote for statement."
  [{:keys [body-params identity]}]
  (toggle-vote-statement
    body-params identity reaction-db/downvote-statement! reaction-db/remove-downvote!
    reaction-db/did-user-downvote-statement reaction-db/did-user-upvote-statement))


;; -----------------------------------------------------------------------------
;; Feedback

(>defn- upload-screenshot!
  "Stores a screenshot from a feedback in s3."
  [screenshot file-name]
  [string? (s/or :number number? :string string?) :ret string?]
  (let [[_header image] (string/split screenshot #",")
        #^bytes decodedBytes (.decode (Base64/getDecoder) ^String image)]
    (s3/upload-stream
      :feedbacks/screenshots
      (io/input-stream decodedBytes)
      (format "%s.png" file-name)
      {:content-length (count decodedBytes)})))

(defn- add-feedback
  "Add new feedback from schnaqs frontend."
  [{:keys [body-params]}]
  (let [feedback (:feedback body-params)
        feedback-id (db/add-feedback! feedback)
        screenshot (:screenshot body-params)]
    (when screenshot
      (upload-screenshot! screenshot feedback-id))
    (log/info "Feedback created")
    (created "" {:feedback feedback})))

(defn- all-feedbacks
  "Returns all feedbacks from the db."
  [_]
  (ok (db/all-feedbacks)))

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

(>defn- add-creation-secret
  "Add creation-secret to a collection of statements. Only add to matching target-id."
  [statements target-id]
  [(s/coll-of ::specs/statement) :db/id :ret (s/coll-of ::specs/statement)]
  (map #(if (= target-id (:db/id %))
          (merge % (db/fast-pull target-id '[:statement/creation-secret]))
          %)
       statements))

(defn- valid-statements-with-votes
  "Returns a data structure, where all statements have been checked for being present and enriched with vote data."
  [data]
  (-> data
      processors/hide-deleted-statement-content
      processors/with-votes))

(defn- starting-conclusions-with-processors
  "Returns starting conclusions for a discussion, with processors applied.
  Optionally a statement-id can be passed to enrich the statement with its creation-secret."
  ([share-hash]
   (let [starting-statements (discussion-db/starting-statements share-hash)
         statement-ids (map :db/id starting-statements)
         info-map (discussion-db/child-node-info statement-ids)]
     (map
       #(assoc % :meta/sub-discussion-info (get info-map (:db/id %)))
       (-> starting-statements
           processors/hide-deleted-statement-content
           processors/with-votes))))
  ([share-hash secret-statement-id]
   (add-creation-secret (starting-conclusions-with-processors share-hash) secret-statement-id)))

(defn- with-sub-discussion-info
  [statements]
  (let [statement-ids (map :db/id statements)
        info-map (discussion-db/child-node-info statement-ids)]
    (map #(assoc % :meta/sub-discussion-info (get info-map (:db/id %))) statements)))

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
      (ok (valid-statements-with-votes
            {:premises (with-sub-discussion-info (discussion-db/children-for-statement (:db/id selected-statement)))}))
      (validator/deny-access invalid-rights-message))))

(defn- search-schnaq
  "Search through any valid schnaq."
  [{:keys [params]}]
  (let [{:keys [share-hash search-string]} params]
    (if (validator/valid-discussion? share-hash)
      (ok {:matching-statements (-> (discussion-db/search-schnaq share-hash search-string)
                                    with-sub-discussion-info
                                    valid-statements-with-votes)})
      (validator/deny-access))))

(defn- get-statement-info
  "Return the sought after conclusion (by id) and the following children."
  [{:keys [body-params]}]
  (let [{:keys [share-hash statement-id]} body-params]
    (if (validator/valid-discussion-and-statement? statement-id share-hash)
      (ok (valid-statements-with-votes
            {:conclusion (first (-> [(db/fast-pull statement-id discussion-db/statement-pattern)]
                                    with-sub-discussion-info
                                    (toolbelt/pull-key-up :db/ident)))
             :premises (with-sub-discussion-info (discussion-db/children-for-statement statement-id))}))
      (validator/deny-access invalid-rights-message))))

(defn- add-starting-statement!
  "Adds a new starting statement to a discussion. Returns the list of starting-conclusions."
  [{:keys [body-params identity]}]
  (let [{:keys [share-hash statement nickname]} body-params
        keycloak-id (:sub identity)
        user-id (if keycloak-id
                  [:user.registered/keycloak-id keycloak-id]
                  (user-db/user-by-nickname nickname))]
    (if (validator/valid-writeable-discussion? share-hash)
      (let [new-starting-id (discussion-db/add-starting-statement! share-hash user-id statement keycloak-id)]
        (log/info "Starting statement added for discussion" share-hash)
        (ok {:starting-conclusions (starting-conclusions-with-processors share-hash new-starting-id)}))
      (validator/deny-access invalid-rights-message))))

(defn- react-to-any-statement!
  "Adds a support or attack regarding a certain statement."
  [{:keys [body-params identity]}]
  (let [{:keys [share-hash conclusion-id nickname premise reaction]} body-params
        keycloak-id (:sub identity)
        user-id (if keycloak-id
                  [:user.registered/keycloak-id keycloak-id]
                  (user-db/user-by-nickname nickname))
        statement-type (case reaction
                         :attack :statement.type/attack
                         :support :statement.type/support
                         :statement.type/neutral)]
    (if (validator/valid-writeable-discussion-and-statement? conclusion-id share-hash)
      (do (log/info "Statement added as reaction to statement" conclusion-id)
          (ok (valid-statements-with-votes
                {:new-statement
                 (discussion-db/react-to-statement! share-hash user-id conclusion-id premise statement-type keycloak-id)})))
      (validator/deny-access invalid-rights-message))))

(defn- check-credentials
  "Checks whether share-hash and edit-hash match.
  If the user is logged in and the credentials are valid, they are added as an admin."
  [{:keys [params identity]}]
  (let [{:keys [share-hash edit-hash]} params
        valid-credentials? (validator/valid-credentials? share-hash edit-hash)
        keycloak-id (:sub identity)]
    (when (and valid-credentials? keycloak-id)
      (discussion-db/add-admin-to-discussion share-hash keycloak-id))
    (ok {:valid-credentials? valid-credentials?})))

(defn- graph-data-for-agenda
  "Delivers the graph-data needed to draw the graph in the frontend."
  [{:keys [body-params]}]
  (let [share-hash (:share-hash body-params)]
    (if (validator/valid-discussion? share-hash)
      (let [statements (discussion-db/all-statements-for-graph share-hash)
            starting-statements (discussion-db/starting-statements share-hash)
            edges (discussion/links-for-starting starting-statements share-hash)
            controversy-vals (discussion/calculate-controversy edges)]
        (ok {:graph {:nodes (discussion/nodes-for-agenda statements share-hash)
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

(defn- edit-statement!
  "Edits the content (and possibly type) of a statement, when the user is the registered author."
  [{:keys [params identity]}]
  (let [{:keys [statement-id statement-type new-content share-hash]} params
        user-identity (:sub identity)
        statement (db/fast-pull statement-id [:db/id
                                              :statement/parent
                                              {:statement/author [:user.registered/keycloak-id]}
                                              :statement/deleted?])]
    (if (= user-identity (-> statement :statement/author :user.registered/keycloak-id))
      (if (and (validator/valid-writeable-discussion-and-statement? statement-id share-hash)
               (not (:statement/deleted? statement)))
        (ok {:updated-statement (discussion-db/change-statement-text-and-type statement statement-type new-content)})
        (bad-request {:error "You can not edit a closed / deleted discussion or statement."}))
      (validator/deny-access invalid-rights-message))))

(defn- delete-statement!
  "Deletes a statement, when the user is the registered author."
  [{:keys [params identity]}]
  (let [{:keys [statement-id share-hash]} params
        user-identity (:sub identity)
        statement (db/fast-pull statement-id [:db/id
                                              :statement/parent
                                              {:statement/author [:user.registered/keycloak-id]}
                                              :statement/deleted?])]
    (if (= user-identity (-> statement :statement/author :user.registered/keycloak-id))
      (if (and (validator/valid-writeable-discussion-and-statement? statement-id share-hash)
               (not (:statement/deleted? statement)))
        (do (discussion-db/delete-statements! [statement-id])
            (ok {:deleted-statement statement-id}))
        (bad-request {:error "You can not Delete a closed / deleted discussion or statement."}))
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
  "Common routes for all modes, already wrapped with jwt-parsing."
  (->
    (routes
      (GET "/export/txt" [] export-txt-data)
      (GET "/ping" [] ping)
      (GET "/schnaq/by-hash/:hash" [] discussion-by-hash)
      (GET "/schnaq/search" [] search-schnaq)
      (GET "/schnaqs/by-hashes" [] schnaqs-by-hashes)
      (GET "/schnaqs/public" [] public-schnaqs)
      (-> (GET "/admin/feedbacks" [] all-feedbacks)
          (wrap-routes auth/is-admin-middleware)
          (wrap-routes auth/auth-middleware))
      (-> (DELETE "/admin/schnaq/delete" [] delete-schnaq!)
          (wrap-routes auth/is-admin-middleware)
          (wrap-routes auth/auth-middleware))
      (POST "/admin/discussions/make-read-only" [] make-discussion-read-only!)
      (POST "/admin/discussions/make-writeable" [] make-discussion-writeable!)
      (POST "/admin/schnaq/disable-pro-con" [] disable-pro-con!)
      (POST "/admin/statements/delete" [] delete-statements!)
      (POST "/author/add" [] add-author)
      (POST "/credentials/validate" [] check-credentials)
      (POST "/discussion/conclusions/starting" [] get-starting-conclusions)
      (POST "/discussion/react-to/statement" [] react-to-any-statement!)
      (-> (PUT "/discussion/statement/edit" [] edit-statement!)
          (wrap-routes auth/auth-middleware))
      (-> (PUT "/discussion/statement/delete" [] delete-statement!)
          (wrap-routes auth/auth-middleware))
      (POST "/discussion/statement/info" [] get-statement-info)
      (POST "/discussion/statements/for-conclusion" [] get-statements-for-conclusion)
      (POST "/discussion/statements/starting/add" [] add-starting-statement!)
      (POST "/emails/send-admin-center-link" [] send-admin-center-link)
      (POST "/emails/send-invites" [] send-invite-emails)
      (POST "/feedback/add" [] add-feedback)
      (POST "/graph/discussion" [] graph-data-for-agenda)
      (POST "/header-image/image" [] media/set-preview-image)
      (POST "/schnaq/add" [] add-schnaq)
      (POST "/schnaq/by-hash-as-admin" [] schnaq-by-hash-as-admin)
      (POST "/votes/down/toggle" [] toggle-downvote-statement)
      (POST "/votes/up/toggle" [] toggle-upvote-statement)
      analytics/analytics-routes
      hub/hub-routes
      user-api/user-routes)
    (wrap-routes auth/wrap-jwt-authentication)))

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
  (log/info "Welcome to schnaq's Backend 🧙")
  (log/info (format "Build Hash: %s" config/build-hash))
  (log/info (format "Environment: %s" config/env-mode))
  (log/info (format "Database Name: %s" config/db-name))
  (log/info (format "Database URI: %s" (subs config/datomic-uri 0 30)))
  (log/info (format "[Keycloak] Server: %s, Realm: %s" keycloak-config/server keycloak-config/realm)))

(def allowed-origin
  "Regular expression, which defines the allowed origins for API requests."
  #"^((https?:\/\/)?(.*\.)?(schnaq\.(com|de)))($|\/.*$)")

(defn -main
  "This is our main entry point for the REST API Server."
  [& _args]
  (let [port (:port config/api)
        allowed-origins [allowed-origin]
        allowed-origins' (if schnaq-core/production-mode? allowed-origins (conj allowed-origins #".*"))]
    ; Run the server with Ring.defaults middle-ware
    (say-hello)
    (schnaq-core/-main)
    (reset! current-server
            (server/run-server
              (-> #'app-routes
                  (wrap-cors :access-control-allow-origin allowed-origins'
                             :access-control-allow-methods [:get :put :post :delete])
                  (wrap-restful-format :formats [:transit-json :transit-msgpack :json-kw :edn :msgpack-kw :yaml-kw :yaml-in-html])
                  (wrap-defaults api-defaults))
              {:port port}))
    (log/info (format "Running web-server at http://127.0.0.1:%s/" port))
    (log/info (format "Allowed Origin: %s" allowed-origins'))))

(comment
  "Start the server from here"
  (-main)
  (stop-server)
  :end)
