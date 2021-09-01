(ns schnaq.interface.views.user.edit-notifications
  (:require [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user.settings :as settings]))

(defn- content []
  [pages/settings-panel
   (labels :user.settings/header)
   [:<>
    ]])

(defn view []
  [settings/user-view
   :user/edit-account
   [content]])
