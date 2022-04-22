(ns schnaq.interface.components.buttons
  (:require [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]))

(defn anchor
  "Create a `a`-Tag styled as a button. By default, in primary colors."
  ([content]
   [anchor content "#"])
  ([content target]
   [anchor content target "btn-primary"])
  ([content target classes]
   [anchor content target classes nil])
  ([content target classes attrs]
   [:a.btn
    (cond->
      {:href target
       :role "button"
       :class classes}
      attrs (merge attrs))
    content]))

(defn anchor-big
  "The default big button as an anchor. By default, in primary colors. No target on default."
  ([content]
   [anchor-big content "#"])
  ([content target]
   [anchor-big content target "btn-primary"])
  ([content target classes]
   [anchor-big content target classes nil])
  ([content target classes attrs]
   [anchor content target (str "btn-lg " classes) attrs]))

(defn button
  "Create a `button`-Tag styled. By default, styled in primary colors."
  ([content]
   [button content nil])
  ([content on-click]
   [button content on-click "btn-primary"])
  ([content on-click classes]
   [button content on-click classes nil])
  ([content on-click classes attrs]
   [:button.btn
    (cond->
      {:on-click on-click
       :role "button"
       :class classes}
      attrs (merge attrs))
    content]))

(defn upgrade-large
  "Large upgrade button for use in different places."
  []
  [:a.btn.btn-secondary.btn-lg.fs-2
   {:href (navigation/href :routes/pricing)}
   [icon :star] (labels :pricing.upgrade-nudge/button)])
