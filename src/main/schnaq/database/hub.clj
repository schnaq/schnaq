(ns schnaq.database.hub
  (:require [datomic.client.api :as d]
            [ghostwheel.core :refer [>defn ? >defn-]]
            [schnaq.meeting.database :refer [transact new-connection query] :as main-db]
            [schnaq.meeting.specs :as specs]
            [taoensso.timbre :as log]))

(def hub-pattern
  [:db/id
   :hub/name
   {:hub/schnaqs [:discussion/title]}])

(>defn create-hub
  [hub-name]
  [:hub/name :ret ::specs/hub]
  (transact [{:hub/name hub-name}]))

(comment
  (create-hub "pornhub")
  (query
    '[:find (pull ?hubs hub-pattern)
      :in $ hub-pattern
      :where [?hubs :hub/name _]]
    hub-pattern)
  )
