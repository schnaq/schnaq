(ns schnaq.api.debug
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [schnaq.config.shared :as shared-config]))

(defn- reveal-information [request]
  (ok {:headers (:headers request)
       :identity (:identity request)}))

;; -----------------------------------------------------------------------------

(s/def ::debug string?)

(def debug-routes
  [(when-not shared-config/production?
     ["/debug" {:swagger {:tags ["debug"]}
                :parameters {:query (s/keys :opt-un [::debug])}}
      ["" {:name :api/debug
           :get {:handler reveal-information}
           :post {:handler reveal-information}}]])])
