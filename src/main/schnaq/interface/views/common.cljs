(ns schnaq.interface.views.common
  (:require [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            ["jdenticon" :as jdenticon]))

(>defn avatar
  "Create an image based on the nickname."
  [display-name size]
  [string? number? :ret vector?]
  [:div.d-flex.flex-row
   [:div.avatar-name.mr-4.align-self-end display-name]
   [:div.avatar-image.img-thumbnail.schnaq-rounded.align-self-end.p-0
    {:dangerouslySetInnerHTML {:__html (jdenticon/toSvg display-name size)}}]])

(>defn add-namespace-to-keyword
  "Prepend a namespace to a keyword. Replaces existing namespace with new
  namespace."
  [prepend-namespace to-keyword]
  [(s/or :keyword keyword? :string string?) keyword? :ret keyword?]
  (keyword (str (name prepend-namespace) "/" (name to-keyword))))
