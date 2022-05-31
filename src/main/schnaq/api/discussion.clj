(ns schnaq.api.discussion
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [=> >defn-]]
            [ring.util.http-response :refer [bad-request created forbidden ok]]
            [schnaq.api.dto-specs :as dto]
            [schnaq.api.toolbelt :as at]
            [schnaq.auth.lib :as auth-lib]
            [schnaq.config :as config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :as db :refer [set-activation-focus]]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.reaction :as reaction-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.discussion :as discussion]
            [schnaq.mail.emails :as emails]
            [schnaq.media :as media]
            [schnaq.processors :as processors]
            [schnaq.validator :as validator]
            [taoensso.timbre :as log]))

(defn get-starting-conclusions
  "Return all starting-conclusions of a certain discussion if share-hash fits."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash display-name]} (:query parameters)
        user-identity (:sub identity)
        author-id (user-db/user-id display-name user-identity)
        startings (discussion-db/starting-statements share-hash)]
    (ok (processors/statement-default
         {:starting-conclusions startings
          :children (discussion-db/children-from-statements startings)}
         share-hash user-identity author-id))))

(defn- search-statements
  "Search through any valid discussion."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash search-string display-name]} (:query parameters)
        keycloak-id (:sub identity)
        user-id (user-db/user-id display-name keycloak-id)]
    (ok {:matching-statements (processors/statement-default
                               (discussion-db/search-statements share-hash search-string)
                               share-hash keycloak-id user-id)})))

(defn- get-statement-info
  "Return premises, conclusion and the history for a given statement id."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash statement-id display-name]} (:query parameters)
        user-identity (:sub identity)
        author-id (user-db/user-id display-name user-identity)]
    (if (validator/valid-discussion-and-statement? statement-id share-hash)
      (let [conclusion (db/fast-pull statement-id patterns/statement)
            premises (discussion-db/children-for-statement statement-id)]
        (ok (processors/statement-default
             {:conclusion conclusion
              :premises premises
              :history (discussion-db/history-for-statement statement-id)
              :children (discussion-db/children-from-statements (conj premises conclusion))}
             share-hash user-identity author-id)))
      at/not-found-hash-invalid)))

(defn- update-seen-statements!
  "Adds a seen flag to the statements data and update"
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash seen-statement-ids]} (:body parameters)
        user-identity (:sub identity)
        statement-ids seen-statement-ids]
    (user-db/create-visited-statements-for-discussion
     user-identity share-hash statement-ids)
    (ok {:share-hash share-hash
         :seen-statement-ids seen-statement-ids})))

(defn- mark-all-statements-as-seen
  "Mark all statements for a discussion as seen. Can be used when accessing a
   schnaq to de-mark all new statements."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash]} (:body parameters)
        keycloak-id (:sub identity)]
    (discussion-db/mark-all-statements-of-discussion-as-read keycloak-id share-hash)
    (ok {:share-hash share-hash})))

(defn- check-statement-author-and-state
  "Checks if a statement is authored by this user-identity and is valid, i.e. not deleted.
  If the statement is valid and authored by this user, `success-fn` is called. if the statement is not valid, `bad-request-fn`
  is called. And if the authored user does not match `deny-access-fn` will be called."
  [user-identity statement-id share-hash statement success-fn bad-request-fn deny-access-fn]
  (let [user-is-author? (= (:sub user-identity) (-> statement :statement/author :user.registered/keycloak-id))]
    (if (or user-is-author? (:admin? user-identity))
      (if (and (discussion-db/check-valid-statement-id-for-discussion statement-id share-hash)
               (not (:statement/deleted? statement)))
        (success-fn)
        (bad-request-fn))
      (deny-access-fn))))

