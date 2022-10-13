(ns schnaq.interface.views.navbar.for-pages
  (:require [clojure.string :as str]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.navbar :as navbar-components]
            [schnaq.interface.navigation :as navigation]
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

(defn navbar-transparent
  "Navbar definition for the default pages."
  [wrapper-classes]
  [:nav.navbar.navbar-expand-lg.py-3.navbar-transparent.bg-transparent.mb-4
   [:section
    {:class (if (str/blank? wrapper-classes) "container container-85" wrapper-classes)}
    [:a.navbar-brand {:href (navigation/href :routes.schnaqs/personal)}
     [:img.d-inline-block.align-middle.me-2
      {:src (img-path :logo-white) :width "150" :alt "schnaq logo"}]]
    ;; hamburger
    [:button.navbar-toggler
     {:type "button" :data-bs-toggle "collapse" :data-bs-target "#schnaq-navbar"
      :aria-controls "schnaq-navbar" :aria-expanded "false" :aria-label "Toggle navigation"
      :data-html2canvas-ignore true}
     [:span.navbar-toggler-icon]]
    ;; menu items
    [:div#schnaq-navbar.collapse.navbar-collapse
     [:ul.navbar-nav.ms-auto
      [:li.nav-item [navbar-components/button :nav/schnaqs (toolbelt/current-overview-link)]]
      [:li.nav-item [pricing-button]]
      [:li.nav-item [privacy-button]]
      [:li.nav-item [blog-link]]
      [:li.nav-item.dropdown
       [language-dropdown]]]
     [um/admin-dropdown]
     [um/register-or-user-button false]]]])
