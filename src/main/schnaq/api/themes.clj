(ns schnaq.api.themes
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok bad-request]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.specs :as specs]
            [schnaq.database.themes :as themes-db]
            [taoensso.timbre :as log]))

(defn- personal
  "Return all themes for a specific user."
  [{{:keys [sub]} :identity}]
  (ok {:themes (themes-db/themes-by-keycloak-id sub)}))

(defn- add-theme
  "Save newly configured theme."
  [{{:keys [sub]} :identity
    {{:keys [theme]} :body} :parameters}]
  (ok {:theme (themes-db/new-theme sub theme)}))

(defn- edit-theme
  "Change the content of a theme"
  [{{:keys [sub]} :identity
    {{:keys [theme]} :body} :parameters}]
  (ok {:theme (themes-db/edit-theme sub theme)}))

(defn- delete-theme
  "Delete a theme."
  [{{:keys [sub]} :identity
    {{:keys [theme-id]} :body} :parameters}]
  (if (themes-db/delete-theme sub theme-id)
    (ok {:themes (themes-db/themes-by-keycloak-id sub)})
    (bad-request (at/build-error-body :theme/not-deleted "Did not delete theme. Either the theme does not exist or the requesting user is not the author of the theme."))))

;; -----------------------------------------------------------------------------

(def theme-routes
  [["/user" {:swagger {:tags ["themes"]}
             :middleware [:user/authenticated?
                          :user/pro-user?]}
    ["/themes"
     {:get personal
      :description (at/get-doc #'personal)
      :name :api.themes/personal
      :responses {200 {:body {:themes (s/coll-of ::specs/theme)}}
                  400 at/response-error-body}}]
    ["/theme"
     ["/add" {:post add-theme
              :description (at/get-doc #'add-theme)
              :name :api.theme/add
              :parameters {:body {:theme ::specs/theme}}
              :responses {200 {:body {:theme ::specs/theme}}
                          400 at/response-error-body}}]
     ["/edit" {:put edit-theme
               :description (at/get-doc #'edit-theme)
               :name :api.theme/edit
               :parameters {:body {:theme ::specs/theme}}
               :responses {200 {:body {:theme ::specs/theme}}
                           400 at/response-error-body}}]
     ["/delete" {:delete delete-theme
                 :description (at/get-doc #'delete-theme)
                 :name :api.theme/delete
                 :parameters {:body {:theme-id :db/id}}
                 :responses {200 {:body {:themes (s/coll-of ::specs/theme)}}
                             400 at/response-error-body}}]]]])