(defn- edit-statement!
  "Edits the content (and possibly type) of a statement, when the user is the registered author.
  `statement-type` is one of `statement.type/attack`, `statement.type/support` or `statement.type/neutral`."
  [{:keys [parameters identity]}]
  (let [{:keys [statement-id statement-type new-content share-hash display-name]} (:body parameters)
        statement (db/fast-pull statement-id [:db/id :statement/parent
                                              {:statement/author [:user.registered/keycloak-id]}
                                              :statement/deleted?])
        author-id (user-db/user-id display-name (:sub identity))]
    (check-statement-author-and-state
     identity statement-id share-hash statement
     #(ok {:updated-statement (-> [(discussion-db/change-statement-text-and-type statement statement-type new-content)]
                                  (processors/with-aggregated-votes author-id)
                                  (processors/with-sub-statement-count share-hash)
                                  (processors/with-new-post-info share-hash (:sub identity))
                                  first)})
     #(bad-request (at/build-error-body :discussion-closed-or-deleted "You can not edit a closed / deleted discussion or statement."))
     #(validator/deny-access at/invalid-rights-message))))

(defn- flag-statement
  "Sends a mail to the schnaq author and info@schnaq.com with a link to the flagged post."
  [{:keys [parameters]}]
  (let [{:keys [share-hash statement-id]} (:body parameters)
        discussion (discussion-db/discussion-by-share-hash share-hash)
        statement (db/fast-pull statement-id patterns/statement)
        author-keycloak-id (-> discussion :discussion/author :user.registered/keycloak-id)
        author (user-db/private-user-by-keycloak-id author-keycloak-id)
        recipients ["info@schnaq.com" (:user.registered/email author)]]
    (log/info "Flagged Statement:" statement-id "in discussion:" share-hash)
    (emails/send-flagged-post discussion statement recipients)
    (ok {:flagged-statement statement-id})))

(defn- delete-statement!
  "Deletes a statement, when the user is the registered author."
  [{:keys [parameters identity]}]
  (let [{:keys [statement-id share-hash]} (:body parameters)
        statement (db/fast-pull statement-id [:db/id :statement/parent
                                              {:statement/author [:user.registered/keycloak-id]}
                                              :statement/deleted?])]
    (check-statement-author-and-state
     identity statement-id share-hash statement
     #(ok {:deleted-statement statement-id
           :method (discussion-db/delete-statement! statement-id)})
     #(bad-request (at/build-error-body :discussion-closed-or-deleted "You can not delete a closed / deleted discussion or statement."))
     #(validator/deny-access at/invalid-rights-message))))

(defn- delete-statements!
  "Deletes the passed list of statements if the admin-rights are fitting.
  Important: Needs to check whether the statement-id really belongs to the discussion with
  the passed edit-hash."
  [{:keys [parameters]}]
  (let [{:keys [share-hash statement-ids]} (:body parameters)
        deny-access (validator/deny-access "You do not have the rights to access this action.")]
    ;; could optimize with a collection query here
    (if (every? #(discussion-db/check-valid-statement-id-for-discussion % share-hash) statement-ids)
      (ok {:deleted-statements statement-ids
           :methods (discussion-db/delete-statements! statement-ids)})
      deny-access)))

(defn- add-starting-statement!
  "Adds a new starting statement to a discussion. Returns the list of starting-conclusions."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash edit-hash statement display-name locked?]} (:body parameters)
        keycloak-id (:sub identity)
        user-id (user-db/user-id display-name keycloak-id)
        ;; Only Moderators can lock
        locked? (if (validator/valid-credentials? share-hash edit-hash) locked? false)]
    (log/info "Starting statement added for discussion" share-hash)
    (created "" {:starting-conclusion (discussion-db/add-starting-statement! share-hash user-id statement
                                                                             :registered-user? keycloak-id
                                                                             :locked? locked?)})))

(defn- react-to-any-statement!
  "Adds a support or attack regarding a certain statement. `conclusion-id` is the
  statement you want to react to. `statement-type` is one of `statement.type/attack`, `statement.type/support` or `statement.type/neutral`.
  `nickname` is required if the user is not logged in."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash edit-hash conclusion-id premise statement-type locked? display-name]} (:body parameters)
        keycloak-id (:sub identity)
        user-id (user-db/user-id display-name keycloak-id)
        ;; Only Moderators can lock
        locked? (if (validator/valid-credentials? share-hash edit-hash) locked? false)]
    (if (discussion-db/check-valid-statement-id-for-discussion conclusion-id share-hash)
      (do (log/info "Statement added as reaction to statement" conclusion-id)
          (created
           ""
           {:new-statement (discussion-db/react-to-statement! share-hash user-id conclusion-id premise statement-type
                                                              :registered-user? keycloak-id
                                                              :locked? locked?)}))
      (validator/deny-access at/invalid-rights-message))))

