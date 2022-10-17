(ns schnaq.interface.views.navbar.for-pages
  (:require [schnaq.interface.components.navbar :as navbar-components]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.navbar.collapse-content :as collapse-content]
            [schnaq.interface.views.navbar.elements :as elements :refer [language-dropdown]]
            [schnaq.interface.views.navbar.user-management :as um]))

;; -----------------------------------------------------------------------------
;; Navbar Elements

(defn- schnaqs-button []
  [navbar-components/button :nav/schnaqs (toolbelt/current-overview-link)])

(defn- blog-link []
  [navbar-components/button :nav/blog "https://schnaq.com/blog/"])

(defn- pricing-button []
  [navbar-components/button :router/pricing "https://schnaq.com/pricing"])

(defn- privacy-button []
  [navbar-components/button :router/privacy "https://schnaq.com/privacy"])

;; -----------------------------------------------------------------------------

(defn- navbar-user []
  [:div.d-flex.schnaq-navbar.align-items-center.px-3
   [schnaqs-button]
   [pricing-button]
   [privacy-button]
   [blog-link]
   [:div.nav-item.dropdown.ms-auto
    [language-dropdown false false {}]]
   [um/admin-dropdown]
   [:div.mx-1.d-none.d-md-block
    [:div.d-flex.flex-row.align-items-center
     [um/register-or-user-button true]]]])

(defn navbar
  "Overview header for a discussion."
  []
  (let [navbar-content-id "Overview-Content"]
    [navbar-components/collapsible-navbar
     [elements/navbar-title]
     navbar-content-id
     "navbar-bg-transparent-sm-white"
     [navbar-user]
     [collapse-content/collapsed-navbar navbar-content-id]]))
