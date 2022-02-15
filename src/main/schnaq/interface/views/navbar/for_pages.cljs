(ns schnaq.interface.views.navbar.for-pages
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.navbar :as navbar-components]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.navbar.collapse-content :as collapse-content]
            [schnaq.interface.views.navbar.elements :as elements]
            [schnaq.interface.views.navbar.user-management :as um]))

;; -----------------------------------------------------------------------------
;; Navbar Elements

(defn- blog-link []
  [navbar-components/button :nav/blog "https://schnaq.com/blog/"])

(defn- pricing-button []
  [navbar-components/button :router/pricing (reitfe/href :routes/pricing)])

(defn- product-button []
  [navbar-components/product-dropdown-button])

(defn- privacy-button []
  [navbar-components/button :router/privacy (reitfe/href :routes/privacy)])

;; -----------------------------------------------------------------------------

(defn- navbar-user []
  [:div.d-flex.schnaq-navbar.align-items-center.px-3
   [pricing-button]
   [privacy-button]
   [blog-link]
   [:div.dropdown.ms-auto
    [navbar-components/language-dropdown false {}]]
   (when @(rf/subscribe [:user/administrator?])
     [um/admin-dropdown "btn-outline-secondary"])
   [:div.mx-1.d-none.d-md-block
    [:div.d-flex.flex-row.align-items-center
     [um/register-or-user-button "btn-link"]]]])

(defn navbar
  "Overview header for a discussion."
  [title]
  (let [navbar-content-id "Overview-Content"
        navbar-title (toolbelt/truncate-to-n-chars title 20)]
    [navbar-components/collapsible-nav-bar
     [elements/navbar-title
      [:h1.h6.fw-bold.my-auto.text-dark navbar-title]]
     navbar-content-id
     "navbar-bg-transparent-sm-white"
     [navbar-user]
     [collapse-content/navbar-external-overview-content navbar-content-id]]))

(defn navbar-transparent
  "Navbar definition for the default pages."
  [wrapper-classes]
  [:nav.navbar.navbar-expand-lg.py-3.navbar-transparent.bg-transparent.mb-4
   [:section
    {:class (if (str/blank? wrapper-classes) "container-fluid" wrapper-classes)}
    [:a.navbar-brand {:href (reitfe/href :routes/startpage)}
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
      [:li.nav-item [product-button]]
      [:li.nav-item [pricing-button]]
      [:li.nav-item [privacy-button]]
      [:li.nav-item [blog-link]]
      [:li.nav-item.dropdown
       [navbar-components/language-dropdown]]]
     [um/admin-dropdown "btn-outline-secondary"]
     [um/register-or-user-button "btn-outline-light btn-transparent"]]]])
