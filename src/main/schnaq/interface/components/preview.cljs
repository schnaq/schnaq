(ns schnaq.interface.components.preview
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.translations :refer [labels]]))

(defn preview-image
  "Show an image with a text displayed on it that the user needs to buy schnaq."
  [img-key]
  [:div.preview-image
   [:img.img-fluid {:src (img-path img-key)}]
   [:div.alert.alert-primary {:on-click #(rf/dispatch [:navigation/navigate :routes/pricing])}
    [:p.mb-1 [:small.fw-bold
              (labels :preview.image-overlay/title)]]
    [:p.mb-0 [:small (labels :preview.image-overlay/body)]]]])
