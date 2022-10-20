(ns schnaq.interface.components.navbar
  (:require ["react-bootstrap/Button" :as Button]
            ["react-bootstrap/ButtonGroup" :as ButtonGroup]
            ["react-bootstrap/Container" :as Container]
            ["react-bootstrap/Nav" :as Nav]
            ["react-bootstrap/Navbar" :as Navbar]
            [com.fulcrologic.guardrails.core :refer [=> >defn]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.common :as common-components :refer [schnaq-logo-white schnaqqi-white]]
            [schnaq.interface.components.icons :refer [icon stacked-icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.share :refer [share-schnaq-modal]]
            [schnaq.interface.views.navbar.elements :refer [LanguageDropdown
                                                            txt-export-request]]
            [schnaq.interface.views.navbar.user-management :refer [admin-dropdown
                                                                   user-navlink-dropdown]]))

(def ^:private NavbarBrand (oget Navbar :Brand))
(def ^:private NavbarText (oget Navbar :Text))
(def ^:private NavbarToggle (oget Navbar :Toggle))
(def ^:private NavbarCollapse (oget Navbar :Collapse))
(def ^:private NavLink (oget Nav :Link))

(>defn button
  "Build a button for the navbar. Takes a label as a keyword and anything, which
  can be passed to an anchor's href."
  [label href]
  [keyword? any? :ret vector?]
  [:> NavLink {:href href :className "text-nowrap"}
   (labels label)])

(defn- upgrade-button
  "Show an upgrade button for non-pro users."
  [& {:keys [props vertical?]}]
  (when (and @(rf/subscribe [:user/authenticated?])
             (not @(rf/subscribe [:user/pro?])))
    [:> NavLink (merge {:bsPrefix "btn btn-outline-secondary"
                        :on-click #(rf/dispatch [:navigation.redirect/follow {:redirect "https://schnaq.com/pricing"}])}
                       props)
     [icon :star (if vertical? "d-block mx-auto" "me-1") {:size :sm}]
     (labels :pricing.upgrade-nudge/button)]))

(defn common-navigation-links
  "Show default navigation links."
  [& {:keys [props vertical? hide-icon?]}]
  [:<>
   [:> NavLink (merge {:href (toolbelt/current-overview-link)} props)
    (when-not hide-icon? [stacked-icon :vertical? vertical? :icon-key :comments])
    (labels :nav/schnaqs)]
   [:> NavLink (merge {:href "https://schnaq.com/pricing"} props)
    (when-not hide-icon? [stacked-icon :vertical? vertical? :icon-key :award])
    (labels :router/pricing)]
   [:> NavLink (merge {:href "https://schnaq.com/privacy"} props)
    (when-not hide-icon? [stacked-icon :vertical? vertical? :icon-key :lock])
    (labels :router/privacy)]
   [:> NavLink (merge {:href "https://schnaq.com/blog/"} props)
    (when-not hide-icon? [stacked-icon :vertical? vertical? :icon-key :newspaper])
    (labels :nav/blog)]])

(def ^:private discussion-views
  {:routes.schnaq/start {:icon :icon-cards-dark
                         :label :discussion.button/text}
   :routes/graph-view {:icon :icon-graph-dark
                       :label :graph.button/text}
   :routes.schnaq/qanda {:icon :icon-qanda-dark
                         :label :qanda.button/text}
   :routes.schnaq/dashboard {:icon :icon-summary-dark
                             :label :summary.link.button/text}})

(defn links-to-discussion-views
  "Toggle between different views in a discussion."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])
        href #(navigation/href % {:share-hash share-hash})
        img (fn [img-key] [:img {:height 25
                                 :class "navbar-icon"
                                 :src (img-path img-key)}])]
    [:<>
     [:> NavLink {:disabled true} (labels :discussion.navbar/views)]
     (doall
      (for [[route {:keys [icon label]}] discussion-views]
        [:> NavLink {:key (str "discussion-view-element-" route)
                     :class "ms-3" :href (href route)}
         [img icon] (labels label)]))]))

(defn active-button? [current-route asked-route]
  (if (= asked-route :routes.schnaq/start)
    (or (= current-route asked-route) (= current-route :routes.schnaq.select/statement))
    (= current-route asked-route)))

(defn discussion-view-group
  "Switch between different discussion views."
  [props]
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])
        current-route @(rf/subscribe [:navigation/current-route-name])
        href #(navigation/href % {:share-hash share-hash})
        img (fn [img-key] [:img {:height 20
                                 :class "bg-white rounded-1"
                                 :src (img-path img-key)}])]
    [:> ButtonGroup (merge {:aria-label "TODO" :size :sm} props)
     (doall
      (for [[route {:keys [icon label]}] discussion-views]
        [:> Button {:key (str "discussion-view-element-" route)
                    :variant (if (active-button? current-route route) :primary :outline-primary)
                    :className "clickable"
                    :href (href route)}
         [:div [img icon]] [:small (labels label)]]))]))

