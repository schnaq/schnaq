(ns schnaq.api.themes
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [schnaq.api.dto-specs :as dto]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.specs :as specs]
            [schnaq.database.themes :as themes-db]
            [taoensso.timbre :as log]))

(defn- personal
  "Return all themes for a specific user."
  [{{:keys [sub]} :identity}]
  (ok {:themes (themes-db/themes-by-keycloak-id sub)}))

(defn- new-theme
  "Save newly configured theme."
  [{{:keys [sub]} :identity
    {theme :body} :parameters}]
  (ok {:theme (themes-db/new-theme sub theme)}))

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
     ["/new" {:post new-theme
              :description (at/get-doc #'new-theme)
              :name :api.theme/new
              :parameters {:body ::dto/theme}
              :responses {200 {:body {:theme ::specs/theme}}
                          400 at/response-error-body}}]]]])

