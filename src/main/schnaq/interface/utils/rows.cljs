(ns schnaq.interface.utils.rows
  (:require [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.common :as common]))

(defn build-text-box
  "Composing the text-part of a feature-row. Takes a `text-namespace` which
  looks up the corresponding text entries, which are then rendered."
  ([text-namespace]
   [build-text-box text-namespace nil])
  ([text-namespace more]
   (let [prepend-namespace (partial common/add-namespace-to-keyword text-namespace)]
     [:article.feature-text-box
      [:h3.h1.text-typography.mb-5 (labels (prepend-namespace :title))]
      [:section (labels (prepend-namespace :body))
       (when more
         more)]])))

(defn row-builder-text-right
  "Generic builder to align text and asset. Here, text is on the right
  and the remainder is on the left."
  [left right]
  [:div.row.feature-row
   [:div.col-12.col-lg-5.my-auto left]
   [:div.col-12.col-lg-6.offset-lg-1.my-auto right]])

(defn row-builder-text-left
  "Build a row, like the feature rows. Here, the text is on the left side."
  [left right]
  [:div.row.feature-row
   [:div.col-12.col-lg-6.my-auto left]
   [:div.col-12.col-lg-5.offset-lg-1.my-auto right]])
