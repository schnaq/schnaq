(ns schnaq.interface.code-of-conduct
  (:require [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.rows :as rows]
            [schnaq.interface.views.pages :as pages]))

(defn- content []
  (let [header
        {:page/heading (labels :coc/heading)
         :page/more-for-heading (labels :coc/subheading)}]
    [pages/with-nav-and-header
     header
     [:<>
      [:section.container
       [rows/image-left
        :schnaqqifant/hippie
        :coc.users]
       [rows/image-right
        :schnaqqifant/police
        :coc.content]]]]))

(defn view
  "A view containing the code of conduct"
  []
  [content])