(ns schnaq.api.themes
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [com.fulcrologic.guardrails.core :refer [=> >defn- ?]]
            [ring.util.http-response :refer [bad-request forbidden ok]]
            [schnaq.api.middlewares :as middlewares]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.database.main :as db]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs]
            [schnaq.database.themes :as themes-db]
            [schnaq.media :as media]
            [schnaq.s3 :as s3]
            [taoensso.timbre :as log]))

(>defn- file-name
  "Create a theme-path in the bucket, prefixed with the keycloak-id."
  [keycloak-id theme-id image-name file-type]
  [:user.registered/keycloak-id :db/id :file/name :file/type => string?]
  (format "%s/themes/%s/%s.%s" keycloak-id theme-id
          image-name (media/mime-type->file-ending file-type)))

(>defn- upload-theme-image
  "Takes an image and uploads it."
  [{:keys [content type] :as image} keycloak-id theme-id image-name image-width]
  [::specs/image :user.registered/keycloak-id :db/id string? nat-int? => (? ::specs/file-stored)]
  (when content
    (media/upload-image! image (file-name keycloak-id theme-id image-name type) image-width :user/media)))

(>defn- prepare-images
  "Adds the raw images to the theme, if provided."
  [keycloak-id theme-id raw-logo raw-header]
  [:user.registered/keycloak-id :db/id (? ::specs/image) (? ::specs/image) => map?]
  (let [logo (upload-theme-image raw-logo keycloak-id theme-id "logo" config/image-max-width-logo)
        header (upload-theme-image raw-header keycloak-id theme-id "header" config/image-max-width-header)]
    (when-let [error (or (:error logo) (:error header))]
      (let [message (or (:message logo) (:message header))
            error-msg (format "Image upload failed: %s, %s" error message)]
        (log/error error-msg)
        (throw (ex-info error-msg {:error error :message message}))))
    (cond-> {}
      (:url logo) (assoc :theme.images/logo (:url logo))
      (:url header) (assoc :theme.images/header (:url header)))))

