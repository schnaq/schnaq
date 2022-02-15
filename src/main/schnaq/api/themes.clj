(ns schnaq.api.themes
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok bad-request]]
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
  (prn (themes-db/themes-by-keycloak-id sub))
  (if-let [theme (themes-db/new-theme sub theme)]
    (ok {:theme theme})
    (bad-request (at/build-error-body :theme/title-taken "The title for the theme is already in use by you"))))

(comment

  (new-theme {:identity {:sub "d6d8a351-2074-46ff-aa9b-9c57ab6c6a18"}
              :parameters {:body
                           {:theme.images/logo "string"
                            :theme.images/activation "string"
                            :theme/title "string"
                            :theme.colors/secondary "#123123"
                            :theme.colors/background "#123123"
                            :theme.colors/primary "#123123"}}})

  nil)

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

