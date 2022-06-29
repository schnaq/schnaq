(ns schnaq.interface.pages.privacy-extended
  "Page explaining our privacy and how we are storing data."
  (:require [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.common :as common]))

(defn- privacy-entry
  "Define a privacy entry."
  ([text-namespace]
   (privacy-entry text-namespace nil))
  ([text-namespace additional-component]
   (let [prepend-namespace (partial common/add-namespace-to-keyword text-namespace)]
     [:section {:style {:text-align :justify
                        :hyphens :auto}}
      [:h3.pt-4 (labels (prepend-namespace :title))]
      (labels (prepend-namespace :body))
      additional-component])))

(defn responsible []
  [privacy-entry :privacy.extended.responsible])

;; ----------------------------------------------------------------------------
