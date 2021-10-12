(ns schnaq.interface.components.schnaq
  (:require [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.clipboard :as clipboard]
            [schnaq.interface.views.notifications :refer [notify!]]))

(defn access-code
  "Component to add leading zeros and a padding between access code blocks."
  [options]
  (let [access-code @(rf/subscribe [:schnaq.selected/access-code])
        code-length shared-config/access-code-length
        padded-access-code (.padStart (str access-code) code-length "0")]
    [:span.clickable
     (merge {:on-click (fn []
                         (clipboard/copy-to-clipboard! access-code)
                         (notify! (labels :schnaq.access-code.clipboard/header)
                                  (labels :schnaq.access-code.clipboard/body)
                                  :info
                                  false))}
            options)
     (subs padded-access-code 0 (/ code-length 2)) [:span.pl-3]
     (subs padded-access-code (/ code-length 2))]))
