(ns schnaq.api.schnaq
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok created bad-request forbidden]]
            [schnaq.api.dto-specs :as dto]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.access-codes :as ac]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.hub :as hub-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.links :as links]
            [schnaq.processors :as processors]
            [schnaq.validator :as validator]
            [spec-tools.core :as st]
            [taoensso.timbre :as log])
  (:import (java.util UUID)))

(defn- schnaq-by-access-code
  "Validate access code and redirect request, if the code was valid."
  [{:keys [parameters]}]
  (let [{:keys [access-code]} (:query parameters)]
    (if-let [share-hash (get-in (ac/discussion-by-access-code access-code) [:discussion.access/discussion :discussion/share-hash])]
      (ok {:location (links/get-share-link share-hash)})
      at/access-code-invalid)))

(defn- schnaq-by-hash
  "Returns a schnaq, identified by its share-hash."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash display-name]} (:query parameters)
        keycloak-id (:sub identity)
        user-id (user-db/user-id display-name keycloak-id)]
    (ok {:schnaq (-> (if (and keycloak-id (validator/user-schnaq-admin? share-hash keycloak-id))
                       (discussion-db/discussion-by-share-hash-private share-hash)
                       (discussion-db/discussion-by-share-hash share-hash))
                     processors/schnaq-default
                     (processors/with-sub-statement-count share-hash)
                     (processors/with-new-post-info share-hash keycloak-id)
                     processors/hide-deleted-statement-content
                     (processors/with-aggregated-votes user-id))})))

(defn- schnaqs-by-hashes
  "Bulk loading of discussions. May be used when users ask for all the schnaqs
  they have access to. If only one schnaq shall be loaded, ring packs it
  into a single string:
  `{:parameters {:query {:share-hashes \"57ce1947-e57f-4395-903e-e2866d2f305c\"}}}`

  If multiple share-hashes are sent to the backend, reitit wraps them into a
  collection:
  `{:parameters {:body {:share-hashes [\"57ce1947-e57f-4395-903e-e2866d2f305c\"
                                       \"b2645217-6d7f-4d00-85c1-b8928fad43f7\"]}}}`"
  [{:keys [parameters identity]}]
  (let [{:keys [share-hashes display-name]} (:body parameters)
        share-hashes-list (if (string? share-hashes) [share-hashes] share-hashes)
        user-id (user-db/user-id display-name (:sub identity))]
    (if share-hashes
      (ok {:schnaqs
           (map #(-> %
                     processors/schnaq-default
                     (processors/with-aggregated-votes user-id))
                (discussion-db/discussions-by-share-hashes share-hashes-list))})
      at/not-found-hash-invalid)))

;; -----------------------------------------------------------------------------

(defn- bad-request-schnaq-creation
  "Return bad request when something went wrong and show the parameters."
  [parameters]
  (let [error-msg (format "The input you provided could not be used to create a discussion: %s" parameters)]
    (log/info error-msg)
    (bad-request (at/build-error-body :schnaq-creation-failed error-msg))))

