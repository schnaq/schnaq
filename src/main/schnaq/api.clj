(ns schnaq.api
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [expound.alpha :as expound]
            [ghostwheel.core :refer [>defn-]]
            [muuntaja.core :as m]
            [org.httpkit.client :as http-client]
            [org.httpkit.server :as server]
            [reitit.coercion.spec]
            [reitit.dev.pretty :as pretty]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.spec :as rrs]
            [reitit.spec :as rs]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.http-response :refer [ok created bad-request forbidden not-found]]
            [schnaq.api.analytics :as analytics]
            [schnaq.api.dto-specs :as dto]
            [schnaq.api.hub :as hub]
            [schnaq.api.summaries :as summaries]
            [schnaq.api.toolbelt :as at]
            [schnaq.api.user :as user-api]
            [schnaq.auth :as auth]
            [schnaq.config :as config]
            [schnaq.config.keycloak :as keycloak-config]
            [schnaq.config.mailchimp :as mailchimp-config]
            [schnaq.config.shared :as shared-config]
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
            [schnaq.links :as links]
            [schnaq.media :as media]
            [schnaq.processors :as processors]
            [schnaq.s3 :as s3]
            [schnaq.toolbelt :as toolbelt]
            [schnaq.translations :refer [email-templates]]
            [schnaq.validator :as validator]
            [spec-tools.core :as st]
            [taoensso.timbre :as log])
  (:import (java.util Base64 UUID))
  (:gen-class))

(s/def :http/status nat-int?)
(s/def :http/headers map?)
(s/def :ring/response (s/keys :req-un [:http/status :http/headers]))
(s/def :ring/body-params map?)
(s/def :ring/route-params map?)
(s/def :ring/request (s/keys :opt [:ring/body-params :ring/route-params]))

(def ^:private invalid-rights-message "You to not have enough permissions to access this data.")
(def ^:private invalid-share-hash "Invalid share-hash.")
(def ^:private not-found-with-error-message
  (not-found
    (at/build-error-body :invalid-share-hash invalid-share-hash)))

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

