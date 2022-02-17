(ns schnaq.database.themes
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn >defn- ? =>]]
            [schnaq.database.main :as db]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs])
  (:import java.util.UUID))

(>defn themes-by-keycloak-id
  "Return all themes for a given user by it's `keycloak-id`."
  [keycloak-id]
  [:user.registered/keycloak-id => (s/coll-of ::specs/theme)]
  (flatten
   (db/query '[:find (pull ?theme pattern)
               :in $ ?keycloak-id pattern
               :where [?user :user.registered/keycloak-id ?keycloak-id]
               [?theme :theme/user ?user]]
             keycloak-id patterns/theme)))

(>defn- query-existing-theme
  "Query theme if exists."
  [keycloak-id {:theme/keys [title]}]
  [:user.registered/keycloak-id ::specs/theme => (? ::specs/theme)]
  (first
   (db/query '[:find [(pull ?theme pattern)]
               :in $ ?keycloak-id ?theme-title pattern
               :where [?user :user.registered/keycloak-id ?keycloak-id]
               [?theme :theme/user ?user]
               [?theme :theme/title ?theme-title]]
             keycloak-id title patterns/theme)))

(>defn- user-is-theme-author?
  "Verify that a user is the theme author."
  [keycloak-id theme-id]
  [:user.registered/keycloak-id :db/id => boolean?]
  (let [theme-author (get-in
                      (db/fast-pull theme-id [{:theme/user [:user.registered/keycloak-id]}])
                      [:theme/user :user.registered/keycloak-id])]
    (= keycloak-id theme-author)))

(>defn new-theme
  "Saves the provided theme for a given user."
  [keycloak-id theme]
  [:user.registered/keycloak-id ::specs/theme => ::specs/theme]
  (let [temp-id (str "new-theme-" keycloak-id)
        theme-title-exists? (query-existing-theme keycloak-id theme)
        unique-title (if theme-title-exists?
                       (format "%s-%s" (:theme/title theme) (.toString (UUID/randomUUID)))
                       (:theme/title theme))
        new-theme (assoc theme
                         :theme/user [:user.registered/keycloak-id keycloak-id]
                         :theme/title unique-title
                         :db/id temp-id)]
    (db/transact-and-pull-temp
     [new-theme]
     temp-id patterns/theme)))

(>defn edit-theme
  "Saves the provided theme for a given user."
  [keycloak-id theme]
  [:user.registered/keycloak-id ::specs/theme => (? ::specs/theme)]
  (when (user-is-theme-author? keycloak-id (:db/id theme))
    (let [prepared-theme (-> theme
                             (assoc :theme/user [:user.registered/keycloak-id keycloak-id])
                             (update :db/id #(Long/valueOf %)))]
      (db/transact [prepared-theme])
      (db/fast-pull (:db/id prepared-theme) patterns/theme))))

(>defn delete-theme
  "Delete a theme."
  [keycloak-id theme-id]
  [:user.registered/keycloak-id :db/id => any?]
  (when (user-is-theme-author? keycloak-id theme-id)
    (db/transact [[:db/retractEntity theme-id]])))
