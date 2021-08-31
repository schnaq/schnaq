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
     :alphazulu.wetog]
    [rows/image-left
     :alphazulu.xignsys/logo
     :alphazulu.xignsys]
    [rows/image-right
     :alphazulu.cobago/logo
     :alphazulu.cobago]
    [rows/image-left
     :alphazulu.trustcerts/logo
     :alphazulu.trustcerts]
    [rows/image-right
     :alphazulu.ec3l/logo
     :alphazulu.ec3l]
    [rows/image-left
     :team/at-table-with-laptop
     :alphazulu.activate]]])

(defn view
  "The alphazulu page showing off the cooperation."
  []
  [container])