(defn download-schnaq-button
  "Button to download a schnaq."
  [& {:keys [props vertical?]}]
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [:> NavLink (merge {:on-click #(txt-export-request share-hash @(rf/subscribe [:schnaq/title]))}
                       props)
     [stacked-icon :vertical? vertical? :icon-key :file-download] (labels :discussion.navbar/download)]))

(defn share-schnaq-button
  "Share schnaq button opening a modal."
  [& {:keys [props vertical?]}]
  [share-schnaq-modal
   (fn [modal-props]
     [:> NavLink (merge modal-props props)
      [stacked-icon :vertical? vertical? :icon-key :share] (labels :discussion.navbar/share)])])

(defn manage-schnaq-button
  "Button to navigate to schnaq management page."
  [& {:keys [props vertical?]}]
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [:> NavLink (merge {:href (navigation/href :routes.schnaq/moderation-center {:share-hash share-hash})}
                       props)
     [stacked-icon :vertical? vertical? :icon-key :sliders-h] (labels :schnaq.moderation.edit/administrate-short)]))

(defn schnaq-settings
  "Show the schnaq settings, export and share links."
  []
  [:<>
   [:> NavLink {:disabled true} (labels :discussion.navbar/settings)]
   [share-schnaq-button :props {:className "ms-2"}]
   [download-schnaq-button :props {:className "ms-2"}]
   [manage-schnaq-button :props {:className "ms-2"}]])

(defn- page-title
  "Display the current title either of the schnaq or the page in the navbar."
  [& {:keys [props]}]
  (let [title (or @(rf/subscribe [:schnaq/title]) @(rf/subscribe [:page/title]))]
    [:> NavbarText (merge {:class "navbar-title"} props)
     [:h1.h6 title]]))

(defn mobile-navigation
  "Mobile navigation."
  [& {:keys [props]}]
  [:> Navbar (merge {:bg :primary :variant :dark :expand false} props)
   [:> Container {:fluid true}
    [:> NavbarBrand {:href (toolbelt/current-overview-link)}
     [schnaqqi-white :props {:className "img-fluid" :width 50}]]
    [page-title]
    [:> NavbarToggle {:aria-controls "mobile-navbar"}]
    [:> NavbarCollapse {:id "mobile-navbar"}
     [:> Nav
      [user-navlink-dropdown]
      [admin-dropdown]
      [LanguageDropdown]
      (if @(rf/subscribe [:schnaq/share-hash])
        [:div.row
         [:div.col-6 [links-to-discussion-views]]
         [:div.col-6 [schnaq-settings]]]
        [common-navigation-links])]]]])

(defn split-navbar [& {:keys [props]}]
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [:> Navbar (merge {:bg :transparent :variant :light :expand :lg :className "small text-nowrap"} props)
     [:> Container {:fluid true}
      [:div.d-flex.align-items-center.panel-white-sm.py-0.ps-0
       [:> NavbarBrand {:className "p-0" :href (toolbelt/current-overview-link)}
        [:div.schnaq-logo-container
         [schnaqqi-white :props {:className "img-fluid" :width 50}]]]
       [page-title]]
      [:> NavbarToggle {:aria-controls "schnaq-navbar-big"}]
      [:> NavbarCollapse {:id "schnaq-navbar-big"
                          :className "justify-content-end"}
       (when share-hash
         [:> Nav {:className "panel-white-sm mx-2"}
          [discussion-view-group]])
       [:> Nav {:className "panel-white-sm"}
        (if share-hash
          [:<>
           [share-schnaq-button :vertical? true]
           [download-schnaq-button :vertical? true]
           [manage-schnaq-button :vertical? true]]
          [common-navigation-links :vertical? true])
        [LanguageDropdown :props {:className "nav-link-no-padding"} :vertical? true]
        [upgrade-button :vertical? true]
        [admin-dropdown :vertical? true :props {:className "nav-link-no-padding"}]
        [user-navlink-dropdown :vertical? true :props {:className "nav-link-no-padding"}]]]]]))

(defn page-navbar []
  [:> Navbar {:bg :primary :variant :dark :expand :lg}
   [:> Container
    [:> NavbarBrand {:href (toolbelt/current-overview-link)}
     [schnaq-logo-white :props {:className "img-fluid" :width 150}]]
    [:> NavbarToggle {:aria-controls "schnaq-navbar"}]
    [:> NavbarCollapse {:id "schnaq-navbar"
                        :className "justify-content-end"}
     [:> Nav
      [common-navigation-links :hide-icon? true]
      [LanguageDropdown :hide-icon? true]
      [upgrade-button]
      [admin-dropdown]
      [user-navlink-dropdown]]]]])

;; -----------------------------------------------------------------------------

(defn collapsible-navbar
  "Collapsible navbar with split content header, collapsible-content-id must match id of collapsible-content."
  [brand-content collapse-content-id navbar-bg-class top-right-content collapsible-content]
  (when-not @(rf/subscribe [:ui/setting :hide-navbar])
    [:<>
     [:div.d-xl-none [mobile-navigation]]
     #_[split-navbar]
     [:div.d-none.d-xl-block [split-navbar]]
     [:div.d-none.d-xl-block
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
      collapsible-content]]))
