(ns meetly.meeting.interface.views.common
  (:require [ghostwheel.core :refer [>defn]]
            ["jdenticon" :as jdenticon]))

(>defn avatar
  "Create an image based on the nickname."
  [display-name size]
  [string? number? :ret vector?]
  [:div.avatar.text-center
   [:div.avatar-image.img-thumbnail.rounded-circle
    {:dangerouslySetInnerHTML {:__html (jdenticon/toSvg display-name size)}}]
   [:div.avatar-name display-name]])