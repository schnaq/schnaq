(ns schnaq.database.access-codes
  (:require [ghostwheel.core :refer [>defn-]]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.specs :as specs]))

(>defn- generate-access-code
  "Generates an access code of a specific length. Generates integers from
   [0, 9]."
  []
  [:ret ::specs/access-code]
  (map (fn [_] (rand-int 10)) (range shared-config/access-code-length)))
