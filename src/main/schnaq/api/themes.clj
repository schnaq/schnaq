(ns schnaq.api.themes
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
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
  (log/info "Add theme" theme)
  (ok {:theme (themes-db/new-theme sub theme)}))

(defn- edit-theme
  "TODO"
  [{{:keys [sub]} :identity
    {{:keys [theme]} :body} :parameters}]
  (log/info "Edit theme" theme)
  (log/info "From user" sub)
  (ok {:theme (themes-db/edit-theme sub theme)}))

(defn- delete-theme
  "TODO"
  [{{:keys [sub]} :identity
    {{:keys [theme-id]} :body} :parameters}]
  (themes-db/delete-theme sub theme-id)
  (ok {:themes (themes-db/themes-by-keycloak-id sub)}))

;; -----------------------------------------------------------------------------

(def theme-routes
  [["" {:swagger {:tags ["themes"]}
        :middleware [:user/authenticated?
                     :user/pro-user?]}
    ["/themes"
     ["/personal" {:get personal
                   :description (at/get-doc #'personal)
                   :name :api.themes/personal
                   :responses {200 {:body {:themes (s/coll-of ::specs/theme)}}
                               400 at/response-error-body}}]]
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

