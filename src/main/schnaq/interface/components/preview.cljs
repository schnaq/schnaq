(ns schnaq.interface.components.preview
  (:require [schnaq.interface.components.images :refer [img-path]]))

(defn preview-image
  "Show an image with a text displayed on it that the user needs to buy schnaq."
  [img-key]
  [:div.preview-image {:style {:position :relative}}
   [:img.img-fluid {:src (img-path img-key)}]
   [:div.alert.alert-primary
    [:p [:strong "Dies ist eine Pro-Funktion."]]
    [:p.mb-0 [:small "Um sie nutzen zu können, benötigst du einen Pro- oder Beta-Zugang."]]]])