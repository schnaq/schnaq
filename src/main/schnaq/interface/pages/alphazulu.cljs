(ns schnaq.interface.pages.alphazulu
  (:require [schnaq.interface.views.pages :as pages]))

(defn container
  []
  [pages/with-nav-and-header
   {:page/title "Alphazulu – Modern Work for Modern Companies"
    :page/heading "Alphazulu"}
   [:div.container "hi there dog"]])

(defn view
  "The alphazulu page showing off the cooperation."
  []
  [container])


