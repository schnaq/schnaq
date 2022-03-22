(ns schnaq.interface.code-of-conduct
  (:require [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.rows :as rows]
            [schnaq.interface.views.pages :as pages]))

(defn- content []
  (let [header
        {:page/heading (labels :coc/heading)
         :page/subheading (labels :coc/subheading)
         :page/vertical-header? true}]
    [pages/with-nav-and-header
     header
     [:section.container
      [rows/image-left :schnaqqifant/hippie :schnaqqifant/hippie-alt-text :coc.users]
      [rows/image-right :schnaqqifant/police :schnaqqifant/police-alt-text :coc.content]]]))

(defn view
  "A view containing the code of conduct"
  []
  [content])
