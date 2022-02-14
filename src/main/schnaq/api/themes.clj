(ns schnaq.api.themes
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.specs :as specs]))

(defn- personal-themes
  "TODO"
  []
  (ok {:message "yep"}))

(def theme-routes
  [["/themes" {:swagger {:tags ["themes"]}
               :middleware [:user/authenticated?
                            :user/pro-user?
                            :discussion/valid-credentials?]}
    ["" {:get personal-themes
         :description (at/get-doc #'personal-themes)
         :name :api.themes/personal
         :responses {200 {:body {:themes (s/coll-of ::specs/theme)}}
                     400 at/response-error-body}}]]])

