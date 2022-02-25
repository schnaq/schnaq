(ns schnaq.api.themes
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn- =>]]
            [ring.util.http-response :refer [ok forbidden bad-request]]
            [schnaq.api.middlewares :as middlewares]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.main :as db]
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
  (ok {:theme (themes-db/new-theme sub theme)}))

(defn- edit-theme
  "Change the content of a theme"
  [{{:keys [sub]} :identity
    {{:keys [theme]} :body} :parameters}]
  (let [theme-converted-id (update theme :db/id #(Long/valueOf %))]
    (ok {:theme (themes-db/edit-theme sub theme-converted-id)})))

(defn- delete-theme
  "Delete a theme."
  [{{:keys [sub]} :identity
    {{:keys [theme-id]} :body} :parameters}]
  (if (themes-db/delete-theme theme-id)
    (ok {:themes (themes-db/themes-by-keycloak-id sub)})
    (bad-request (at/build-error-body :theme/not-deleted "Did not delete theme. Either the theme does not exist or the requesting user is not the author of the theme."))))

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
    (prn "theme author:" theme-author)
    (prn "keycloak-id" keycloak-id)
    (prn (= keycloak-id theme-author))
    (= keycloak-id theme-author)))

(defn user-is-theme-author?-middleware
  "Verify that the user has the correct permissions to "
  [handler]
  (fn [request]
    (let [keycloak-id (get-in request [:identity :sub])
          theme-id (:db/id (middlewares/extract-parameter-from-request request :theme))]
      (if (user-is-theme-author? keycloak-id theme-id)
        (handler request)
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
               :parameters {:body {:theme ::specs/theme}}
               :responses {200 {:body {:theme ::specs/theme}}}}]
     ["/delete" {:delete delete-theme
                 :description (at/get-doc #'delete-theme)
                 :name :api.theme/delete
                 :middleware [user-is-theme-author?-middleware]
                 :parameters {:body {:theme ::specs/theme}}
                 :responses {200 {:body {:themes (s/coll-of ::specs/theme)}}}}]
     ["/discussion"
      ["/assign" {:put assign-theme
                  :description (at/get-doc #'assign-theme)
                  :name :api.theme.discussion/assign
                  :parameters {:body {:theme ::specs/theme
                                      :share-hash :discussion/share-hash}}
                  :responses {200 {:body {:assigned? boolean?}}}}]
      ["/unassign" {:delete unassign-theme
                    :description (at/get-doc #'unassign-theme)
                    :name :api.theme.discussion/unassign
                    :parameters {:body {:share-hash :discussion/share-hash}}
                    :responses {200 {:body {:unassigned? boolean?}}}}]]]]])

