(ns schnaq.interface.views.user.edit-account
  (:require [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.user.elements :as elements]
            [schnaq.interface.views.pages :as pages]))


(defn- content []
  (let [user nil]
    [pages/with-nav
     {:page/heading (labels :user/edit-account)}
     [elements/user-view-desktop
      user
      [:div "fooo"]]]))

(defn view []
  [content])