(>defn- url->path-to-file
  "Takes an url in our s3 store and returns the path to the theme file after the
  bucket's name."
  [url]
  [string? => string?]
  (->> (str/split url #"/") (drop 4) (str/join "/")))

(defn- delete-images!
  "Delete images of a theme."
  [theme-id]
  (let [{:theme.images/keys [logo header]} (themes-db/theme-by-id theme-id)]
    (run! #(s3/delete-file :user/media (url->path-to-file %))
          [logo header])))

;; -----------------------------------------------------------------------------
;; Endpoints

(defn- personal
  "Return all themes for a specific user."
  [{{:keys [sub]} :identity}]
  (ok {:themes (themes-db/themes-by-keycloak-id sub)}))

(defn- add-theme
  "Save newly configured theme."
  [{{:keys [sub]} :identity
    {{:keys [theme]} :body} :parameters}]
  (try
    (let [theme-no-raw-images (dissoc theme :theme.images.raw/logo :theme.images.raw/header)
          new-theme (themes-db/new-theme sub theme-no-raw-images)
          images (prepare-images
                  sub (:db/id new-theme)
                  (:theme.images.raw/logo theme)
                  (:theme.images.raw/header theme))
          imaged-theme (if-not (empty? images)
                         (themes-db/edit-theme sub (assoc images :db/id (:db/id new-theme)))
                         new-theme)]
      (ok {:theme imaged-theme}))
    (catch Exception e
      (let [{:keys [error message]} (ex-data e)]
        (bad-request (at/build-error-body error message))))))

(defn- edit-theme
  "Change the content of a theme."
  [{{:keys [sub]} :identity
    {{:keys [theme delete-header? delete-logo?]} :body} :parameters}]
  (try
    (let [images (prepare-images sub (:db/id theme) (:theme.images.raw/logo theme) (:theme.images.raw/header theme))
          prepared-theme (-> theme
                             (merge images)
                             (dissoc :theme.images.raw/logo :theme.images.raw/header))
          updated-theme (themes-db/edit-theme sub prepared-theme)
          theme-id (:db/id updated-theme)]
      (when delete-header?
        (themes-db/delete-header theme-id)
        (s3/delete-file :user/media (url->path-to-file (:theme.images/header updated-theme))))
      (when delete-logo?
        (themes-db/delete-logo theme-id)
        (s3/delete-file :user/media (url->path-to-file (:theme.images/logo updated-theme))))
      (ok {:theme (db/fast-pull theme-id patterns/theme)}))
    (catch Exception e
      (let [{:keys [error message]} (ex-data e)]
        (bad-request (at/build-error-body error message))))))

(defn- delete-theme
  "Delete a theme."
  [{{:keys [sub]} :identity
    {{:keys [theme]} :body} :parameters}]
  (delete-images! (:db/id theme))
  (themes-db/delete-theme (:db/id theme))
  (ok {:themes (themes-db/themes-by-keycloak-id sub)}))

(defn- assign-theme
  "Assign a theme to a discussion."
  [{{{:keys [theme share-hash]} :body} :parameters}]
  (themes-db/assign-theme share-hash (:db/id theme))
  (ok {:assigned? true}))

(defn- unassign-theme
  "Unassign a discussion's theme."
  [{{{:keys [share-hash]} :body} :parameters}]
  (themes-db/unassign-theme share-hash)
  (ok {:unassigned? true}))

;; -----------------------------------------------------------------------------

(>defn- user-is-theme-author?
  "Verify that a user is the theme author."
  [keycloak-id theme-id]
  [:user.registered/keycloak-id :db/id => boolean?]
  (let [theme-author (get-in
                      (db/fast-pull theme-id [{:theme/user [:user.registered/keycloak-id]}])
                      [:theme/user :user.registered/keycloak-id])]
    (= keycloak-id theme-author)))

(defn user-is-theme-author?-middleware
  "Verify that the user has the correct permissions to "
  [handler]
  (fn [request]
    (let [keycloak-id (get-in request [:identity :sub])
          request' (update-in request [:parameters :body :theme :db/id] #(Long/valueOf %))
          theme-id (:db/id (middlewares/extract-parameter-from-request request' :theme))]
      (if (user-is-theme-author? keycloak-id theme-id)
        (handler request')
        (forbidden (at/build-error-body :themes/not-the-author "You are not the author of this theme."))))))

;; -----------------------------------------------------------------------------

(def theme-routes
  [["/user" {:swagger {:tags ["themes"]}
             :middleware [:user/authenticated?
                          :user/pro-user?]
             :responses {400 at/response-error-body}}
    ["/themes"
     {:get personal
      :description (at/get-doc #'personal)
      :name :api.themes/personal
      :responses {200 {:body {:themes (s/coll-of ::specs/theme)}}}}]
    ["/theme"
     ["/add" {:post add-theme
              :description (at/get-doc #'add-theme)
              :name :api.theme/add
              :parameters {:body {:theme ::specs/theme}}
              :responses {200 {:body {:theme ::specs/theme}}}}]
     ["/edit" {:put edit-theme
               :description (at/get-doc #'edit-theme)
               :name :api.theme/edit
               :middleware [user-is-theme-author?-middleware]
               :parameters {:body {:theme ::specs/theme
                                   :delete-header? boolean?
                                   :delete-logo? boolean?}}
               :responses {200 {:body {:theme ::specs/theme}}}}]
     ["/delete" {:delete delete-theme
                 :description (at/get-doc #'delete-theme)
                 :name :api.theme/delete
                 :middleware [user-is-theme-author?-middleware]
                 :parameters {:body {:theme ::specs/theme}}
                 :responses {200 {:body {:themes (s/coll-of ::specs/theme)}}}}]
     ["/discussion" {:middleware [:discussion/valid-credentials?]}
      ["/assign" {:put assign-theme
                  :description (at/get-doc #'assign-theme)
                  :name :api.theme.discussion/assign
                  :parameters {:body {:theme ::specs/theme
                                      :share-hash :discussion/share-hash
                                      :edit-hash :discussion/edit-hash}}
                  :responses {200 {:body {:assigned? boolean?}}}}]
      ["/unassign" {:delete unassign-theme
                    :description (at/get-doc #'unassign-theme)
                    :name :api.theme.discussion/unassign
                    :parameters {:body {:share-hash :discussion/share-hash
                                        :edit-hash :discussion/edit-hash}}
                    :responses {200 {:body {:unassigned? boolean?}}}}]]]]])