(defn graph-for-discussion
  "Delivers the graph-data needed to draw the graph in the frontend."
  [{:keys [parameters]}]
  (let [share-hash (get-in parameters [:query :share-hash])
        statements (discussion-db/all-statements-for-graph share-hash)
        starting-statements (discussion-db/starting-statements share-hash)
        edges (discussion/links-for-starting starting-statements share-hash)
        controversy-vals (discussion/calculate-controversy edges)]
    (ok {:graph {:nodes (discussion/nodes-for-agenda statements share-hash)
                 :edges edges
                 :controversy-values controversy-vals}})))

;; -----------------------------------------------------------------------------
;; Discussion Options

(defn- make-discussion-read-only!
  "Makes a discussion read-only if share- and edit-hash are correct and present."
  [{:keys [parameters]}]
  (let [{:keys [share-hash]} (:body parameters)]
    (log/info "Setting discussion to read-only:" share-hash)
    (discussion-db/set-discussion-read-only share-hash)
    (ok {:share-hash share-hash})))

(defn- make-discussion-writeable!
  "Makes a discussion writeable if discussion-admin credentials are there."
  [{:keys [parameters]}]
  (let [{:keys [share-hash]} (:body parameters)]
    (log/info "Removing read-only from discussion:" share-hash)
    (discussion-db/remove-read-only share-hash)
    (ok {:share-hash share-hash})))

(defn- disable-pro-con!
  "Disable pro-con option for a schnaq."
  [{:keys [parameters]}]
  (let [{:keys [disable-pro-con? share-hash]} (:body parameters)]
    (log/info "Setting \"disable-pro-con option\" to" disable-pro-con? "for schnaq:" share-hash)
    (discussion-db/set-disable-pro-con share-hash disable-pro-con?)
    (ok {:share-hash share-hash})))

(defn- mods-mark-only!
  "Only allow moderators to mark correct answers."
  [{:keys [parameters]}]
  (let [{:keys [mods-mark-only? share-hash]} (:body parameters)]
    (log/info "Setting \"mods-mark-only option\" to" mods-mark-only? "for schnaq:" share-hash)
    (discussion-db/mods-mark-only! share-hash mods-mark-only?)
    (ok {:share-hash share-hash})))

(defn- set-focus
  "Set the current focus of the discussion to a single entity."
  [{{{:keys [entity-id share-hash]} :body} :parameters}]
  (set-activation-focus [:discussion/share-hash share-hash] entity-id)
  (ok {:share-hash share-hash}))

;; -----------------------------------------------------------------------------
;; Votes

(defn- toggle-vote-statement
  "Toggle up- or downvote of statement."
  [{:keys [share-hash statement-id]} registered-user
   add-vote-fn remove-vote-fn check-vote-fn counter-check-vote-fn]
  (if (validator/valid-discussion-and-statement? statement-id share-hash)
    (let [vote (check-vote-fn statement-id registered-user)
          counter-vote (counter-check-vote-fn statement-id registered-user)]
      (log/trace "Triggered Vote on Statement by registered user" registered-user)
      (if vote
        (do (remove-vote-fn statement-id registered-user)
            (ok {:operation :removed}))
        (do (add-vote-fn statement-id registered-user)
            (if counter-vote
              (ok {:operation :switched})
              (ok {:operation :added})))))
    (bad-request (at/build-error-body :vote-not-registered "Vote could not be registered"))))

(defn- toggle-anon-vote-statement
  "Toggle up- or downvote of anon statement."
  [{:keys [share-hash statement-id inc-or-dec]} vote-type]
  (if (and (not (nil? inc-or-dec))
           (validator/valid-discussion-and-statement? statement-id share-hash))
    (let [vote-function
          (cond
            (and (= inc-or-dec :inc) (= vote-type :upvote)) reaction-db/upvote-anonymous-statement!
            (and (= inc-or-dec :dec) (= vote-type :upvote)) reaction-db/remove-anonymous-upvote!
            (and (= inc-or-dec :inc) (= vote-type :downvote)) reaction-db/downvote-anonymous-statement!
            (and (= inc-or-dec :dec) (= vote-type :downvote)) reaction-db/remove-anonymous-downvote!)]
      (log/trace "Triggered Anonymous vote on Statement " statement-id)
      (vote-function statement-id)
      (ok {:operation :succeeded}))
    (bad-request (at/build-error-body :vote-not-registered "Anonymous vote could not be registered"))))

