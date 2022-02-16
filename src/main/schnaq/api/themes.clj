(ns schnaq.api.themes
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.specs :as specs]
            [schnaq.database.themes :as themes-db]))

(defn- personal
  "Return all themes for a specific user."
  [{{:keys [sub]} :identity}]
  (ok {:themes (themes-db/themes-by-keycloak-id sub)}))

(defn- add-theme
  "Save newly configured theme."
  [{{:keys [sub]} :identity
    {{:keys [theme]} :body} :parameters}]
  (themes-db/new-theme sub theme)
  (ok {:themes (themes-db/themes-by-keycloak-id sub)}))

(defn- edit-theme
  "TODO"
  [{{:keys [sub]} :identity
    {{:keys [theme]} :body} :parameters}]
  (themes-db/edit-theme sub theme)
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
                   :responses {200 {:body {:themes (s/or :no-themes empty? :themes (s/coll-of ::specs/theme))}}
                               400 at/response-error-body}}]]
    ["/theme"
     ["/add" {:post add-theme
              :description (at/get-doc #'add-theme)
              :name :api.theme/add
              :parameters {:body {:theme ::specs/theme}}
              :responses {200 {:body {:themes (s/coll-of ::specs/theme)}}
                          400 at/response-error-body}}]
     ["/edit" {:put edit-theme
               :description (at/get-doc #'edit-theme)
               :name :api.theme/edit
               :parameters {:body {:theme ::specs/theme}}
               :responses {200 {:body {:themes (s/coll-of ::specs/theme)}}
                           400 at/response-error-body}}]]]])

