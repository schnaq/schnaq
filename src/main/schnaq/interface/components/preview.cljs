(ns schnaq.interface.components.preview
  (:require [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.translations :refer [labels]]))

(defn preview-image
  "Show an image with a text displayed on it that the user needs to buy schnaq."
  [img-key]
  [:div.preview-image
   [:img.img-fluid {:src (img-path img-key) :alt "Blurred background image"}]
   [:a {:href "https://schnaq.com/pricing"}
    [:div.alert.alert-primary
     [:p.mb-1 [:small.fw-bold
               (labels :preview.image-overlay/title)]]
     [:p.mb-0 [:small (labels :preview.image-overlay/body)]]]]])
