(ns schnaq.api.discussion
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn-]]
            [ring.util.http-response :refer [ok created bad-request]]
            [schnaq.api.dto-specs :as dto]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :as db]
            [schnaq.database.reaction :as reaction-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.discussion :as discussion]
            [schnaq.media :as media]
            [schnaq.processors :as processors]
            [schnaq.toolbelt :as toolbelt]
            [schnaq.validator :as validator]
            [taoensso.timbre :as log]))

(defn- extract-user
  "Returns a user-id, either from nickname if anonymous user or from identity, if jwt token is present."
  [nickname identity]
  (let [nickname (user-db/user-by-nickname nickname)
        registered-user (:db/id (db/fast-pull [:user.registered/keycloak-id (:sub identity)]))]
    (or registered-user nickname)))

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
      at/not-found-hash-invalid)))

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
      at/not-found-hash-invalid)))

(defn- search-statements
  "Search through any valid discussion."
  [{:keys [parameters]}]
  (let [{:keys [share-hash search-string]} (:query parameters)]
    (if (validator/valid-discussion? share-hash)
      (ok {:matching-statements (-> (discussion-db/search-statements share-hash search-string)
                                    with-sub-discussion-info
                                    valid-statements-with-votes)})
      at/not-found-hash-invalid)))

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
      at/not-found-hash-invalid)))

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
  "Edits the content (and possibly type) of a statement, when the user is the registered author.
  `statement-type` is one of `statement.type/attack`, `statement.type/support` or `statement.type/neutral`."
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
      #(validator/deny-access at/invalid-rights-message))))

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
      #(validator/deny-access at/invalid-rights-message))))

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
      (validator/deny-access at/invalid-rights-message))))

(defn- react-to-any-statement!
  "Adds a support or attack regarding a certain statement. `conclusion-id` is the
  statement you want to react to. `statement-type` is one of `statement.type/attack`, `statement.type/support` or `statement.type/neutral`.
  `nickname` is required if the user is not logged in."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash conclusion-id nickname premise statement-type]} (:body parameters)
        keycloak-id (:sub identity)
        user-id (if keycloak-id
                  [:user.registered/keycloak-id keycloak-id]
                  (user-db/user-by-nickname nickname))]
    (if (validator/valid-writeable-discussion-and-statement? conclusion-id share-hash)
      (do (log/info "Statement added as reaction to statement" conclusion-id)
          (created ""
                   {:new-statement
                    (valid-statements-with-votes
                      (discussion-db/react-to-statement! share-hash user-id conclusion-id premise statement-type keycloak-id))}))
      (validator/deny-access at/invalid-rights-message))))

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
      at/not-found-hash-invalid)))


;; -----------------------------------------------------------------------------
;; Discussion Options

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

(def discussion-routes
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
                          404 at/response-error-body}}]
   ["/manage" {:parameters {:body {:share-hash :discussion/share-hash
                                   :edit-hash :discussion/edit-hash}}
               :responses {403 at/response-error-body}}
    ["" {:responses {200 {:body {:share-hash :discussion/share-hash}}}}
     ["/disable-pro-con" {:put disable-pro-con!
                          :description (at/get-doc #'disable-pro-con!)
                          :parameters {:body {:disable-pro-con? boolean?}}}]
     ["/make-read-only" {:put make-discussion-read-only!
                         :description (at/get-doc #'make-discussion-read-only!)}]
     ["/make-writeable" {:put make-discussion-writeable!
                         :description (at/get-doc #'make-discussion-writeable!)}]]]
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
                                               :nickname ::dto/maybe-nickname
                                               :premise :statement/content
                                               :statement-type :statement/type}}
                           :responses {201 {:body {:new-statement ::dto/statement}}
                                       403 at/response-error-body}}]
   ["/statements"
    ["/delete" {:delete delete-statements!
                :description (at/get-doc #'delete-statements!)
                :parameters {:body {:share-hash :discussion/share-hash
                                    :edit-hash :discussion/edit-hash
                                    :statement-ids (s/coll-of :db/id)}}
                :responses {200 {:body {:deleted-statements (s/coll-of :db/id)}}
                            401 at/response-error-body
                            403 at/response-error-body}}]
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
                                          :nickname ::dto/maybe-nickname}}
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
               :middleware [:user/authenticated?]
               :parameters {:body {:statement-type (s/or :nil nil?
                                                         :type :statement/type)
                                   :new-content :statement/content}}
               :responses {200 {:body {:updated-statement ::dto/statement}}
                           400 at/response-error-body
                           403 at/response-error-body}}]
     ["/delete" {:delete delete-statement!
                 :description (at/get-doc #'delete-statement!)
                 :middleware [:user/authenticated?]
                 :responses {200 {:body {:deleted-statement :db/id}}
                             400 at/response-error-body
                             403 at/response-error-body}}]
     ["/vote" {:parameters {:body {:nickname ::dto/maybe-nickname}}}
      ["/down" {:post toggle-downvote-statement
                :description (at/get-doc #'toggle-downvote-statement)
                :responses {200 {:body (s/keys :req-un [:statement.vote/operation])}
                            400 at/response-error-body}}]
      ["/up" {:post toggle-upvote-statement
              :description (at/get-doc #'toggle-upvote-statement)
              :responses {200 {:body (s/keys :req-un [:statement.vote/operation])}
                          400 at/response-error-body}}]]]]])