(defn- toggle-upvote-statement
  "Upvote if no upvote has been made, otherwise remove upvote for statement.
  `nickname` is optional and used for anonymous votes. If no `nickname` is
  provided, request must contain a valid authentication token."
  [{:keys [parameters identity]}]
  (if-let [registered-user (:db/id (db/fast-pull [:user.registered/keycloak-id (:sub identity)]))]
    (toggle-vote-statement
     (:body parameters) registered-user reaction-db/upvote-statement! reaction-db/remove-upvote!
     reaction-db/did-user-upvote-statement reaction-db/did-user-downvote-statement)
    (toggle-anon-vote-statement (:body parameters) :upvote)))

(defn- toggle-downvote-statement
  "Upvote if no upvote has been made, otherwise remove upvote for statement.
  `nickname` is optional and used for anonymous votes. If no `nickname` is
  provided, request must contain a valid authentication token."
  [{:keys [parameters identity]}]
  (if-let [registered-user (:db/id (db/fast-pull [:user.registered/keycloak-id (:sub identity)]))]
    (toggle-vote-statement
     (:body parameters) registered-user reaction-db/downvote-statement! reaction-db/remove-downvote!
     reaction-db/did-user-downvote-statement reaction-db/did-user-upvote-statement)
    (toggle-anon-vote-statement (:body parameters) :downvote)))

(defn- user-allowed-to-label?
  "Helper function checking, whether the user is allowed to use labels in the discussion."
  [identity share-hash]
  (let [pro-user? (auth-lib/pro-user? identity)
        mods-only? (-> (discussion-db/discussion-by-share-hash share-hash)
                       :discussion/states
                       set
                       (contains? :discussion.state.qa/mark-as-moderators-only))]
    (or (not mods-only?)
        (and mods-only? pro-user?))))

(defn- add-label
  "Add a label to a statement. Only pre-approved labels can be set. Custom labels have no effect.
  The user needs to be authenticated. The statement concerned is always returned."
  [{:keys [parameters identity]}]
  (let [{:keys [statement-id label share-hash display-name]} (:body parameters)
        keycloak-id (:sub identity)
        user-id (user-db/user-id display-name keycloak-id)]
    (if (user-allowed-to-label? identity share-hash)
      (ok {:statement (processors/statement-default (discussion-db/add-label statement-id label)
                                                    share-hash keycloak-id user-id)})
      (forbidden (at/build-error-body :permission/forbidden "You are not allowed to edit labels")))))

(defn- remove-label
  "Remove a label from a statement. Removing a label not present has no effect.
  The user needs to be authenticated. The statement concerned is always returned."
  [{:keys [parameters identity]}]
  (let [{:keys [statement-id label share-hash display-name]} (:body parameters)
        keycloak-id (:sub identity)
        user-id (user-db/user-id display-name keycloak-id)]
    (if (user-allowed-to-label? identity share-hash)
      (ok {:statement (processors/statement-default (discussion-db/remove-label statement-id label)
                                                    share-hash keycloak-id user-id)})
      (forbidden (at/build-error-body :permission/forbidden "You are not allowed to edit labels")))))

(defn- toggle-statement-lock
  "Lock or unlock a statement. (Makes it read-only)"
  [{:keys [parameters]}]
  (let [{:keys [statement-id share-hash lock?]} (:body parameters)]
    (if (validator/valid-discussion-and-statement? statement-id share-hash)
      (if (discussion-db/toggle-statement-lock statement-id lock?)
        (ok {:locked? lock?})
        (bad-request (at/build-error-body :statement.lock/error "Something went wrong while locking, try again.")))
      (bad-request (at/build-error-body :statement/invalid "The statement you are trying to lock is not valid.")))))

