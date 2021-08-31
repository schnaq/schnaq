(ns schnaq.api.schnaq
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn-]]
            [ring.util.http-response :refer [ok created bad-request]]
            [schnaq.api.dto-specs :as dto]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.hub :as hub-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.links :as links]
            [schnaq.processors :as processors]
            [schnaq.validator :as validator]
            [spec-tools.core :as st]
            [taoensso.timbre :as log])
  (:import (java.util UUID)
           (java.time LocalDateTime ZoneOffset)))

(defn- schnaq-by-hash
  "Returns a schnaq, identified by its share-hash."
  [{:keys [parameters identity]}]
  (let [share-hash (get-in parameters [:query :share-hash])
        keycloak-id (:sub identity)]
    (ok {:schnaq (-> (if (and keycloak-id (validator/user-schnaq-admin? share-hash keycloak-id))
                       (discussion-db/discussion-by-share-hash-private share-hash)
                       (discussion-db/discussion-by-share-hash share-hash))
                     processors/add-meta-info-to-schnaq
                     processors/with-sub-discussion-info
                     (processors/with-new-post-info share-hash keycloak-id)
                     processors/hide-deleted-statement-content
                     processors/with-votes)})))

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
           (map #(-> % processors/add-meta-info-to-schnaq processors/with-votes)
                (discussion-db/valid-discussions-by-hashes share-hashes-list))}))
    at/not-found-hash-invalid))

(defn- public-schnaqs
  "Return all public schnaqs."
  [_req]
  (ok {:schnaqs (map processors/add-meta-info-to-schnaq (discussion-db/public-discussions))}))

(>defn- now-plus-days-instant
  "Adds a number of days to the current datetime and then converts that to an instant."
  [days]
  [integer? :ret inst?]
  (.toInstant (.plusDays (LocalDateTime/now) days) ZoneOffset/UTC))

(defn- add-schnaq
  "Adds a discussion to the database. Returns the newly-created discussion."
  [{:keys [parameters identity]}]
  (let [{:keys [nickname discussion-title public-discussion? hub-exclusive? hub ends-in-days]} (:body parameters)
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
                                (assoc :discussion/hub-origin [:hub/keycloak-name hub])
                                ends-in-days (assoc :discussion/end-time (now-plus-days-instant ends-in-days)))
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


;; -----------------------------------------------------------------------------
(s/def ::discussion-title :discussion/title)
(s/def ::public-discussion? boolean?)
(s/def ::ends-in-days pos-int?)
(s/def ::hub-exclusive? boolean?)
(s/def ::hub :hub/keycloak-name)
(s/def ::schnaq-add-body
  (s/keys :req-un [::discussion-title ::public-discussion?]
          :opt-un [::ends-in-days :user/nickname ::hub-exclusive? ::hub]))

(def schnaq-routes
  [["" {:swagger {:tags ["schnaqs"]}}
    ["/schnaq"
     ["/by-hash" {:get schnaq-by-hash
                  :description (at/get-doc #'schnaq-by-hash)
                  :name :api.schnaq/by-hash
                  :middleware [:discussion/valid-share-hash?]
                  :parameters {:query {:share-hash :discussion/share-hash}}
                  :responses {200 {:body {:schnaq ::specs/discussion}}
                              403 at/response-error-body}}]
     ["/add" {:post add-schnaq
              :description (at/get-doc #'add-schnaq)
              :name :api.schnaq/add
              :parameters {:body ::schnaq-add-body}
              :responses {201 {:body {:new-schnaq ::dto/discussion}}
                          400 at/response-error-body}}]
     ["/by-hash-as-admin" {:post schnaq-by-hash-as-admin
                           :description (at/get-doc #'schnaq-by-hash-as-admin)
                           :name :api.schnaq/by-hash-as-admin
                           :middleware [:discussion/valid-credentials?]
                           :parameters {:body {:share-hash :discussion/share-hash
                                               :edit-hash :discussion/edit-hash}}
                           :responses {200 {:body {:schnaq ::dto/discussion}}}}]]

    ["/schnaqs"
     ["/by-hashes" {:get schnaqs-by-hashes
                    :name :api.schnaqs/by-hashes
                    :description (at/get-doc #'schnaqs-by-hashes)
                    :parameters {:query {:share-hashes (s/or :share-hashes (st/spec {:spec (s/coll-of :discussion/share-hash)
                                                                                     :swagger/collectionFormat "multi"})
                                                             :share-hash :discussion/share-hash)}}
                    :responses {200 {:body {:schnaqs (s/coll-of ::dto/discussion)}}
                                404 at/response-error-body}}]
     ["/public" {:get public-schnaqs
                 :name :api.schnaqs/public
                 :description (at/get-doc #'public-schnaqs)
                 :responses {200 {:body {:schnaqs (s/coll-of ::dto/discussion)}}}}]]
    ["/admin" {:swagger {:tags ["admin"]}
               :responses {401 at/response-error-body}
               :middleware [:user/authenticated? :user/admin?]}
     ["/schnaq/delete" {:delete delete-schnaq!
                        :name :api.schnaq.admin/delete
                        :description (at/get-doc #'delete-schnaq!)
                        :parameters {:body {:share-hash :discussion/share-hash}}
                        :responses {200 {:share-hash :discussion/share-hash}
                                    400 at/response-error-body}}]]]])
