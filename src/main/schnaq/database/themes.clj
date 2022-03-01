(ns schnaq.database.themes
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn >defn- ? =>]]
            [schnaq.database.main :as db]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs]
            [schnaq.shared-toolbelt :as shared-tools])
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
     [(shared-tools/clean-db-vals new-theme)]
     temp-id patterns/theme)))

(>defn edit-theme
  "Saves the provided theme for a given user."
  [keycloak-id theme]
  [:user.registered/keycloak-id ::specs/theme => (? ::specs/theme)]
  (prn "edit-theme")
  (prn theme)
  (let [prepared-theme (assoc theme :theme/user [:user.registered/keycloak-id keycloak-id])]
    @(db/transact [(shared-tools/clean-db-vals prepared-theme)])
    (db/fast-pull (:db/id theme) patterns/theme)))

(>defn delete-theme
  "Delete a theme."
  [theme-id]
  [:db/id => map?]
  @(db/transact [[:db/retractEntity theme-id]]))

(>defn assign-theme
  "Assigns a theme to a discussion."
  [share-hash theme-id]
  [:discussion/share-hash :db/id => map?]
  @(db/transact [[:db/add [:discussion/share-hash share-hash] :discussion/theme theme-id]]))

(>defn unassign-theme
  "Unassign theme from discussion."
  [share-hash]
  [:discussion/share-hash => map?]
  @(db/transact [[:db/retract [:discussion/share-hash share-hash] :discussion/theme]]))
