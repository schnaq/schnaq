(ns schnaq.interface.components.navbar
  (:require ["react-bootstrap/Container" :as Container]
            ["react-bootstrap/Nav" :as Nav]
            ["react-bootstrap/Navbar" :as Navbar]
            ["react-bootstrap/NavDropdown" :as NavDropdown]
            [com.fulcrologic.guardrails.core :refer [=> >defn ?]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.common :as common-components :refer [schnaqqi-white]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.navbar.user-management :refer [UserNavLinkDropdown]]))

(def ^:private NavbarBrand (oget Navbar :Brand))
(def ^:private NavbarText (oget Navbar :Text))
(def ^:private NavbarToggle (oget Navbar :Toggle))
(def ^:private NavbarCollapse (oget Navbar :Collapse))
(def ^:private NavLink (oget Nav :Link))
(def ^:private NavDropdownItem (oget NavDropdown :Item))

(>defn button
  "Build a button for the navbar. Takes a label as a keyword and anything, which
  can be passed to an anchor's href."
  [label href]
  [keyword? any? :ret vector?]
  [:a.nav-link.text-nowrap.btn.btn-link {:href href :role "button"}
   (labels label)])

(defn MobileNav []
  [:> Navbar {:bg :primary :variant :dark :expand :xl}
   [:> Container {:fluid true}
    [:> NavbarBrand {:href "#"}
     [schnaqqi-white {:class "img-fluid" :width "50"}]]
    [:> NavbarText {:class "opacity-100 text-white"} @(rf/subscribe [:schnaq/title])]
    [:> NavbarToggle {:aria-controls :basic-navbar-nav}]
    [:> NavbarCollapse {:id "basic-navbar-nav"}
     [:> Nav {:class "me-auto"}
      [:> NavLink {:href "#home"} "Home"]
      [UserNavLinkDropdown]]]]])

(defn collapsible-navbar
  "Collapsible navbar with split content header, collapsible-content-id must match id of collapsible-content."
  [brand-content collapse-content-id navbar-bg-class top-right-content collapsible-content]
  [:<>
   [MobileNav]
   (when-not @(rf/subscribe [:ui/setting :hide-navbar])
     [:<>
      [:nav.navbar.navbar-expand-lg.navbar-light.schnaq-navbar-dynamic-padding
       {:class navbar-bg-class}
       [:div.container-fluid
        [:div.navbar-brand.pt-0 brand-content]
        [:button.navbar-toggler.mx-2.panel-white
         {:type "button" :data-bs-toggle "collapse"
          :data-bs-target (str "#" collapse-content-id)
          :aria-controls collapse-content-id
          :aria-expanded "false"
          :aria-label "Toggle navigation"}
         [:span.navbar-toggler-icon]]
        [:div.d-md-none [common-components/theme-logo {:style {:max-width "100px"}}]]
        [:div.ms-auto.d-none.d-lg-block
         top-right-content]]]
      collapsible-content])])
