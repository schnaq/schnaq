(ns meetly.interface.views.notifications
  (:require [goog.dom :as gdom]))

(defn toast [title body]
  [:div.toast {:role "alert" :aria-live "assertive" :aria-atomic "true"}
   [:div.toast-header
    [:strong.mr-auto title]
    [:small.text-muted "11 mins ago"]
    [:button.ml-2.mb-1.close {:type "button" :data-dismiss "toast" :aria-label "Close"}
     [:span {:aria-hidden "true"} "&times;"]]]
   [:div.toast-body body]])

(comment

  :end)

(defn view []
  (.toast (js/$ ".toast"))
  [:h1 (toast "foo" "bar")])