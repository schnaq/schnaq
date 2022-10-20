(ns schnaq.interface.views.navbar.for-pages
  (:require ["react-bootstrap/Nav" :as Nav]
            [com.fulcrologic.guardrails.core :refer [>defn-]]
            [oops.core :refer [oget]]
            [schnaq.interface.components.navbar :as navbar-components]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.navbar.collapse-content :as collapse-content]
            [schnaq.interface.views.navbar.elements :as elements :refer [language-dropdown]]
            [schnaq.interface.views.navbar.user-management :as um]))

(def ^:private NavLink (oget Nav :Link))

;; -----------------------------------------------------------------------------
;; Navbar Elements

(>defn- button
  "Build a button for the navbar. Takes a label as a keyword and anything, which
  can be passed to an anchor's href."
  [label href]
  [keyword? any? :ret vector?]
  [:> NavLink {:href href :className "text-nowrap"}
   (labels label)])

(defn- schnaqs-button []
  [button :nav/schnaqs (toolbelt/current-overview-link)])

(defn- blog-link []
  [button :nav/blog "https://schnaq.com/blog/"])

(defn- pricing-button []
  [button :router/pricing "https://schnaq.com/pricing"])

(defn- privacy-button []
  [button :router/privacy "https://schnaq.com/privacy"])

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
    [navbar-components/discussion-navbar
     [elements/navbar-title]
     navbar-content-id
     "navbar-bg-transparent-sm-white"
     [navbar-user]
     [collapse-content/collapsed-navbar navbar-content-id]]))