(defn- toggle-pinned-statement
  "Pin or unpin a statement."
  [{:keys [parameters]}]
  (let [{:keys [statement-id share-hash pin?]} (:body parameters)]
    (if (validator/valid-discussion-and-statement? statement-id share-hash)
      (if (discussion-db/toggle-pinned-statement statement-id pin?)
        (ok {:pinned? pin?})
        (bad-request (at/build-error-body :statement.lock/error "Something went wrong while pinning, try again.")))
      (bad-request (at/build-error-body :statement/invalid "The statement you are trying to pin is not valid.")))))

;; -----------------------------------------------------------------------------

(>defn- image-file-name
  "Create a file name to store assets for a schnaq."
  [file share-hash]
  [::specs/file :discussion/share-hash => string?]
  (format "%s/files/%s/image.%s" share-hash (str (random-uuid)) (media/image-type->file-ending (:type file))))

(>defn- common-file-name
  "Create a file name to store assets for a schnaq."
  [file share-hash]
  [::specs/file :discussion/share-hash => string?]
  (format "%s/files/%s/%s" share-hash (str (random-uuid)) (:name file)))

(defn- upload-file
  "Upload an image to a given bucket."
  [{{{:keys [file bucket share-hash]} :body} :parameters}]
  (let [{:keys [url error message]}
        (if (media/image? file)
          (media/upload-image! file (image-file-name file share-hash) config/image-width-in-statement bucket)
          (media/upload-file! file (common-file-name file share-hash) bucket))]
    (if url
      (created "" {:url url})
      (bad-request {:error error
                    :message message}))))

;; -----------------------------------------------------------------------------

