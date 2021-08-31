(ns schnaq.interface.pages.alphazulu
  (:require [schnaq.interface.utils.rows :as rows]
            [schnaq.interface.views.pages :as pages]))

(defn container
  []
  [pages/with-nav-and-header
   {:page/title "Alphazulu – Modern Work for Modern Companies"
    :page/heading "Alphazulu"
    :page/vertical-header? true}
   [:section.container
    [rows/image-right
     :alphazulu/logo
     :alphazulu.introduction]
    [rows/image-left
     :logo
     :alphazulu.schnaq]
    [rows/image-right
     :alphazulu.wetog/logo
     :alphazulu.wetog]]])

(defn view
  "The alphazulu page showing off the cooperation."
  []
  [container])