(defn- add-schnaq
  "Adds a discussion to the database. Returns the newly-created discussion. Required fields are `discussion-title` and
   (`nickname` or an authenticated user)."
  [{:keys [parameters identity]}]
  (let [{:keys [nickname discussion-title hub-exclusive? hub] :as parameters} (:body parameters)
        keycloak-id (:sub identity)]
    (if-not (or keycloak-id nickname)
      (bad-request-schnaq-creation parameters)
      (let [author (if keycloak-id
                     [:user.registered/keycloak-id keycloak-id]
                     (user-db/add-user-if-not-exists nickname))
            authorized-for-hub? (some #(= % hub) (:groups identity))
            discussion-data (cond-> {:discussion/title discussion-title
                                     :discussion/share-hash (.toString (UUID/randomUUID))
                                     :discussion/edit-hash (.toString (UUID/randomUUID))
                                     :discussion/author author
                                     :discussion/mode :discussion.mode/qanda}
                              keycloak-id (assoc :discussion/admins [author])
                              (and hub-exclusive? authorized-for-hub?)
                              (assoc :discussion/hub-origin [:hub/keycloak-name hub]))
            new-discussion-id (discussion-db/new-discussion discussion-data)]
        (if new-discussion-id
          (do
            (when (and hub-exclusive? hub authorized-for-hub?)
              (hub-db/add-discussions-to-hub! [:hub/keycloak-name hub] [new-discussion-id]))
            (ac/add-access-code-to-discussion! new-discussion-id)
            (log/info "Discussion created: " discussion-data)
            (created "" {:new-schnaq (links/add-links-to-discussion (discussion-db/secret-discussion-data new-discussion-id))}))
          (bad-request-schnaq-creation parameters))))))

;; -----------------------------------------------------------------------------

(defn- schnaq-by-hash-as-admin
  "If user is authenticated, a meeting with an edit-hash is returned for further
  processing in the frontend."
  [{:keys [parameters]}]
  (let [{:keys [share-hash]} (:body parameters)]
    (ok {:schnaq (discussion-db/discussion-by-share-hash-private share-hash)})))

(defn- delete-schnaq!
  "Sets the state of a schnaq to delete. Should be only available to superusers (admins)."
  [{:keys [parameters]}]
  (let [{:keys [share-hash]} (:body parameters)]
    (if (discussion-db/delete-discussion share-hash)
      (ok {:share-hash share-hash})
      (bad-request (at/build-error-body :error-deleting-schnaq "An error occurred, while deleting the schnaq.")))))

(defn- check-edit-discussion-error
  "Check if an editor is the author of a discussion or if it is deleted."
  [user-identity author-identity share-hash ok-answer bad-answer forbidden-answer]
  (if (= user-identity author-identity)
    (if (discussion-db/discussion-deleted? share-hash)
      bad-answer
      ok-answer)
    forbidden-answer))

(defn- edit-schnaq-title!
  "Edit title of a schnaq"
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash new-title]} (:body parameters)
        user-identity (:sub identity)
        discussion (discussion-db/discussion-by-share-hash share-hash)
        author-identity (-> discussion :discussion/author :user.registered/keycloak-id)]
    (check-edit-discussion-error
     user-identity author-identity share-hash
     (do (discussion-db/edit-title share-hash new-title)
         (ok {:schnaq (discussion-db/discussion-by-share-hash share-hash)}))
     (bad-request (at/build-error-body :discussion-not-the-author
                                       "You can not edit the title of a deleted discussion."))
     (forbidden (at/build-error-body :discussion-not-the-author
                                     "You can not edit the title of someone else's discussion.")))))

(defn- add-visited-schnaq
  "Add schnaq id to visited schnaqs by share-hash"
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash]} (:body parameters)
        user-identity (:sub identity)
        discussion-id (:db/id (discussion-db/discussion-by-share-hash share-hash))]
    (user-db/update-visited-schnaqs user-identity [discussion-id])
    (ok {:share-hash share-hash})))

(defn- remove-visited-schnaq
  "Remove a schnaq id from visited schnaqs by share-hash"
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash]} (:body parameters)
        keycloak-id (:sub identity)]
    (user-db/remove-visited-schnaq keycloak-id share-hash)
    (ok {:share-hash share-hash})))

(defn- archive-schnaq
  "Add a share-hash to a user's archived schnaqs."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash]} (:body parameters)
        keycloak-id (:sub identity)]
    (user-db/archive-schnaq keycloak-id share-hash)
    (ok {:share-hash share-hash})))

(defn- unarchive-schnaq
  "Remove a schnaq from the archived schnaqs."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash]} (:body parameters)
        keycloak-id (:sub identity)]
    (user-db/unarchive-schnaq keycloak-id share-hash)
    (ok {:share-hash share-hash})))

(defn- search-qa
  "Search through any valid discussion."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash search-string display-name]} (:query parameters)
        keycloak-id (:sub identity)
        user-id (user-db/user-id display-name keycloak-id)
        matching-statements (discussion-db/search-similar-questions share-hash search-string)]
    (ok (processors/statement-default
         {:matching-statements matching-statements
          :children (discussion-db/children-from-statements matching-statements)}
         share-hash keycloak-id user-id))))

;; -----------------------------------------------------------------------------

