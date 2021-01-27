(ns schnaq.interface.views.admin.control-center
  (:require [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.pages :as pages]))

(defn- center-overview
  "The startpage of the admin center."
  []
  [pages/with-nav-and-header
   {:page/title (labels :admin.center.start/title)
    :page/heading (labels :admin.center.start/heading)
    :page/subheading (labels :admin.center.start/subheading)}
   [:div.container "hi"]])

(defn center-overview-route
  []
  [center-overview])