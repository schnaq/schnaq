(ns schnaq.interface.views.navbar.collapse-content
  (:require [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.navbar.elements :as nav-elements :refer [language-dropdown]]
            [schnaq.interface.views.navbar.user-management :as um]))

(defn- external-content [collapse-content-id content]
  [:div.collapse.navbar-collapse.bg-white.p-2.m-1.rounded-2.d-lg-none
   {:id collapse-content-id}
   content])

(defn- li-link-button
  [label href]
  [:a.list-group-item.list-group-item-action
   {:href href}
   (labels label)])

(defn collapsed-navbar
  "Collapsible content for schnaq overview. Used in the collapsed navbar."
  [collapse-content-id]
  [external-content collapse-content-id
   [:<>
    [um/register-or-user-button true]
    [:ul.list-group.list-group-flush
     [li-link-button :router/pricing "https://schnaq.com/pricing"]
     [li-link-button :router/privacy "https://schnaq.com/en/privacy"]
     [li-link-button :nav/blog "https://schnaq.com/blog/"]
     [:li.list-group-item.dropdown [language-dropdown true false {:class "p-0 text-dark"}]]]]])
