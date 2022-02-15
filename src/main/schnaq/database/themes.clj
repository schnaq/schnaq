(ns schnaq.database.themes
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn >defn- ? =>]]
            [schnaq.api.dto-specs :as dto]
            [schnaq.database.main :as db]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs]
            [schnaq.toolbelt :as tools]))

(def ^:private keycloak-id "d6d8a351-2074-46ff-aa9b-9c57ab6c6a18")
(def ^:private beta-keycloak-id "8278b980-bf33-45c0-924c-5d7c8f64a564")

(def theme
  {:theme.images/logo "string"
   :theme.images/activation "string"
   :theme/title "string"
   :theme.colors/secondary "#123123"
   :theme.colors/background "#123123"
   :theme.colors/primary "#123123"})

(>defn themes-by-keycloak-id
  "Return all themes for a given user by it's `keycloak-id`."
  [keycloak-id]
  [:user.registered/keycloak-id => (s/coll-of ::specs/theme)]
  (db/query '[:find [(pull ?theme pattern)]
              :in $ ?keycloak-id pattern
              :where [?user :user.registered/keycloak-id ?keycloak-id]
              [?theme :theme/user ?user]]
            keycloak-id patterns/theme))

(>defn query-existing-theme
  "Query theme if exists."
  [keycloak-id {:theme/keys [title]}]
  [:user.registered/keycloak-id ::dto/theme => (? ::specs/theme)]
  (first
   (db/query '[:find [(pull ?theme pattern)]
               :in $ ?keycloak-id ?theme-title pattern
               :where [?user :user.registered/keycloak-id ?keycloak-id]
               [?theme :theme/user ?user]
               [?theme :theme/title ?theme-title]]
             keycloak-id title patterns/theme)))

(>defn new-theme
  "Saves the provided theme for a given user. Returns `nil` if there is already
  a title"
  [keycloak-id theme]
  [:user.registered/keycloak-id ::dto/theme => (? ::specs/theme)]
  (when-not (query-existing-theme keycloak-id theme)
    (let [temp-id (str "new-theme-" keycloak-id)
          new-theme (assoc theme
                           :theme/user [:user.registered/keycloak-id keycloak-id]
                           :db/id temp-id)]
      (db/transact-and-pull-temp
       [new-theme]
       temp-id patterns/theme))))

(comment

  (-> (themes-by-keycloak-id "d6d8a351-2074-46ff-aa9b-9c57ab6c6a18")
      first
      :theme.images/logo)

  (new-theme keycloak-id theme)
  (new-theme beta-keycloak-id theme)

  (themes-by-keycloak-id beta-keycloak-id)

  nil)
