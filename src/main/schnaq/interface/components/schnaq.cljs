(ns schnaq.interface.components.schnaq
  (:require [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]))

(defn access-code
  "Component to add leading zeros and a padding between access code blocks."
  []
  (let [access-code @(rf/subscribe [:schnaq.selected/access-code])
        code-length shared-config/access-code-length
        padded-access-code (.padStart (str access-code) code-length "0")]
    [:span
     (subs padded-access-code 0 (/ code-length 2)) [:span.pl-2]
     (subs padded-access-code (/ code-length 2))]))
