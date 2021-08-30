(ns schnaq.api.user
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok created]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.mail.emails :as mail]
            [schnaq.media :as media]
            [taoensso.timbre :as log]))

(defn- register-user-if-they-not-exist
  "Register a new user if they do not exist. In all cases return the user. New
  users will receive a welcome mail. `creation-secrets` can optionally be provided
  to associate previous created entities with the registered user."
  [{:keys [identity parameters]}]
  (log/info "User-Registration queried for" (:id identity)
            ", username:" (:preferred_username identity))
  (let [{:keys [creation-secrets visited-hashes visited-statement-ids]} (:body parameters)
        visited-schnaqs (map :db/id (discussion-db/valid-discussions-by-hashes visited-hashes))
        [new-user? queried-user] (user-db/register-new-user identity visited-schnaqs visited-statement-ids)
        updated-statements? (associative? (discussion-db/update-authors-from-secrets
                                            creation-secrets (:db/id queried-user)))
        response {:registered-user queried-user
                  :updated-statements? updated-statements?}]
    (if new-user?
      (do (mail/send-welcome-mail (:email identity))
          (created "" response))
      (ok response))))

(defn- change-profile-picture
  "Change the profile picture of a user.
  This includes uploading an image to s3 and updating the associated url in the database."
  [{:keys [identity parameters]}]
  (let [image-type (get-in parameters [:body :image :type])
        image-name (get-in parameters [:body :image :name])
        image-content (get-in parameters [:body :image :content])]
    (log/info "User" (:id identity) "trying to set profile picture to:" image-name)
    (let [{:keys [image-url] :as response} (media/upload-image! image-type image-content :user/profile-pictures)]
      (if image-url
        (ok {:updated-user (user-db/update-profile-picture-url (:id identity) image-url)})
        response))))

(defn- change-display-name
  "Change the display name of a registered user."
  [{:keys [parameters identity]}]
  (let [display-name (get-in parameters [:body :display-name])]
    (ok {:updated-user (user-db/update-display-name (:id identity) display-name)})))

(defn- add-anonymous-user
  "Generate a user based on the nickname. This is an *anonymous* user, and we
  can only refer to the user by the nickname. So this function is idempotent and
  returns always the same id when providing the same nickname."
  [{:keys [parameters]}]
  (let [author-name (get-in parameters [:body :nickname])
        user-id (user-db/add-user-if-not-exists author-name)]
    (created "" {:user-id user-id})))

;; -----------------------------------------------------------------------------

(s/def ::creation-secrets map?)
(s/def ::visited-hashes (s/coll-of :discussion/share-hash))
(s/def ::visited-statement-ids map?)
(s/def ::user-register (s/keys :req-un [::visited-hashes]
                               :opt-un [::creation-secrets
                                        ::visited-statement-ids]))

(def user-routes
  ["/user" {:swagger {:tags ["user"]}}
   ["/anonymous/add" {:put add-anonymous-user
                      :description (at/get-doc #'add-anonymous-user)
                      :parameters {:body {:nickname :user/nickname}}
                      :responses {201 {:body {:user-id :db/id}}}}]
   ["" {:middleware [:user/authenticated?]}
    ["/register" {:put register-user-if-they-not-exist
                  :description (at/get-doc #'register-user-if-they-not-exist)
                  :parameters {:body ::user-register}
                  :responses {201 {:body {:registered-user ::specs/registered-user
                                          :updated-statements? boolean?}}
                              200 {:body {:registered-user ::specs/registered-user
                                          :updated-statements? boolean?}}}}]
    ["/picture" {:put change-profile-picture
                 :description (at/get-doc #'change-profile-picture)
                 :parameters {:body {:image ::specs/image}}
                 :responses {200 {:body {:updated-user ::specs/registered-user}}
                             400 at/response-error-body}}]
    ["/name" {:put change-display-name
              :description (at/get-doc #'change-display-name)
              :parameters {:body {:display-name :user/nickname}}
              :responses {200 {:body {:updated-user ::specs/registered-user}}}}]]])
