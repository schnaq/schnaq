(ns schnaq.interface.components.navbar
  (:require ["react-bootstrap/Container" :as Container]
            ["react-bootstrap/Nav" :as Nav]
            ["react-bootstrap/Navbar" :as Navbar]
            ["react-bootstrap/NavDropdown" :as NavDropdown]
            [com.fulcrologic.guardrails.core :refer [=> >defn]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.common :as common-components :refer [schnaqqi-white]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.navigation :as navigation]
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

(defn DiscussionViews
  "Toggle between different views in a discussion."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])
        href #(navigation/href % {:share-hash share-hash})
        img (fn [img-key] [:img {:height 25
                                 :class "navbar-icon"
                                 :src (img-path img-key)}])]
    [:<>
     [:> NavLink {:disabled true} (labels :discussion.navbar/views)]
     [:> NavLink {:class "ms-3" :href (href :routes.schnaq/start)}
      [img :icon-cards-dark] (labels :discussion.button/text)]
     [:> NavLink {:class "ms-3" :href (href :routes/graph-view)}
      [img :icon-graph-dark] (labels :graph.button/text)]
     [:> NavLink {:class "ms-3" :href (href :routes.schnaq/qanda)}
      [img :icon-qanda-dark] (labels :qanda.button/text)]
     [:> NavLink {:class "ms-3" :href (href :routes.schnaq/dashboard)}
      [img :icon-summary-dark] (labels :summary.link.button/text)]]))

(defn SchnaqSettings []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])
        href #(navigation/href % {:share-hash share-hash})
        img (fn [img-key] [:img {:height 25
                                 :class "navbar-icon"
                                 :src (img-path img-key)}])]
    [:<>
     [:> NavLink {:disabled true} (labels :discussion.navbar/settings)]
     [:> NavLink {:class "ms-3" :href (href :routes.schnaq/start)}
      [img :icon-cards-dark] (labels :sharing/tooltip)]
     [:> NavLink {:class "ms-3" :href (href :routes/graph-view)}
      [img :icon-graph-dark] (labels :graph.button/text)]
     [:> NavLink {:class "ms-3" :href (href :routes.schnaq/qanda)}
      [img :icon-qanda-dark] (labels :qanda.button/text)]
     [:> NavLink {:class "ms-3" :href (href :routes.schnaq/dashboard)}
      [img :icon-summary-dark] (labels :summary.link.button/text)]]))

(defn MobileNav []
  [:> Navbar {:bg :primary :variant :dark :expand :xl :expanded true}
   [:> Container {:fluid true}
    [:> NavbarBrand {:href "#"}
     [schnaqqi-white {:class "img-fluid" :width "50"}]]
    [:> NavbarText @(rf/subscribe [:schnaq/title])]
    [:> NavbarToggle {:aria-controls "mobile-navbar"}]
    [:> NavbarCollapse {:id "mobile-navbar"}
     [:> Nav
      [UserNavLinkDropdown]
      [:div.row
       [:div.col-6 [DiscussionViews]]
       [:div.col-6 [SchnaqSettings]]]]]]])

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