(defn- add-schnaq
  "Adds a discussion to the database. Returns the newly-created discussion."
  [{:keys [parameters identity]}]
  (let [{:keys [nickname discussion-title public-discussion? hub-exclusive? hub]} (:body parameters)
        keycloak-id (:sub identity)
        authorized-for-hub? (some #(= % hub) (:groups identity))
        author (if keycloak-id
                 [:user.registered/keycloak-id keycloak-id]
                 (user-db/add-user-if-not-exists nickname))
        discussion-data (cond-> {:discussion/title discussion-title
                                 :discussion/share-hash (.toString (UUID/randomUUID))
                                 :discussion/edit-hash (.toString (UUID/randomUUID))
                                 :discussion/author author}
                                (and hub-exclusive? authorized-for-hub?)
                                (assoc :discussion/hub-origin [:hub/keycloak-name hub]))
        new-discussion-id (discussion-db/new-discussion discussion-data public-discussion?)]
    (if new-discussion-id
      (let [created-discussion (discussion-db/private-discussion-data new-discussion-id)]
        (when (and hub-exclusive? hub authorized-for-hub?)
          (hub-db/add-discussions-to-hub [:hub/keycloak-name hub] [new-discussion-id]))
        (log/info "Discussion created: " new-discussion-id " - "
                  (:discussion/title created-discussion) " – Public? " public-discussion?
                  "Exclusive?" hub-exclusive? "for" hub)
        (created "" {:new-schnaq (links/add-links-to-discussion created-discussion)}))
      (let [error-msg (format "The input you provided could not be used to create a discussion:%n%s" discussion-data)]
        (log/info error-msg)
        (bad-request (at/build-error-body :schnaq-creation-failed error-msg))))))

(defn- add-author
  "Generate a user based on the nickname. This is an *anonymous* user, and we
  can only refer to the user by the nickname. So this function is idempotent and
  returns always the same id when providing the same nickname."
  [{:keys [parameters]}]
  (let [author-name (get-in parameters [:body :nickname])
        user-id (user-db/add-user-if-not-exists author-name)]
    (created "" {:user-id user-id})))

(defn- schnaq-by-hash
  "Returns a discussion, identified by its share-hash."
  [{:keys [parameters identity]}]
  (let [hash (get-in parameters [:query :share-hash])
        keycloak-id (:sub identity)]
    (if (validator/valid-discussion? hash)
      (ok {:schnaq (processors/add-meta-info-to-schnaq
                     (if (and keycloak-id (validator/user-schnaq-admin? hash keycloak-id))
                       (discussion-db/discussion-by-share-hash-private hash)
                       (discussion-db/discussion-by-share-hash hash)))})
      (validator/deny-access))))

(defn- schnaqs-by-hashes
  "Bulk loading of discussions. May be used when users asks for all the schnaqs
  they have access to. If only one schnaq shall be loaded, ring packs it
  into a single string:
  `{:parameters {:query {:share-hashes \"57ce1947-e57f-4395-903e-e2866d2f305c\"}}}`

  If multiple share-hashes are sent to the backend, reitit wraps them into a
  collection:
  `{:parameters {:query {:share-hashes [\"57ce1947-e57f-4395-903e-e2866d2f305c\"
                                        \"b2645217-6d7f-4d00-85c1-b8928fad43f7\"]}}}"
  [request]
  (if-let [share-hashes (get-in request [:parameters :query :share-hashes])]
    (let [share-hashes-list (if (string? share-hashes) [share-hashes] share-hashes)]
      (ok {:schnaqs
           (map processors/add-meta-info-to-schnaq
                (discussion-db/valid-discussions-by-hashes share-hashes-list))}))
    not-found-with-error-message))

(defn- public-schnaqs
  "Return all public schnaqs."
  [_req]
  (ok {:schnaqs (map processors/add-meta-info-to-schnaq (discussion-db/public-discussions))}))

(defn- schnaq-by-hash-as-admin
  "If user is authenticated, a meeting with an edit-hash is returned for further
  processing in the frontend."
  [{:keys [parameters]}]
  (let [{:keys [share-hash edit-hash]} (:body parameters)]
    (if (validator/valid-credentials? share-hash edit-hash)
      (ok {:schnaq (discussion-db/discussion-by-share-hash-private share-hash)})
      (validator/deny-access "You provided the wrong hashes to access this schnaq."))))

(defn- make-discussion-read-only!
  "Makes a discussion read-only if share- and edit-hash are correct and present."
  [{:keys [parameters]}]
  (let [{:keys [share-hash edit-hash]} (:body parameters)]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/info "Setting discussion to read-only: " share-hash)
          (discussion-db/set-discussion-read-only share-hash)
          (ok {:share-hash share-hash}))
      (validator/deny-access))))

(defn- make-discussion-writeable!
  "Makes a discussion writeable if discussion-admin credentials are there."
  [{:keys [parameters]}]
  (let [{:keys [share-hash edit-hash]} (:body parameters)]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/info "Removing read-only from discussion: " share-hash)
          (discussion-db/remove-read-only share-hash)
          (ok {:share-hash share-hash}))
      (validator/deny-access))))

(defn- disable-pro-con!
  "Disable pro-con option for a schnaq."
  [{:keys [parameters]}]
  (let [{:keys [disable-pro-con? share-hash edit-hash]} (:body parameters)]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/info "Setting \"disable-pro-con option\" to" disable-pro-con? "for schnaq:" share-hash)
          (discussion-db/set-disable-pro-con share-hash disable-pro-con?)
          (ok {:share-hash share-hash}))
      (validator/deny-access))))

(defn- delete-statements!
  "Deletes the passed list of statements if the admin-rights are fitting.
  Important: Needs to check whether the statement-id really belongs to the discussion with
  the passed edit-hash."
  [{:keys [parameters]}]
  (let [{:keys [share-hash edit-hash statement-ids]} (:body parameters)
        deny-access (validator/deny-access "You do not have the rights to access this action.")]
    (if (validator/valid-credentials? share-hash edit-hash)
      ;; could optimize with a collection query here
      (if (every? #(discussion-db/check-valid-statement-id-for-discussion % share-hash) statement-ids)
        (do (discussion-db/delete-statements! statement-ids)
            (ok {:deleted-statements statement-ids}))
        deny-access)
      deny-access)))

(defn- delete-schnaq!
  "Sets the state of a schnaq to delete. Should be only available to superusers (admins)."
  [{:keys [parameters]}]
  (let [{:keys [share-hash]} (:body parameters)]
    (if (discussion-db/delete-discussion share-hash)
      (ok {:share-hash share-hash})
      (bad-request (at/build-error-body :error-deleting-schnaq "An error occurred, while deleting the schnaq.")))))

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
    (bad-request (at/build-error-body :vote-not-registered
                                      "Vote could not be registered"))))

(defn- toggle-upvote-statement
  "Upvote if no upvote has been made, otherwise remove upvote for statement.
  `nickname` is optional and used for anonymous votes. If no `nickname` is
  provided, request must contain a valid authentication token."
  [{:keys [parameters identity]}]
  (toggle-vote-statement
    (:body parameters) identity reaction-db/upvote-statement! reaction-db/remove-upvote!
    reaction-db/did-user-upvote-statement reaction-db/did-user-downvote-statement))

(defn- toggle-downvote-statement
  "Upvote if no upvote has been made, otherwise remove upvote for statement.
  `nickname` is optional and used for anonymous votes. If no `nickname` is
  provided, request must contain a valid authentication token."
  [{:keys [parameters identity]}]
  (toggle-vote-statement
    (:body parameters) identity reaction-db/downvote-statement! reaction-db/remove-downvote!
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
  "Add new feedback from schnaq's frontend. If a screenshot is provided, it will
  be uploaded in our s3 bucket. Screenshot must be a base64 encoded string. The
  screenshot-field is optional."
  [{:keys [parameters]}]
  (let [feedback (get-in parameters [:body :feedback])
        feedback-id (db/add-feedback! feedback)
        screenshot (get-in parameters [:body :screenshot])]
    (when screenshot
      (upload-screenshot! screenshot feedback-id))
    (log/info "Feedback created")
    (created "" {:feedback feedback})))

(defn- all-feedbacks
  "Returns all feedbacks from the db."
  [_]
  (ok {:feedbacks (db/all-feedbacks)}))

(>defn- send-invite-emails
  "Expects a list of recipients and the meeting which shall be send."
  [{:keys [parameters]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash edit-hash recipients share-link]} (:body parameters)
        discussion-title (:discussion/title (discussion-db/discussion-by-share-hash share-hash))]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/debug "Invite Emails for some meeting sent")
          (ok (merge
                {:message "Emails sent successfully"}
                (emails/send-mails
                  (format (email-templates :invitation/title) discussion-title)
                  (format (email-templates :invitation/body) discussion-title share-link)
                  recipients))))
      (validator/deny-access))))

(>defn- send-admin-center-link
  "Send URL to admin-center via mail to recipient."
  [{:keys [parameters]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash edit-hash recipient admin-center]} (:body parameters)
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
  [statements]
  (-> statements
      processors/hide-deleted-statement-content
      processors/with-votes))

(defn- with-sub-discussion-info
  "Add sub-discussion-info, if necessary. Sub-Discussion-infos are number of
  sub-statements, authors, ..."
  [statements]
  (let [statement-ids (map :db/id statements)
        info-map (discussion-db/child-node-info statement-ids)]
    (map (fn [statement]
           (if-let [sub-discussions (get info-map (:db/id statement))]
             (assoc statement :meta/sub-discussion-info sub-discussions)
             statement))
         statements)))

(defn- starting-conclusions-with-processors
  "Returns starting conclusions for a discussion, with processors applied.
  Optionally a statement-id can be passed to enrich the statement with its creation-secret."
  ([share-hash]
   (-> share-hash
       discussion-db/starting-statements
       valid-statements-with-votes
       with-sub-discussion-info))
  ([share-hash secret-statement-id]
   (add-creation-secret (starting-conclusions-with-processors share-hash) secret-statement-id)))

(defn- get-starting-conclusions
  "Return all starting-conclusions of a certain discussion if share-hash fits."
  [{:keys [parameters]}]
  (let [{:keys [share-hash]} (:query parameters)]
    (if (validator/valid-discussion? share-hash)
      (ok {:starting-conclusions (starting-conclusions-with-processors share-hash)})
      not-found-with-error-message)))

(defn- get-statements-for-conclusion
  "Return all premises and fitting undercut-premises for a given statement."
  [{:keys [parameters]}]
  (let [{:keys [share-hash conclusion-id]} (:query parameters)
        prepared-statements (-> conclusion-id
                                discussion-db/children-for-statement
                                valid-statements-with-votes
                                with-sub-discussion-info)]
    (if (validator/valid-discussion? share-hash)
      (ok {:premises prepared-statements})
      not-found-with-error-message)))

(defn- search-statements
  "Search through any valid discussion."
  [{:keys [parameters]}]
  (let [{:keys [share-hash search-string]} (:query parameters)]
    (if (validator/valid-discussion? share-hash)
      (ok {:matching-statements (-> (discussion-db/search-statements share-hash search-string)
                                    with-sub-discussion-info
                                    valid-statements-with-votes)})
      not-found-with-error-message)))

(defn- get-statement-info
  "Return premises and conclusion for a given statement id."
  [{:keys [parameters]}]
  (let [{:keys [share-hash statement-id]} (:query parameters)]
    (if (validator/valid-discussion-and-statement? statement-id share-hash)
      (ok (valid-statements-with-votes
            {:conclusion (first (-> [(db/fast-pull statement-id discussion-db/statement-pattern)]
                                    with-sub-discussion-info
                                    (toolbelt/pull-key-up :db/ident)))
             :premises (with-sub-discussion-info (discussion-db/children-for-statement statement-id))}))
      not-found-with-error-message)))

(defn- add-starting-statement!
  "Adds a new starting statement to a discussion. Returns the list of starting-conclusions."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash statement nickname]} (:body parameters)
        keycloak-id (:sub identity)
        user-id (if keycloak-id
                  [:user.registered/keycloak-id keycloak-id]
                  (user-db/add-user-if-not-exists nickname))]
    (if (validator/valid-writeable-discussion? share-hash)
      (let [new-starting-id (discussion-db/add-starting-statement! share-hash user-id statement keycloak-id)]
        (log/info "Starting statement added for discussion" share-hash)
        (created "" {:starting-conclusions (starting-conclusions-with-processors share-hash new-starting-id)}))
      (validator/deny-access invalid-rights-message))))

(defn- react-to-any-statement!
  "Adds a support or attack regarding a certain statement. `conclusion-id` is the
  statement you want to react to. `reaction` is one of `attack`, `support` or `neutral`.
  `nickname` is required if the user is not logged in."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash conclusion-id nickname premise reaction]} (:body parameters)
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
          (created ""
                   {:new-statement
                    (valid-statements-with-votes
                      (discussion-db/react-to-statement! share-hash user-id conclusion-id premise statement-type keycloak-id))}))
      (validator/deny-access invalid-rights-message))))

(defn- check-credentials!
  "Checks whether share-hash and edit-hash match.
  If the user is logged in and the credentials are valid, they are added as an admin."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash edit-hash]} (:body parameters)
        valid-credentials? (validator/valid-credentials? share-hash edit-hash)
        keycloak-id (:sub identity)]
    (when (and valid-credentials? keycloak-id)
      (discussion-db/add-admin-to-discussion share-hash keycloak-id))
    (if valid-credentials?
      (ok {:valid-credentials? valid-credentials?})
      (forbidden {:valid-credentials? valid-credentials?}))))

(defn- graph-data-for-agenda
  "Delivers the graph-data needed to draw the graph in the frontend."
  [{:keys [parameters]}]
  (let [share-hash (get-in parameters [:query :share-hash])]
    (if (validator/valid-discussion? share-hash)
      (let [statements (discussion-db/all-statements-for-graph share-hash)
            starting-statements (discussion-db/starting-statements share-hash)
            edges (discussion/links-for-starting starting-statements share-hash)
            controversy-vals (discussion/calculate-controversy edges)]
        (ok {:graph {:nodes (discussion/nodes-for-agenda statements share-hash)
                     :edges edges
                     :controversy-values controversy-vals}}))
      (bad-request (at/build-error-body :invalid-share-hash invalid-share-hash)))))

(defn- export-txt-data
  "Exports the discussion data as a string."
  [{:keys [parameters]}]
  (let [{:keys [share-hash]} (:query parameters)]
    (if (validator/valid-discussion? share-hash)
      (do (log/info "User is generating a txt export for discussion" share-hash)
          (ok {:string-representation (export/generate-text-export share-hash)}))
      (bad-request (at/build-error-body :invalid-share-hash invalid-share-hash)))))

(defn- check-statement-author-and-state
  "Checks if a statement is authored by this user-identity and is valid, i.e. not deleted.
  If the statement is valid and authored by this user, `success-fn` is called. if the statement is not valid, `bad-request-fn`
  is called. And if the authored user does not match `deny-access-fn` will be called."
  [user-identity statement-id share-hash statement success-fn bad-request-fn deny-access-fn]
  (if (= user-identity (-> statement :statement/author :user.registered/keycloak-id))
    (if (and (validator/valid-writeable-discussion-and-statement? statement-id share-hash)
             (not (:statement/deleted? statement)))
      (success-fn)
      (bad-request-fn))
    (deny-access-fn)))

(defn- edit-statement!
  "Edits the content (and possibly type) of a statement, when the user is the registered author."
  [{:keys [parameters identity]}]
  (let [{:keys [statement-id statement-type new-content share-hash]} (:body parameters)
        user-identity (:sub identity)
        statement (db/fast-pull statement-id [:db/id :statement/parent
                                              {:statement/author [:user.registered/keycloak-id]}
                                              :statement/deleted?])]
    (check-statement-author-and-state
      user-identity statement-id share-hash statement
      #(ok {:updated-statement (discussion-db/change-statement-text-and-type statement statement-type new-content)})
      #(bad-request (at/build-error-body :discussion-closed-or-deleted "You can not edit a closed / deleted discussion or statement."))
      #(validator/deny-access invalid-rights-message))))

(defn- delete-statement!
  "Deletes a statement, when the user is the registered author."
  [{:keys [parameters identity]}]
  (let [{:keys [statement-id share-hash]} (:body parameters)
        user-identity (:sub identity)
        statement (db/fast-pull statement-id [:db/id :statement/parent
                                              {:statement/author [:user.registered/keycloak-id]}
                                              :statement/deleted?])]
    (check-statement-author-and-state
      user-identity statement-id share-hash statement
      #(do (discussion-db/delete-statements! [statement-id])
           (ok {:deleted-statement statement-id}))
      #(bad-request (at/build-error-body :discussion-closed-or-deleted "You can not delete a closed / deleted discussion or statement."))
      #(validator/deny-access invalid-rights-message))))

(defn- subscribe-lead-magnet!
  "Subscribes to the mailing list and sends the lead magnet to the email-address."
  [{:keys [parameters]}]
  (let [email (get-in parameters [:body :email])
        options {:timeout 10000
                 :basic-auth ["user" mailchimp-config/api-key]
                 :body (json/write-str {:email_address email
                                        :status "subscribed"
                                        :email_type "html"
                                        :tags ["lead-magnet" "datenschutz"]})
                 :user-agent "schnaq Backend Application"}]
    (http-client/post mailchimp-config/subscribe-uri options)
    (if (emails/send-remote-work-lead-magnet email)
      (ok {:status :ok})
      (bad-request (at/build-error-body :failed-subscription "Something went wrong. Check your Email-Address and try again.")))))


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
  (log/info (format "Environment: %s" shared-config/environment))
  (log/info (format "Database Name: %s" config/db-name))
  (log/info (format "Database URI (truncated): %s" (subs config/datomic-uri 0 30)))
  (log/info (format "[Keycloak] Server: %s, Realm: %s" keycloak-config/server keycloak-config/realm)))

(def allowed-origin
  "Regular expression, which defines the allowed origins for API requests."
  #"^((https?:\/\/)?(.*\.)?(schnaq\.(com|de)))($|\/.*$)")

(def app
  (ring/ring-handler
    (ring/router
      [["/ping" {:get ping
                 :description (at/get-doc #'ping)
                 :responses {200 {:body {:ok string?}}}}]
       ["/export/txt" {:get export-txt-data
                       :description (at/get-doc #'export-txt-data)
                       :swagger {:tags ["exports"]}
                       :parameters {:query {:share-hash :discussion/share-hash}}
                       :responses {200 {:body {:string-representation string?}}
                                   400 at/response-error-body}}]
       ["/author/add" {:put add-author
                       :description (at/get-doc #'add-author)
                       :parameters {:body {:nickname :user/nickname}}
                       :responses {201 {:body {:user-id :db/id}}}}]
       ["/credentials/validate" {:post check-credentials!
                                 :description (at/get-doc #'check-credentials!)
                                 :responses {200 {:body {:valid-credentials? boolean?}}
                                             403 {:body {:valid-credentials? boolean?}}}
                                 :parameters {:body {:share-hash :discussion/share-hash
                                                     :edit-hash :discussion/edit-hash}}}]
       ["/feedback/add" {:post add-feedback
                         :description (at/get-doc #'add-feedback)
                         :parameters {:body (s/keys :req-un [::dto/feedback] :opt-un [:feedback/screenshot])}
                         :responses {201 {:body {:feedback ::dto/feedback}}}}]
       ["/lead-magnet/subscribe" {:post subscribe-lead-magnet!
                                  :description (at/get-doc #'subscribe-lead-magnet!)
                                  :parameters {:body {:email string?}}
                                  :responses {200 {:body {:status keyword?}}
                                              400 at/response-error-body}}]

       ["/discussion" {:swagger {:tags ["discussions"]}}
        ["/conclusions/starting" {:get get-starting-conclusions
                                  :description (at/get-doc #'get-starting-conclusions)
                                  :parameters {:query {:share-hash :discussion/share-hash}}
                                  :responses {200 {:body {:starting-conclusions (s/coll-of ::dto/statement)}}
                                              404 at/response-error-body}}]
        ["/graph" {:get graph-data-for-agenda
                   :description (at/get-doc #'graph-data-for-agenda)
                   :parameters {:query {:share-hash :discussion/share-hash}}
                   :responses {200 {:body {:graph ::specs/graph}}
                               400 at/response-error-body}}]
        ["/header-image" {:post media/set-preview-image
                          :description (at/get-doc #'media/set-preview-image)
                          :parameters {:body {:share-hash :discussion/share-hash
                                              :edit-hash :discussion/edit-hash
                                              :image-url :discussion/header-image-url}}
                          :responses {201 {:body {:message string?}}
                                      403 at/response-error-body}}]
        ["/react-to/statement" {:post react-to-any-statement!
                                :description (at/get-doc #'react-to-any-statement!)
                                :parameters {:body {:share-hash :discussion/share-hash
                                                    :conclusion-id :db/id
                                                    :nickname :user/nickname
                                                    :premise :statement/content
                                                    :reaction keyword? #_:statement/unqualified-types}}
                                :responses {201 {:body {:new-statement ::dto/statement}}
                                            403 at/response-error-body}}]
        ["/statements"
         ["/search" {:get search-statements
                     :description (at/get-doc #'search-statements)
                     :parameters {:query {:share-hash :discussion/share-hash
                                          :search-string string?}}
                     :responses {200 {:body {:matching-statements (s/coll-of ::dto/statement)}}
                                 404 at/response-error-body}}]
         ["/for-conclusion" {:get get-statements-for-conclusion
                             :description (at/get-doc #'get-statements-for-conclusion)
                             :parameters {:query {:share-hash :discussion/share-hash
                                                  :conclusion-id :db/id}}
                             :responses {200 {:body {:premises (s/coll-of ::dto/statement)}}
                                         404 at/response-error-body}}]
         ["/starting/add" {:post add-starting-statement!
                           :description (at/get-doc #'add-starting-statement!)
                           :parameters {:body {:share-hash :discussion/share-hash
                                               :statement :statement/content
                                               :nickname :user/nickname}}
                           :responses {201 {:body {:starting-conclusions (s/coll-of ::dto/statement)}}
                                       403 at/response-error-body}}]]
        ["/statement"
         ["/info" {:get get-statement-info
                   :description (at/get-doc #'get-statement-info)
                   :parameters {:query {:statement-id :db/id
                                        :share-hash :discussion/share-hash}}
                   :responses {200 {:body {:conclusion ::dto/statement
                                           :premises (s/coll-of ::dto/statement)}}
                               404 at/response-error-body}}]
         ["" {:parameters {:body {:statement-id :db/id
                                  :share-hash :discussion/share-hash}}}
          ["/edit" {:put edit-statement!
                    :description (at/get-doc #'edit-statement!)
                    :middleware [auth/authenticated?-middleware]
                    :parameters {:body {:statement-type :statement/unqualified-types
                                        :new-content :statement/content}}
                    :responses {200 {:body {:updated-statement ::dto/statement}}
                                400 at/response-error-body
                                403 at/response-error-body}}]
          ["/delete" {:delete delete-statement!
                      :description (at/get-doc #'delete-statement!)
                      :middleware [auth/authenticated?-middleware]
                      :responses {200 {:body {:deleted-statement :db/id}}
                                  400 at/response-error-body
                                  403 at/response-error-body}}]
          ["/vote" {:parameters {:body {:nickname :user/nickname}}}
           ["/down" {:post toggle-downvote-statement
                     :description (at/get-doc #'toggle-downvote-statement)
                     :responses {200 {:body (s/keys :req-un [:statement.vote/operation])}
                                 400 at/response-error-body}}]
           ["/up" {:post toggle-upvote-statement
                   :description (at/get-doc #'toggle-upvote-statement)
                   :responses {200 {:body (s/keys :req-un [:statement.vote/operation])}
                               400 at/response-error-body}}]]]]]

       ["/emails" {:swagger {:tags ["emails"]}
                   :parameters {:body {:share-hash :discussion/share-hash
                                       :edit-hash :discussion/edit-hash}}}
        ["/send-admin-center-link" {:post send-admin-center-link
                                    :description (at/get-doc #'send-admin-center-link)
                                    :parameters {:body {:recipient string?
                                                        :admin-center string?}}
                                    :responses {200 {:body {:message string?
                                                            :failed-sendings (s/coll-of string?)}}
                                                403 at/response-error-body}}]
        ["/send-invites" {:post send-invite-emails
                          :description (at/get-doc #'send-invite-emails)
                          :parameters {:body {:recipients (s/coll-of string?)
                                              :share-link :discussion/share-link}}
                          :responses {200 {:body {:message string?
                                                  :failed-sendings (s/coll-of string?)}}
                                      403 at/response-error-body}}]]

       ["/schnaq" {:swagger {:tags ["schnaqs"]}}
        ["/by-hash" {:get schnaq-by-hash
                     :description (at/get-doc #'schnaq-by-hash)
                     :parameters {:query {:share-hash :discussion/share-hash}}
                     :responses {200 {:body {:schnaq ::specs/discussion}}
                                 403 at/response-error-body}}]
        ["/add" {:description (at/get-doc #'add-schnaq)
                 :parameters {:body {:discussion-title :discussion/title
                                     :public-discussion? boolean?}}
                 :responses {201 {:body {:new-schnaq ::dto/discussion}}
                             400 at/response-error-body}}
         ["" {:post add-schnaq}]
         ["/anonymous" {:post add-schnaq
                        :parameters {:body {:nickname :user/nickname}}}]
         ["/with-hub" {:post add-schnaq
                       :parameters {:body {:hub-exclusive? boolean?
                                           :hub :hub/keycloak-name}}}]]
        ["/by-hash-as-admin" {:post schnaq-by-hash-as-admin
                              :parameters {:body {:share-hash :discussion/share-hash
                                                  :edit-hash :discussion/edit-hash}}
                              :responses {200 {:body {:schnaq ::dto/discussion}}}}]]

       ["/schnaqs" {:swagger {:tags ["schnaqs"]}}
        ["/by-hashes" {:get schnaqs-by-hashes
                       :description (at/get-doc #'schnaqs-by-hashes)
                       :parameters {:query {:share-hashes (s/or :share-hashes (st/spec {:spec (s/coll-of :discussion/share-hash)
                                                                                        :swagger/collectionFormat "multi"})
                                                                :share-hash :discussion/share-hash)}}
                       :responses {200 {:body {:schnaqs (s/coll-of ::dto/discussion)}}
                                   404 at/response-error-body}}]
        ["/public" {:get public-schnaqs
                    :description (at/get-doc #'public-schnaqs)
                    :responses {200 {:body {:schnaqs (s/coll-of ::dto/discussion)}}}}]]

       ["/admin" {:swagger {:tags ["admin"]}
                  :responses {401 at/response-error-body}
                  :middleware [auth/authenticated?-middleware auth/admin?-middleware]}
        ["/feedbacks" {:get all-feedbacks
                       :description (at/get-doc #'all-feedbacks)
                       :responses {200 {:body {:feedbacks (s/coll-of ::specs/feedback)}}}}]
        ["/schnaq/delete" {:delete delete-schnaq!
                           :description (at/get-doc #'delete-schnaq!)
                           :parameters {:body {:share-hash :discussion/share-hash}}
                           :responses {200 {:share-hash :discussion/share-hash}
                                       400 at/response-error-body}}]]
       ["/manage" {:swagger {:tags ["manage"]}
                   :parameters {:body {:share-hash :discussion/share-hash
                                       :edit-hash :discussion/edit-hash}}
                   :responses {403 at/response-error-body}}
        ["/schnaq" {:responses {200 {:body {:share-hash :discussion/share-hash}}}}
         ["/disable-pro-con" {:put disable-pro-con!
                              :description (at/get-doc #'disable-pro-con!)
                              :parameters {:body {:disable-pro-con? boolean?}}}]
         ["/make-read-only" {:put make-discussion-read-only!
                             :description (at/get-doc #'make-discussion-read-only!)}]
         ["/make-writeable" {:put make-discussion-writeable!
                             :description (at/get-doc #'make-discussion-writeable!)}]]
        ["/statements/delete" {:delete delete-statements!
                               :description (at/get-doc #'delete-statements!)
                               :parameters {:body {:statement-ids (s/coll-of :db/id)}}
                               :responses {200 {:body {:deleted-statements (s/coll-of :db/id)}}}}]]

       user-api/user-routes
       hub/hub-routes
       analytics/analytics-routes
       summaries/summary-routes

       ["/swagger.json"
        {:get {:no-doc true
               :swagger {:info {:title "schnaq API"}}
               :handler (swagger/create-swagger-handler)}}]]
      {:exception pretty/exception
       :validate rrs/validate
       ::rs/explain expound/expound-str
       :data {:coercion reitit.coercion.spec/coercion
              :muuntaja m/instance
              :middleware [swagger/swagger-feature
                           parameters/parameters-middleware ;; query-params & form-params
                           muuntaja/format-middleware
                           exception/exception-middleware   ;; exception handling
                           coercion/coerce-response-middleware ;; coercing response bodys
                           coercion/coerce-request-middleware ;; coercing request parameters
                           multipart/multipart-middleware
                           auth/wrap-jwt-authentication]}})
    (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path "/"
         :config {:validatorUrl nil
                  :operationsSorter "alpha"}})
      (ring/create-default-handler))))

(defn -main
  "This is our main entry point for the REST API Server."
  [& _args]
  (let [allowed-origins [allowed-origin]
        allowed-origins' (if shared-config/production? allowed-origins (conj allowed-origins #".*"))]
    ; Run the server with Ring.defaults middle-ware
    (say-hello)
    (schnaq-core/-main)
    (reset! current-server
            (server/run-server
              (-> #'app
                  (wrap-cors :access-control-allow-origin allowed-origins'
                             :access-control-allow-methods [:get :put :post :delete]))
              {:port shared-config/api-port}))
    (log/info (format "Running web-server at %s" shared-config/api-url))
    (log/info (format "Allowed Origin: %s" allowed-origins'))))

(comment
  "Start the server from here"
  (-main)
  (stop-server)
  :end)
