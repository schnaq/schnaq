(ns schnaq.interface.pages.privacy-extended
  "Page explaining our privacy and how we are storing data."
  (:require [schnaq.interface.pages.privacy :as privacy]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.pages :as pages]))

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

(defn- intro []
  [privacy-entry :privacy.extended.intro])

(defn- logfiles []
  [privacy-entry :privacy.extended.logfiles])

(defn- cookies []
  [privacy-entry :privacy.extended.cookies [privacy/open-privacy-settings]])

(defn- personal-data []
  [privacy-entry :privacy.extended.personal-data])

(defn- matomo []
  [privacy-entry :privacy.extended.matomo [privacy/open-privacy-settings]])

(defn- rights-of-the-affected []
  [privacy-entry :privacy.extended.rights-of-the-affected])

(defn- right-to-complain []
  [privacy-entry :privacy.extended.right-to-complain])

(defn- hosting []
  [privacy-entry :privacy.extended.hosting])

(defn responsible []
  [privacy-entry :privacy.extended.responsible])

;; -----------------------------------------------------------------------------

(defn- page []
  [pages/with-nav-and-header
   {:page/heading (labels :privacy.extended/heading)
    :page/subheading (labels :privacy.extended/subheading)}
   [:section.container
    [responsible]
    [intro]
    [logfiles]
    [cookies]
    [personal-data]
    [matomo]
    [rights-of-the-affected]
    [right-to-complain]
    [hosting]]])

(defn view []
  [page])