(def schnaq-routes
  [["" {:swagger {:tags ["schnaqs"]}}
    ["/schnaq"
     ["/by-hash" {:get schnaq-by-hash
                  :description (at/get-doc #'schnaq-by-hash)
                  :name :api.schnaq/by-hash
                  :middleware [:discussion/valid-share-hash?]
                  :parameters {:query {:share-hash :discussion/share-hash
                                       :display-name ::specs/non-blank-string}}
                  :responses {200 {:body {:schnaq ::specs/discussion
                                          :entity-ids map?}}
                              403 at/response-error-body}}]
     ["/join" {:get schnaq-by-access-code
               :description (at/get-doc #'schnaq-by-access-code)
               :name :api.schnaq/by-access-code
               :parameters {:query {:access-code :discussion.access/code}}
               :responses {200 {:body {:location string?}}
                           403 at/response-error-body}}]
     ["/add-visited" {:put add-visited-schnaq
                      :description (at/get-doc #'add-visited-schnaq)
                      :name :api.schnaq/add-visited
                      :middleware [:user/authenticated?
                                   :discussion/valid-share-hash?]
                      :parameters {:body {:share-hash :discussion/share-hash}}
                      :responses {200 {:body {:share-hash :discussion/share-hash}}
                                  400 at/response-error-body}}]
     ["/remove-visited" {:delete remove-visited-schnaq
                         :description (at/get-doc #'remove-visited-schnaq)
                         :name :api.schnaq/remove-visited
                         :middleware [:user/authenticated?
                                      :discussion/valid-share-hash?]
                         :parameters {:body {:share-hash :discussion/share-hash}}
                         :responses {200 {:body {:share-hash :discussion/share-hash}}
                                     400 at/response-error-body}}]
     ["/archive" {:name :api.schnaq/archive
                  :middleware [:user/authenticated?
                               :discussion/valid-share-hash?]
                  :parameters {:body {:share-hash :discussion/share-hash}}
                  :responses {200 {:body {:share-hash :discussion/share-hash}}}
                  :put {:handler archive-schnaq
                        :description (at/get-doc #'archive-schnaq)}
                  :delete {:handler unarchive-schnaq
                           :description (at/get-doc #'unarchive-schnaq)}}]

     ["/add" {:post add-schnaq
              :description (at/get-doc #'add-schnaq)
              :name :api.schnaq/add
              :parameters {:body ::dto/discussion-add-body}
              :middleware [:user/authenticated?]
              :responses {201 {:body {:new-schnaq ::dto/discussion}}
                          400 at/response-error-body}}]
     ["/edit/title" {:put edit-schnaq-title!
                     :description (at/get-doc #'edit-schnaq-title!)
                     :name :api.schnaq/edit-title
                     :middleware [:discussion/valid-credentials?]
                     :parameters {:body {:share-hash :discussion/share-hash
                                         :edit-hash :discussion/edit-hash
                                         :new-title :discussion/title}}
                     :responses {201 {:body {:schnaq ::dto/discussion}}
                                 400 at/response-error-body
                                 403 at/response-error-body}}]
     ["/by-hash-as-admin" {:post schnaq-by-hash-as-admin
                           :description (at/get-doc #'schnaq-by-hash-as-admin)
                           :name :api.schnaq/by-hash-as-admin
                           :middleware [:discussion/valid-credentials?]
                           :parameters {:body {:share-hash :discussion/share-hash
                                               :edit-hash :discussion/edit-hash}}
                           :responses {200 {:body {:schnaq ::dto/discussion}}}}]
     ["/qa"
      ["/search" {:get search-qa
                  :description (at/get-doc #'search-qa)
                  :name :api.schnaq.qa/search
                  :middleware [:discussion/valid-share-hash?]
                  :parameters {:query {:share-hash :discussion/share-hash
                                       :search-string string?
                                       :display-name ::specs/non-blank-string}}
                  :responses {200 {:body {:matching-statements (s/coll-of ::dto/statement)}}
                              404 at/response-error-body}}]]]

    ["/schnaqs/by-hashes"
     {:post schnaqs-by-hashes
      :name :api.schnaqs/by-hashes
      :description (at/get-doc #'schnaqs-by-hashes)
      :parameters {:body {:share-hashes (s/or :share-hashes (st/spec {:spec (s/coll-of :discussion/share-hash)
                                                                      :swagger/collectionFormat "multi"})
                                              :share-hash :discussion/share-hash)
                          :display-name ::specs/non-blank-string}}
      :responses {200 {:body {:schnaqs (s/coll-of ::dto/discussion)}}
                  404 at/response-error-body}}]
    ["/admin" {:swagger {:tags ["admin"]}
               :responses {401 at/response-error-body}
               :middleware [:user/authenticated? :user/admin?]}
     ["/schnaq/delete" {:delete delete-schnaq!
                        :name :api.schnaq.admin/delete
                        :description (at/get-doc #'delete-schnaq!)
                        :parameters {:body {:share-hash :discussion/share-hash}}
                        :responses {200 {:share-hash :discussion/share-hash}
                                    400 at/response-error-body}}]]]])
