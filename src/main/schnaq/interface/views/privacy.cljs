(ns schnaq.interface.views.privacy
  "Page explaining our privacy and how we are storing data."
  (:require [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.rows :as rows]
            [schnaq.interface.views.pages :as pages]))

(defn- dsgvo-row []
  [rows/icon-right
   [:i {:class (str "m-auto fas fa-lg " (fa :shield))}]
   :privacy.made-in-germany])

(defn- personal-data-row []
  [rows/icon-left
   [:i {:class (str "m-auto fas fa-lg " (fa :user/lock))}]
   :privacy.personal-data])

(defn- page []
  (pages/with-nav-and-header
    {:page/heading (labels :privacy/heading)
     :page/subheading (labels :privacy/subheading)}
    [:section.container
     [dsgvo-row]
     [personal-data-row]]))

(defn view []
  [page])