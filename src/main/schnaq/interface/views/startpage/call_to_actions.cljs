(ns schnaq.interface.views.startpage.call-to-actions
  (:require [schnaq.interface.text.display-data :refer [labels fa]]))

(defn bullet-point
  "Display a bullet-point with a leading icon.
  _call-to-action.scss contains the related styling."
  [icon-label desc-label]
  [:div.row.py-3
   [:div.col-1
    [:i.icon-points-icon {:class (str "fas fa-2x " (fa icon-label))}]]
   [:div.col-10
    [:span.icon-points-text (labels desc-label)]]])