(ns schnaq.auth.lib
  (:require [com.fulcrologic.guardrails.core :refer [>defn]]))

(>defn prepare-identity-map
  "Extend identity map parsed from JWT and convert types."
  [request]
  [map? :ret map?]
  (-> request
      (update-in [:identity :sub] str)
      (assoc-in [:identity :id] (str (get-in request [:identity :sub])))
      (assoc-in [:identity :preferred_username] (or (get-in request [:identity :preferred_username])
                                                    (get-in request [:identity :name])))
      (assoc-in [:identity :roles] (get-in request [:identity :realm_access :roles]))))
