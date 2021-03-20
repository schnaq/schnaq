(ns schnaq.database.hub
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.meeting.database :refer [transact] :as main-db]
            [schnaq.meeting.specs :as specs]
            [schnaq.toolbelt :as toolbelt]
            [taoensso.timbre :as log]))

(def ^:private hub-pattern
  [:db/id
   :hub/name
   :hub/keycloak-name
   {:hub/schnaqs discussion-db/discussion-pattern}])

(defn- pull-hub
  "Pull a hub from the database and pull db/ident up."
  [hub-query]
  (toolbelt/pull-key-up
    (main-db/fast-pull hub-query hub-pattern)
    :db/ident))

(>defn create-hub
  "Create a hub and reference it to the keycloak-name."
  [hub-name keycloak-name]
  [:hub/name :hub/keycloak-name :ret ::specs/hub]
  (let [new-hub (get-in
                  (transact [{:db/id "temp"
                              :hub/name hub-name
                              :hub/keycloak-name keycloak-name}])
                  [:tempids "temp"])]
    (log/info "Created hub" new-hub)
    (pull-hub new-hub)))

(>defn add-discussions-to-hub
  [hub-id discussion-ids]
  [:db/id (s/coll-of :db/id) :ret ::specs/hub]
  (transact (mapv #(vector :db/add hub-id :hub/schnaqs %) discussion-ids))
  (log/info "Added schnaqs with ids" discussion-ids "to hub" hub-id)
  (pull-hub hub-id))

(>defn hub-by-keycloak-name
  "Return a hub by the reference in keycloak."
  [keycloak-name]
  [string? :ret ::specs/hub]
  (pull-hub [:hub/keycloak-name keycloak-name]))
