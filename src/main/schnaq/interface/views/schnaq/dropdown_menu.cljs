(ns schnaq.interface.views.schnaq.dropdown-menu
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]))

(defn item
  "Dropdown item element to be used inside a dropdown menu."
  [icon-label label on-click-fn icon-class]
  (let [icon-classes (or icon-class "my-auto me-1")]
    [:button.dropdown-item
     {:on-click on-click-fn
      :title (labels label)}
     [icon icon-label icon-classes] (labels label)]))

(defn moderator
  "Dropdown menu for moderator elements (Polls, Activation, Wordcloud)."
  [{:keys [id] :as attributes} dropdown-menu-content]
  (let [current-edit-hash @(rf/subscribe [:schnaq.current/admin-access])
        pro-user? @(rf/subscribe [:user/pro-user?])]
    (when (and pro-user? current-edit-hash)
      [:div.dropdown.mx-2
       [:button.btn.m-0.p-0
        (merge
         {:role "button" :data-bs-toggle "dropdown"
          :aria-haspopup "true" :aria-expanded "false"}
         attributes)
        [icon :dots]]
       [:div.dropdown-menu.dropdown-menu-end {:aria-labelledby id}
        dropdown-menu-content]])))
