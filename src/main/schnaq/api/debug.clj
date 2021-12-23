(ns schnaq.api.debug
  (:require [ring.util.http-response :refer [ok]]
            [schnaq.config.shared :as shared-config]))

(defn- reveal-information [request]
  (ok {:headers (:headers request)
       :identity (:identity request)}))

;; -----------------------------------------------------------------------------

(def debug-routes
  [(when-not shared-config/production?
     ["/debug" {:swagger {:tags ["debug"]}
                :middleware [:security/schnaq-csrf-header]}
      ["" {:get reveal-information
           :post reveal-information}]])])
