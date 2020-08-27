(ns meetly.interface.views.common
  (:require [ghostwheel.core :refer [>defn]]
            ["jdenticon" :as jdenticon]))

(>defn avatar
  "Create an image based on the nickname."
  [display-name size]
  [string? number? :ret vector?]
  ;[:div.avatar.text-center
  [:div.d-flex.flex-row
   [:div.avatar-name.mr-4.align-self-end display-name]
   [:div.avatar-image.img-thumbnail.schnaq-rounded.align-self-end.p-0
    {:dangerouslySetInnerHTML {:__html (jdenticon/toSvg display-name size)}}]])