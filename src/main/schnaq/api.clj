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
            [reitit.spec :as rs]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.http-response :refer [ok created bad-request forbidden]]
            [schnaq.api.analytics :as analytics]
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
        (created "" {:new-discussion (links/add-links-to-discussion created-discussion)}))
      (let [error-msg (format "The input you provided could not be used to create a discussion:%n%s" discussion-data)]
        (log/info error-msg)
        (bad-request error-msg)))))

(defn- add-author
  "Generate a user based on the nickname. This is an *anonymous* user, and we
  can only refer to the user by the nickname. So this function is idempotent and
  returns always the same id when providing the same nickname."
  [{:keys [parameters]}]
  (let [author-name (get-in parameters [:body :nickname])
        user-id (user-db/add-user-if-not-exists author-name)]
    (created "" {:user-id user-id})))

(defn- discussion-by-hash
  "Returns a meeting, identified by its share-hash."
  [{:keys [parameters identity]}]
  (let [hash (get-in parameters [:path :share-hash])
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
  `{:parameters {:query {:share-hashes #uuid\"57ce1947-e57f-4395-903e-e2866d2f305c\"}}}`

  If multiple share-hashes are sent to the backend, reitit wraps them into a
  collection:
  `{:parameters {:query {:share-hashes [#uuid\"57ce1947-e57f-4395-903e-e2866d2f305c\"
                                        #uuid\"b2645217-6d7f-4d00-85c1-b8928fad43f7\"]}}}"
  [request]
  (if-let [hashes (get-in request [:parameters :query :share-hashes])]
    (let [hashes-list (if (string? hashes) [hashes] hashes)]
      (ok {:discussions
           (map processors/add-meta-info-to-schnaq
                (discussion-db/valid-discussions-by-hashes hashes-list))}))
    (bad-request {:error "Schnaqs could not be loaded."})))

(defn- public-schnaqs
  "Return all public schnaqs."
  [_req]
  (ok {:discussions (map processors/add-meta-info-to-schnaq (discussion-db/public-discussions))}))

(defn- schnaq-by-hash-as-admin
  "If user is authenticated, a meeting with an edit-hash is returned for further
  processing in the frontend."
  [{:keys [parameters]}]
  (let [{:keys [share-hash edit-hash]} (:body parameters)]
    (if (validator/valid-credentials? share-hash edit-hash)
      (ok {:discussion (discussion-db/discussion-by-share-hash-private share-hash)})
      (validator/deny-access "You provided the wrong hashes to access this schnaq."))))

(defn- make-discussion-read-only!
  "Makes a discussion read-only if discussion-admin credentials are there."
  [{:keys [parameters]}]
  (let [{:keys [share-hash edit-hash]} (:body parameters)]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/info "Setting discussion to read-only: " share-hash)
          (discussion-db/set-discussion-read-only share-hash)
          (ok {:share-hash share-hash}))
      (validator/deny-access "You do not have the rights to access this action."))))

(defn- make-discussion-writeable!
  "Makes a discussion writeable if discussion-admin credentials are there."
  [{:keys [parameters]}]
  (let [{:keys [share-hash edit-hash]} (:body parameters)]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/info "Removing read-only from discussion: " share-hash)
          (discussion-db/remove-read-only share-hash)
          (ok {:share-hash share-hash}))
      (validator/deny-access "You do not have the rights to access this action."))))

(defn- disable-pro-con!
  "Disable pro-con option for a schnaq."
  [{:keys [parameters]}]
  (let [{:keys [disable-pro-con? share-hash edit-hash]} (:body parameters)]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/info "Setting \"disable-pro-con option\" to" disable-pro-con? "for schnaq:" share-hash)
          (discussion-db/set-disable-pro-con share-hash disable-pro-con?)
          (ok {:share-hash share-hash}))
      (validator/deny-access "You do not have the rights to access this action."))))

(defn- delete-statements!
  "Deletes the passed list of statements if the admin-rights are fitting.
  Important: Needs to check whether the statement-id really belongs to the discussion with
  the passed edit-hash."
  [{:keys [parameters]}]
  (let [{:keys [share-hash edit-hash statement-ids]} (:body parameters)]
    (if (validator/valid-credentials? share-hash edit-hash)
      ;; could optimize with a collection query here
      (if (every? #(discussion-db/check-valid-statement-id-for-discussion % share-hash) statement-ids)
        (do (discussion-db/delete-statements! statement-ids)
            (ok {:deleted-statements statement-ids}))
        (bad-request {:error "You are trying to delete statements, without the appropriate rights"}))
      (validator/deny-access "You do not have the rights to access this action."))))

(defn- delete-schnaq!
  "Sets the state of a schnaq to delete. Should be only available to superusers (admins)."
  [{:keys [parameters]}]
  (let [{:keys [share-hash]} (:body parameters)]
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
  [{:keys [parameters identity]}]
  (toggle-vote-statement
    (:body parameters) identity reaction-db/upvote-statement! reaction-db/remove-upvote!
    reaction-db/did-user-upvote-statement reaction-db/did-user-downvote-statement))

(defn- toggle-downvote-statement
  "Upvote if no upvote has been made, otherwise remove upvote for statement."
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
  (ok (db/all-feedbacks)))

(defn- all-summaries
  "Returns all summaries form the db."
  [_]
  (ok {:summaries (discussion-db/all-summaries)}))

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
       (valid-statements-with-votes starting-statements))))
  ([share-hash secret-statement-id]
   (add-creation-secret (starting-conclusions-with-processors share-hash) secret-statement-id)))

(defn- with-sub-discussion-info
  [statements]
  (let [statement-ids (map :db/id statements)
        info-map (discussion-db/child-node-info statement-ids)]
    (map #(assoc % :meta/sub-discussion-info (get info-map (:db/id %))) statements)))

(defn- get-starting-conclusions
  "Return all starting-conclusions of a certain discussion if share-hash fits."
  [{:keys [parameters]}]
  (let [{:keys [share-hash]} (:query parameters)]
    (if (validator/valid-discussion? share-hash)
      (ok {:starting-conclusions (starting-conclusions-with-processors share-hash)})
      (validator/deny-access invalid-rights-message))))

(defn- get-statements-for-conclusion
  "Return all premises and fitting undercut-premises for a given statement."
  [{:keys [parameters]}]
  (let [{:keys [share-hash conclusion]} (:body parameters)
        prepared-statements (-> (:db/id conclusion)
                                discussion-db/children-for-statement
                                valid-statements-with-votes
                                with-sub-discussion-info)]
    (if (validator/valid-discussion? share-hash)
      (ok {:premises prepared-statements})
      (validator/deny-access invalid-rights-message))))

(defn- search-schnaq
  "Search through any valid schnaq."
  [{:keys [parameters]}]
  (let [{:keys [share-hash search-string]} (:query parameters)]
    (if (validator/valid-discussion? share-hash)
      (ok {:matching-statements (-> (discussion-db/search-schnaq share-hash search-string)
                                    with-sub-discussion-info
                                    valid-statements-with-votes)})
      (validator/deny-access))))

(defn- get-statement-info
  "Return the sought after conclusion (by id) and the following children."
  [{:keys [parameters]}]
  (let [{:keys [share-hash statement-id]} (:body parameters)]
    (if (validator/valid-discussion-and-statement? statement-id share-hash)
      (ok (valid-statements-with-votes
            {:conclusion (first (-> [(db/fast-pull statement-id discussion-db/statement-pattern)]
                                    with-sub-discussion-info
                                    (toolbelt/pull-key-up :db/ident)))
             :premises (with-sub-discussion-info (discussion-db/children-for-statement statement-id))}))
      (validator/deny-access invalid-rights-message))))

(defn- add-starting-statement!
  "Adds a new starting statement to a discussion. Returns the list of starting-conclusions."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash statement nickname]} (:body parameters)
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
          (ok (valid-statements-with-votes
                {:new-statement
                 (discussion-db/react-to-statement! share-hash user-id conclusion-id premise statement-type keycloak-id)})))
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
      (bad-request (at/build-error-body :invalid-share-hash "Invalid share-hash. You are not allowed to view this data.")))))

(comment
  (validator/valid-discussion? "0080b626-d713-4655-adca-3717aa052eea")
  (graph-data-for-agenda {:parameters {:body {:share-hash "f1507b02-c5e7-4191-9437-f4282d60f436"}}})

  :nil)

(defn- export-txt-data
  "Exports the discussion data as a string."
  [{:keys [parameters]}]
  (let [{:keys [share-hash]} (:query parameters)]
    (if (validator/valid-discussion? share-hash)
      (do (log/info "User is generating a txt export for discussion" share-hash)
          (ok {:string-representation (export/generate-text-export share-hash)}))
      (validator/deny-access invalid-rights-message))))

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
      #(bad-request {:error "You can not edit a closed / deleted discussion or statement."})
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
      #(bad-request {:error "You can not delete a closed / deleted discussion or statement."})
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
      (bad-request {:error "Something went wrong. Check your Email-Address and try again."}))))


;; -----------------------------------------------------------------------------
;; Routes

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

(s/def :feedback.api/feedback ::specs/feedback-dto)

(def app
  (ring/ring-handler
    (ring/router
      [["/ping" {:get ping}]
       ["/export/txt" {:get export-txt-data
                       :swagger {:tags ["exports"]}
                       :parameters {:query {:share-hash :discussion/share-hash}}}]
       ["/author/add" {:put add-author
                       :responses {201 {:body {:user-id :db/id}}}
                       :description (:doc (meta #'add-author))
                       :parameters {:body {:nickname :user/nickname}}}]
       ["/credentials/validate" {:post check-credentials!
                                 :description (:doc (meta #'check-credentials!))
                                 :responses {200 {:body {:valid-credentials? boolean?}}
                                             403 {:body {:valid-credentials? boolean?}}}
                                 :parameters {:body {:share-hash :discussion/share-hash
                                                     :edit-hash :discussion/edit-hash}}}]
       ["/feedback/add" {:post add-feedback
                         :description (:doc (meta #'add-feedback))
                         :responses {201 {:body {:feedback ::specs/feedback}}}
                         :parameters {:body (s/keys :req-un [:feedback.api/feedback] :opt-un [:feedback/screenshot])}}]
       ["/graph/discussion" {:get graph-data-for-agenda
                             :description (:doc (meta #'graph-data-for-agenda))
                             :parameters {:query {:share-hash :discussion/share-hash}}
                             :responses {200 {:body {:graph ::specs/graph}}
                                         400 {:body ::at/error-body}}}]
       ["/header-image/image" {:post media/set-preview-image
                               :parameters {:body {:share-hash :discussion/share-hash
                                                   :edit-hash :discussion/edit-hash
                                                   :image-url :discussion/header-image-url}}}]
       ["/lead-magnet/subscribe" {:post subscribe-lead-magnet!
                                  :parameters {:body {:email string?}}}]
       ["/votes" {:swagger {:tags ["votes"]}
                  :parameters {:body {:share-hash :discussion/share-hash
                                      :statement-id :db/id
                                      :nickname :user/nickname}}}
        ["/down/toggle" {:post toggle-downvote-statement}]
        ["/up/toggle" {:post toggle-upvote-statement}]]

       ["/discussion" {:swagger {:tags ["discussions"]}}
        ["/conclusions/starting" {:get get-starting-conclusions
                                  :parameters {:query {:share-hash string?}}}]
        ["/react-to/statement" {:post react-to-any-statement!
                                :parameters {:body {:share-hash :discussion/share-hash
                                                    :conclusion-id :db/id
                                                    :nickname :user/nickname
                                                    :premise :statement/content
                                                    :reaction :statement/unqualified-types}}}]
        ["/statements" {:parameters {:body {:share-hash :discussion/share-hash}}}
         ["/for-conclusion" {:post get-statements-for-conclusion
                             :parameters {:body {:conclusion (s/keys :req [:db/id])}}}]
         ["/starting/add" {:post add-starting-statement!
                           :parameters {:body {:statement :statement/content
                                               :nickname :user/nickname}}}]]
        ["/statement" {:parameters {:body {:share-hash :discussion/share-hash
                                           :statement-id :db/id}}}
         ["/info" {:post get-statement-info}]
         ["/edit" {:put edit-statement!
                   :middleware [auth/auth-middleware]
                   :parameters {:body {:statement-type :statement/unqualified-types
                                       :new-content :statement/content}}}]
         ["/delete" {:delete delete-statement!
                     :middleware [auth/auth-middleware]}]]]

       ["/emails" {:swagger {:tags ["emails"]}
                   :parameters {:body {:share-hash :discussion/share-hash
                                       :edit-hash :discussion/edit-hash}}}
        ["/send-admin-center-link" {:post send-admin-center-link
                                    :parameters {:body {:recipient string?
                                                        :admin-center string?}}}]
        ["/send-invites" {:post send-invite-emails
                          :parameters {:body {:recipients (s/coll-of string?)
                                              :share-link :discussion/share-link}}}]]

       ["/schnaq" {:swagger {:tags ["schnaqs"]}}
        ["/by-hash/:share-hash" {:get discussion-by-hash
                                 :parameters {:path {:share-hash :discussion/share-hash}}}]
        ["/search" {:get search-schnaq
                    :parameters {:query {:share-hash :discussion/share-hash
                                         :search-string string?}}}]
        ["/add" {:post add-schnaq
                 :parameters {:body {:discussion-title :discussion/title
                                     :public-discussion? boolean?}}}
         [""]
         ["/anonymous" {:parameters {:body {:nickname :user/nickname}}}]
         ["/with-hub" {:parameters {:body {:hub-exclusive? boolean?
                                           :hub :hub/keycloak-name}}}]]
        ["/by-hash-as-admin" {:post schnaq-by-hash-as-admin
                              :parameters {:body {:share-hash :discussion/share-hash
                                                  :edit-hash :discussion/edit-hash}}}]]

       ["/schnaqs" {:swagger {:tags ["schnaqs"]}}
        ["/by-hashes" {:get schnaqs-by-hashes
                       :description "Takes one or more share-hashes as query parameters."
                       :parameters {:query {:share-hashes (s/or :hashes (s/coll-of :discussion/share-hash)
                                                                :hash :discussion/share-hash)}}}]
        ["/public" {:get public-schnaqs}]]

       ["/admin" {:swagger {:tags ["admin"]}
                  :middleware [auth/auth-middleware auth/is-admin-middleware]}
        ["/feedbacks" {:get all-feedbacks}]
        ["/summaries/all" {:get all-summaries}]
        ["/schnaq/delete" {:delete delete-schnaq!
                           :parameters {:body {:share-hash :discussion/share-hash}}}]
        ["/statements/delete" {:post delete-statements!
                               :parameters {:body {:share-hash :discussion/share-hash
                                                   :edit-hash :discussion/edit-hash
                                                   :statement-ids :db/id}}}]]
       ["/manage" {:swagger {:tags ["manage"]}}
        ["/schnaq" {:parameters {:body {:share-hash :discussion/share-hash
                                        :edit-hash :discussion/edit-hash}}}
         ["/disable-pro-con" {:post disable-pro-con!
                              :parameters {:body {:disable-pro-con? boolean?}}}]
         ["/schnaq/make-read-only" {:post make-discussion-read-only!}]
         ["/schnaq/make-writeable" {:post make-discussion-writeable!}]]
        ["/statements/delete" {:post delete-statement!
                               :parameters {:body {:statement-id :db/id
                                                   :share-hash :discussion/share-hash}}}]]

       user-api/user-routes
       hub/hub-routes
       analytics/analytics-routes
       summaries/summary-routes

       ["/swagger.json"
        {:get {:no-doc true
               :swagger {:info {:title "schnaq API"}}
               :handler (swagger/create-swagger-handler)}}]]
      {:exception pretty/exception
       ::rs/explain expound/expound-str
       :data {:coercion reitit.coercion.spec/coercion
              :muuntaja m/instance
              :middleware [swagger/swagger-feature
                           parameters/parameters-middleware ;; query-params & form-params
                           muuntaja/format-negotiate-middleware ;; content-negotiation
                           muuntaja/format-response-middleware ;; encoding response body
                           exception/exception-middleware   ;; exception handling
                           muuntaja/format-request-middleware ;; decoding request body
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
                             :access-control-allow-methods [:get :put :post :delete])
                  #_(wrap-restful-format :formats [:transit-json :transit-msgpack :json-kw :edn :msgpack-kw :yaml-kw :yaml-in-html])
                  #_(wrap-defaults api-defaults))
              {:port shared-config/api-port}))
    (log/info (format "Running web-server at %s" shared-config/api-url))
    (log/info (format "Allowed Origin: %s" allowed-origins'))))

(comment
  "Start the server from here"
  (-main)
  (stop-server)
  :end)
