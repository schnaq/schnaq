(ns schnaq.interface.components.buttons
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
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

(defn upgrade
  "Upgrade button for use in different places."
  []
  (when-not @(rf/subscribe [:user/pro?])
    [anchor
     [:<>
      [icon :star "me-1"]
      (labels :pricing.upgrade-nudge/button)]
     "https://schnaq.com/pricing"
     "btn-secondary"]))