(def discussion-routes
  ["/discussion" {:swagger {:tags ["discussions"]}}
   ["/conclusions/starting" {:get get-starting-conclusions
                             :description (at/get-doc #'get-starting-conclusions)
                             :name :api.discussion.conclusions/starting
                             :middleware [:discussion/valid-share-hash?]
                             :parameters {:query {:share-hash :discussion/share-hash
                                                  :display-name ::specs/non-blank-string}}
                             :responses {200 {:body {:starting-conclusions (s/coll-of ::dto/statement)}}}}]
   ["/graph" {:get graph-for-discussion
              :description (at/get-doc #'graph-for-discussion)
              :name :api.discussion/graph
              :middleware [:discussion/valid-share-hash?]
              :parameters {:query {:share-hash :discussion/share-hash}}
              :responses {200 {:body {:graph ::specs/graph}}}}]
   ["/manage" {:parameters {:body {:share-hash :discussion/share-hash
                                   :edit-hash :discussion/edit-hash}}
               :responses {403 at/response-error-body}}
    ["" {:responses {200 {:body {:share-hash :discussion/share-hash}}}
         :middleware [:discussion/valid-credentials?]}
     ["/disable-pro-con" {:put disable-pro-con!
                          :description (at/get-doc #'disable-pro-con!)
                          :middleware [:user/pro-user?]
                          :name :api.discussion.manage/disable-pro-con
                          :parameters {:body {:disable-pro-con? boolean?}}}]
     ["/mods-mark-only" {:put mods-mark-only!
                         :description (at/get-doc #'mods-mark-only!)
                         :name :api.discussion.manage/mods-mark-only
                         :middleware [:user/pro-user?]
                         :parameters {:body {:mods-mark-only? boolean?}}}]
     ["/make-read-only" {:put make-discussion-read-only!
                         :description (at/get-doc #'make-discussion-read-only!)
                         :middleware [:user/pro-user?]
                         :name :api.discussion.manage/make-read-only}]
     ["/make-writeable" {:put make-discussion-writeable!
                         :description (at/get-doc #'make-discussion-writeable!)
                         :middleware [:user/pro-user?]
                         :name :api.discussion.manage/make-writeable}]
     ["/focus" {:put set-focus
                :description (at/get-doc #'set-focus)
                :name :api.discussion.manage/focus
                :parameters {:body {:entity-id :db/id}}}]]]
   ["/header-image" {:post media/set-preview-image
                     :description (at/get-doc #'media/set-preview-image)
                     :name :api.discussion/header-image
                     :middleware [:discussion/valid-credentials?
                                  :user/pro-user?]
                     :parameters {:body {:share-hash :discussion/share-hash
                                         :edit-hash :discussion/edit-hash
                                         :image-url :discussion/header-image-url}}
                     :responses {201 {:body {:message string?}}
                                 403 at/response-error-body}}]
   ["/upload/file" {:put upload-file
                    :name :api.discussion.upload/file
                    :description (at/get-doc #'upload-file)
                    :middleware [:discussion/valid-share-hash?]
                    :parameters {:body {:file ::specs/file
                                        :bucket keyword?
                                        :share-hash :discussion/share-hash}}
                    :responses {201 {:body ::specs/file-stored}
                                400 at/response-error-body}}]
   ["/react-to/statement" {:post react-to-any-statement!
                           :description (at/get-doc #'react-to-any-statement!)
                           :name :api.discussion.react-to/statement
                           :middleware [:discussion/parent-unlocked?
                                        :discussion/valid-writeable-discussion?]
                           :parameters {:body {:share-hash :discussion/share-hash
                                               :conclusion-id :db/id
                                               :premise :statement/content
                                               :edit-hash (s/or :edit-hash :discussion/edit-hash
                                                                :nil nil?)
                                               :statement-type dto/statement-type
                                               :locked? :statement/locked?
                                               :display-name ::specs/non-blank-string}}
                           :responses {201 {:body {:new-statement ::dto/statement}}
                                       403 at/response-error-body}}]
   ["/statements"
    ["/delete" {:delete delete-statements!
                :description (at/get-doc #'delete-statements!)
                :name :api.discussion.statements/delete
                :middleware [:discussion/valid-credentials?]
                :parameters {:body {:share-hash :discussion/share-hash
                                    :edit-hash :discussion/edit-hash
                                    :statement-ids (s/coll-of :db/id)}}
                :responses {200 {:body {:deleted-statements (s/coll-of :db/id)
                                        :methods (s/coll-of keyword?)}}
                            401 at/response-error-body
                            403 at/response-error-body}}]
    ["/search" {:get search-statements
                :description (at/get-doc #'search-statements)
                :name :api.discussion.statements/search
                :middleware [:discussion/valid-share-hash?]
                :parameters {:query {:share-hash :discussion/share-hash
                                     :search-string string?
                                     :display-name ::specs/non-blank-string}}
                :responses {200 {:body {:matching-statements (s/coll-of ::dto/statement)}}
                            404 at/response-error-body}}]
    ["/starting/add" {:post add-starting-statement!
                      :description (at/get-doc #'add-starting-statement!)
                      :name :api.discussion.statements.starting/add
                      :middleware [:discussion/valid-writeable-discussion?]
                      :parameters {:body {:share-hash :discussion/share-hash
                                          :edit-hash (s/or :edit-hash :discussion/edit-hash
                                                           :nil nil?)
                                          :statement :statement/content
                                          :display-name ::specs/non-blank-string
                                          :locked? :statement/locked?}}
                      :responses {201 {:body {:starting-conclusion ::dto/statement}}
                                  403 at/response-error-body}}]
    ["/update-seen" {:put update-seen-statements!
                     :description (at/get-doc #'update-seen-statements!)
                     :name :api.discussion.statements/update-seen
                     :middleware [:user/authenticated?]
                     :parameters {:body {:share-hash :discussion/share-hash
                                         :seen-statement-ids (s/coll-of :db/id)}}
                     :responses {200 {:body {:share-hash :discussion/share-hash
                                             :seen-statement-ids (s/coll-of :db/id)}}
                                 400 at/response-error-body}}]
    ["/mark-all-as-seen" {:put mark-all-statements-as-seen
                          :description (at/get-doc #'mark-all-statements-as-seen)
                          :name :api.discussion.statements/all-seen
                          :middleware [:user/authenticated?]
                          :parameters {:body {:share-hash :discussion/share-hash}}
                          :responses {200 {:body {:share-hash :discussion/share-hash}}}}]]
   ["/statement"
    ["/info" {:get get-statement-info
              :description (at/get-doc #'get-statement-info)
              :name :api.discussion.statement/info
              :parameters {:query {:statement-id :db/id
                                   :share-hash :discussion/share-hash
                                   :display-name ::specs/non-blank-string}}
              :responses {200 {:body {:conclusion ::dto/statement
                                      :premises (s/coll-of ::dto/statement)
                                      :history (s/coll-of ::dto/statement)}}
                          404 at/response-error-body}}]
    ["" {:parameters {:body {:statement-id :db/id
                             :share-hash :discussion/share-hash}}}
     ["/lock/toggle" {:post toggle-statement-lock
                      :description (at/get-doc #'toggle-statement-lock)
                      :name :api.discussion.statements/lock
                      :middleware [:user/authenticated? :discussion/valid-credentials?]
                      :parameters {:body {:edit-hash :discussion/edit-hash
                                          :lock? boolean?}}
                      :responses {200 {:body {:locked? boolean?}}}}]
     ["/pin/toggle" {:post toggle-pinned-statement
                     :description (at/get-doc #'toggle-pinned-statement)
                     :name :api.discussion.statements/pin
                     :middleware [:user/authenticated? :user/pro-user? :discussion/valid-credentials?]
                     :parameters {:body {:edit-hash :discussion/edit-hash
                                         :pin? boolean?}}
                     :responses {200 {:body {:pinned? boolean?}}}}]
     ["/edit" {:put edit-statement!
               :description (at/get-doc #'edit-statement!)
               :name :api.discussion.statement/edit
               :middleware [:user/authenticated?
                            :discussion/valid-writeable-discussion?]
               :parameters {:body {:statement-type (s/or :nil nil?
                                                         :type dto/statement-type)
                                   :new-content :statement/content
                                   :display-name ::specs/non-blank-string}}
               :responses {200 {:body {:updated-statement ::dto/statement}}
                           400 at/response-error-body
                           403 at/response-error-body}}]
     ["/flag" {:post flag-statement
               :description (at/get-doc #'flag-statement)
               :name :api.discussion.statements/flag
               :parameters {:body {:share-hash :discussion/share-hash
                                   :statement-id :db/id}}
               :responses {200 {:body {:flagged-statement :db/id}}}}]
     ["/delete" {:delete delete-statement!
                 :description (at/get-doc #'delete-statement!)
                 :name :api.discussion.statement/delete
                 :middleware [:user/authenticated?
                              :discussion/valid-writeable-discussion?]
                 :responses {200 {:body {:deleted-statement :db/id
                                         :method keyword?}}
                             400 at/response-error-body
                             403 at/response-error-body}}]
     ["/vote" {:middleware [:discussion/valid-writeable-discussion?]
               :parameters {:body {:inc-or-dec ::dto/maybe-inc-or-dec}}}
      ["/down" {:post toggle-downvote-statement
                :description (at/get-doc #'toggle-downvote-statement)
                :name :api.discussion.statement.vote/down
                :responses {200 {:body (s/keys :req-un [:statement.vote/operation])}
                            400 at/response-error-body}}]
      ["/up" {:post toggle-upvote-statement
              :description (at/get-doc #'toggle-upvote-statement)
              :name :api.discussion.statement.vote/up
              :responses {200 {:body (s/keys :req-un [:statement.vote/operation])}
                          400 at/response-error-body}}]]
     ["/label" {:middleware [:discussion/valid-statement?
                             :discussion/valid-writeable-discussion?]}
      ["/add" {:put add-label
               :description (at/get-doc #'add-label)
               :name :api.discussion.statement.label/add
               :parameters {:body {:label :statement/label
                                   :display-name ::specs/non-blank-string}}
               :responses {200 {:body {:statement ::dto/statement}}
                           400 at/response-error-body
                           403 at/response-error-body}}]
      ["/remove" {:put remove-label
                  :description (at/get-doc #'remove-label)
                  :name :api.discussion.statement.label/remove
                  :parameters {:body {:label :statement/label
                                      :display-name ::specs/non-blank-string}}
                  :responses {200 {:body {:statement ::dto/statement}}
                              400 at/response-error-body
                              403 at/response-error-body}}]]]]])
