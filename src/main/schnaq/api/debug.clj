(ns schnaq.api.debug
  (:require [schnaq.config.shared :as shared-config]))

(def debug-routes
  [(when-not shared-config/production?
     ["" {:swagger {:tags ["debug"]}}
      ["/debug/headers" {:get identity}]])])