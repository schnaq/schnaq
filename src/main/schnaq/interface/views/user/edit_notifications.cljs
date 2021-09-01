(ns schnaq.interface.views.user.edit-notifications
  (:require [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user.settings :as settings]))

(defn set-all-to-read []
  [:div.py-5
   [:div.text-center.mt-5.mb-3
    [:button.btn.btn-secondary (labels :user.notifications.set-all-to-read/button)]]
   [:small.text-muted (labels :user.notifications.set-all-to-read/info)]])

(defn change-interval-drop-down []
  [:div.dropdown
   [:button#dropdownMailInterval.btn.btn-outline-dark.dropdown-toggle
    {:type "button" :data-toggle "dropdown" :aria-haspopup "true" :aria-expanded "false"}
    "Dropdown button"]
   [:div.dropdown-menu {:aria-labelledby "dropdownMailInterval"}
    [:a.dropdown-item {:href "#"} (labels :user.notifications.interval/daily)]
    [:a.dropdown-item {:href "#"} (labels :user.notifications.interval/weekly)]
    [:div.dropdown-divider]
    [:a.dropdown-item {:href "#"} (labels :user.notifications.interval/never)]]])

(defn change-update-mail-interval []
  [:<>
   [:div.row.mt-5.mb-3
    [:div.col
     [:h5.text-muted (labels :user.notifications/mails)]]
    [:div.col.text-right
     [change-interval-drop-down]]]
   [:small.text-muted (labels :user.notifications/info)]])

(defn- content []
  [pages/settings-panel
   (labels :user.notifications/header)
   [:<>
    [change-update-mail-interval]
    [set-all-to-read]]])

(defn view []
  [settings/user-view
   :user/edit-account
   [content]])
