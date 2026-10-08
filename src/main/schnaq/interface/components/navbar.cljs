(ns schnaq.interface.components.navbar
  (:require ["react-bootstrap/Container" :as Container]
            ["react-bootstrap/Nav" :as Nav]
            ["react-bootstrap/Navbar" :as Navbar]
            [goog.string :refer [format]]
            [oops.core :refer [oget oset!]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.interface.components.colors :refer [colors]]
            [schnaq.interface.components.common :refer [schnaq-logo-white schnaqqi-white]]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.components.navbar-lib :refer [graph-settings-notification
                                                            LanguageDropdown txt-export-request
                                                            admin-dropdown
                                                            user-navlink-dropdown]]
            [schnaq.interface.config :as config]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.discussion.card-elements :as card-elements]
            [schnaq.interface.views.discussion.share :refer [share-schnaq-modal]]
            [schnaq.links :as links]))

(def ^:private NavbarBrand (oget Navbar :Brand))
(def ^:private NavbarText (oget Navbar :Text))
(def ^:private NavbarToggle (oget Navbar :Toggle))
(def ^:private NavbarCollapse (oget Navbar :Collapse))
(def ^:private NavLink (oget Nav :Link))

(defn- common-navigation-links
  "Show default navigation links."
  [& {:keys [props hide-icon?]}]
  [:<>
   [tooltip/text
    (labels :nav/schnaqs-tooltip)
    [:> NavLink (merge {:href (toolbelt/current-overview-link)} props)
     (when-not hide-icon? [icon :comments "fa-fw me-2"])
     (labels :nav/schnaqs)]]
   [tooltip/text
    (labels :router/privacy-tooltip)
    [:> NavLink (merge {:href (links/privacy-policy @(rf/subscribe [:current-locale]))} props)
     (when-not hide-icon? [icon :lock "fa-fw me-2"])
     (labels :router/privacy)]]])

(def ^:private discussion-views
  "Collection containing the discussion views."
  {:routes.schnaq/start {:icon-key :table-cells-large
                         :label :discussion.button/text}
   :routes/graph-view {:icon-key :graph
                       :label :graph.button/text}
   :routes.schnaq/qanda {:icon-key :info-question
                         :label :qanda.button/text}
   :routes.schnaq/dashboard {:icon-key :layer-group
                             :label :summary.link.button/text}})

(defn- active-button? [current-route asked-route]
  (if (= asked-route :routes.schnaq/start)
    (or (= current-route asked-route) (= current-route :routes.schnaq.select/statement))
    (= current-route asked-route)))

(defn- menu-heading
  "Section heading inside the collapsed mobile menu."
  [label]
  [:span.nav-link.fw-bold.pe-none {:role "heading" :aria-level 2} label])

(defn- links-to-discussion-views
  "Toggle between different views in a discussion."
  [& {:keys [props]}]
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])
        current-route @(rf/subscribe [:navigation/current-route-name])
        href #(navigation/href % {:share-hash share-hash})]
    [:<>
     [menu-heading (labels :discussion.navbar/views)]
     (doall
      (for [[route {:keys [icon-key label]}] discussion-views]
        (let [active? (active-button? current-route route)]
          [:> NavLink (merge {:key (str "discussion-view-element-" route)
                              :class (str "ms-3 px-2 rounded" (when active? " fw-bold bg-white bg-opacity-25"))
                              :href (href route)
                              :active active?
                              :aria-current (when active? "page")}
                             props)
           [icon icon-key "fa-fw me-2"] (labels label)])))]))

(defn- discussion-view-group
  "Switch between different discussion views."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])
        current-route @(rf/subscribe [:navigation/current-route-name])]
    [:nav.view-switcher {:aria-label (labels :discussion.navbar/views)}
     (doall
      (for [[route {:keys [icon-key label]}] discussion-views
            :let [active? (active-button? current-route route)]]
        [:a {:key (str "discussion-view-element-" route)
             :href (navigation/href route {:share-hash share-hash})
             :class (when active? "active")
             :aria-current (when active? "page")}
         [icon icon-key "fa-fw me-2"] (labels label)]))]))

(defn- download-schnaq-button
  "Button to download a schnaq."
  [& {:keys [props]}]
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [tooltip/text
     (labels :schnaq.export/as-text)
     [:> NavLink (merge {:on-click #(txt-export-request share-hash @(rf/subscribe [:schnaq/title]))}
                        props)
      [icon :file-download "fa-fw me-2"] (labels :discussion.navbar/download)]]))

(defn- share-schnaq-button
  "Share schnaq button opening a modal."
  [& {:keys [props]}]
  [share-schnaq-modal
   (fn [modal-props]
     [tooltip/text
      (labels :sharing/tooltip)
      [:> NavLink (merge modal-props props)
       [icon :share "fa-fw me-2"] (labels :discussion.navbar/share)]])])

(defn- manage-schnaq-button
  "Button to navigate to schnaq management page."
  [& {:keys [props]}]
  (when @(rf/subscribe [:user/moderator?])
    (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
      [tooltip/text
       (labels :schnaq.admin/tooltip)
       [:> NavLink (merge {:href (navigation/href :routes.schnaq/moderation-center {:share-hash share-hash})}
                          props)
        [icon :sliders-h "fa-fw me-2"] (labels :schnaq.moderation.edit/administrate-short)]])))

(defn- overview-page-button
  "Return to the overview page."
  [& {:keys [props]}]
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])
        {:keys [icon-key label]} (:routes.schnaq/start discussion-views)]
    [:> NavLink (merge {:href (navigation/href :routes.schnaq/start {:share-hash share-hash})}
                       props)
     [icon icon-key "fa-fw me-2"] (labels label)]))

(defn- login-register-buttons [& {:keys [props]}]
  [:<>
   [tooltip/text
    (labels :nav/login-tooltip)
    [:> NavLink (merge {:bsPrefix "btn btn-outline-dark nav-control me-2"
                        :on-click #(rf/dispatch [:keycloak/login])}
                       props)
     [icon :sign-in "fa-fw me-2"]
     (labels :nav/login)]]
   [tooltip/text
    (labels :nav/register-tooltip)
    [:> NavLink (merge {:bsPrefix "btn btn-outline-secondary nav-control"
                        :on-click #(rf/dispatch [:keycloak/register
                                                 (str (links/relative-to-absolute-url (navigation/href :routes.schnaqs/personal))
                                                      "?create-demo=true")])}
                       props)
     [icon :user-plus "fa-fw me-2"]
     (labels :nav/register)]]])

(defn- schnaq-settings
  "Show the schnaq settings, export and share links."
  []
  [:<>
   [menu-heading (labels :discussion.navbar/settings)]
   [share-schnaq-button :props {:className "ms-2"}]
   [download-schnaq-button :props {:className "ms-2"}]
   [manage-schnaq-button :props {:className "ms-2"}]])

(defn- download-graph-as-png
  "Download the graph as a png file."
  []
  (let [canvas (.querySelector js/document (format "#%s div canvas" config/graph-id))
        anchor (.createElement js/document "a")]
    (oset! anchor [:href] (.toDataURL canvas "image/png"))
    (oset! anchor [:download] "graph.png")
    (.click anchor)))

(defn- graph-settings
  "Show graph settings. `ids?` adds the anchors for the mindmap tour."
  [& {:keys [props ids?]}]
  [:<>
   [tooltip/text
    (labels :graph.download/as-png)
    [:> NavLink (merge props {:on-click download-graph-as-png
                              :id (when ids? "graph-export")})
     [icon :file-export "fa-fw me-2"] (labels :graph.download/button)]]
   [tooltip/text
    (labels :graph.settings/title)
    [:> NavLink (merge props {:on-click graph-settings-notification
                              :id (when ids? "graph-settings")})
     [icon :sliders-h "fa-fw me-2"] (labels :graph.settings/button)]]])

(defn- page-title
  "Display the current title either of the schnaq or the page in the navbar."
  [& {:keys [props]}]
  (let [title (or @(rf/subscribe [:schnaq/title]) @(rf/subscribe [:page/title]))]
    [:> NavbarText (merge {:class "navbar-title"} props)
     [:h1.h6.mb-0 title]]))

(defn- statement-counter
  "A counter showing all statements and pulsing live."
  []
  (let [number-of-questions @(rf/subscribe [:schnaq.selected/statement-number])]
    [:> NavbarText {}
     [:div.d-flex.flex-row.px-3
      [motion/pulse-once [icon :comment/alt]
       [:schnaq.qa.new-question/pulse?]
       [:schnaq.qa.new-question/pulse false]
       (:white colors)
       (:secondary colors)]
      [:div.ms-2 number-of-questions]]]))

;; -----------------------------------------------------------------------------

(defn- theme-logo
  "Theme logo; renders `fallback` when no logo is set or it fails to load."
  [_props _fallback]
  (let [failed-src (r/atom nil)]
    (fn [props fallback]
      (let [logo (:theme.images/logo @(rf/subscribe [:schnaq/theme]))]
        (if (and logo (not= logo @failed-src))
          [:img.object-fit-contain (merge {:src logo :alt ""
                                           :on-error #(reset! failed-src logo)}
                                          props)]
          fallback)))))

(defn- schnaqqi-white-brand []
  [theme-logo {:height 50 :style {:max-width "35vw"}}
   [schnaqqi-white :props {:className "img-fluid" :width 50}]])

(defn- mobile-navigation
  "Mobile navigation."
  [& {:keys [props]}]
  [:> Navbar (merge {:bg :primary :variant :dark :expand false} props)
   [:> Container {:fluid true}
    [:> NavbarBrand {:href (toolbelt/current-overview-link) :aria-label (labels :nav/schnaqs)}
     [schnaqqi-white-brand]]
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
         [:div.col-6
          [schnaq-settings]
          (when @(rf/subscribe [:navigation/current-route? :routes/graph-view])
            [:<>
             [menu-heading (labels :graph.button/text)]
             [graph-settings :props {:className "ms-2"}]])]]
        [common-navigation-links])]]]])

(defn- discussion-context-bar
  "Second navigation row of a discussion: switch views and use the tools of the current view."
  []
  (let [current-route @(rf/subscribe [:navigation/current-route-name])]
    [:div.discussion-context-bar.panel-white-sm.d-flex.align-items-center.gap-2.small.text-nowrap
     [discussion-view-group]
     [:div.ms-auto.d-flex.align-items-center
      (cond
        (= current-route :routes/graph-view) [graph-settings :ids? true]
        (active-button? current-route :routes.schnaq/start) [card-elements/discussion-tools "search-bar"])]]))

(defn- split-navbar
  "Navbar for discussions."
  []
  (let [authenticated? @(rf/subscribe [:user/authenticated?])
        share-hash @(rf/subscribe [:schnaq/share-hash])]
    [:<>
     [:> Navbar {:bg :transparent :variant :light :expand :lg :className "split-navbar small text-nowrap"}
      [:> Container {:fluid true}
       [:div.d-flex.align-items-center.panel-white-sm.p-0.w-100
        [:> NavbarBrand {:className "p-0" :href (toolbelt/current-overview-link)
                         :aria-label (labels :nav/schnaqs)}
         [theme-logo {:class "p-1" :height 65 :style {:max-width "12rem"}}
          [:div.schnaq-logo-container
           [schnaqqi-white :props {:className "img-fluid" :width 50}]]]]
        [page-title]
        [:> Nav {:className "ms-auto align-items-center px-2"}
         (if share-hash
           [:<>
            [share-schnaq-button]
            [download-schnaq-button]
            [manage-schnaq-button]]
           [common-navigation-links])
         [:div.navbar-separator {:aria-hidden true}]
         [LanguageDropdown :props {:id "language-dropdown-desktop"}]
         [admin-dropdown]
         (if (or share-hash authenticated?)
           [user-navlink-dropdown]
           [login-register-buttons])]]]]
     (when share-hash
       [:div.container-fluid.pb-2 [discussion-context-bar]])]))

;; -----------------------------------------------------------------------------

(defn page-navbar
  "Navbar for the static pages."
  []
  (when-not @(rf/subscribe [:ui/setting :hide-navbar])
    [:> Navbar {:bg :primary :variant :dark :expand :lg}
     [:> Container {:fluid true}
      [:> NavbarBrand {:href (toolbelt/current-overview-link)}
       [schnaq-logo-white :props {:className "img-fluid" :width 150}]]
      [:> NavbarToggle {:aria-controls "schnaq-navbar"}]
      [:> NavbarCollapse {:id "schnaq-navbar"
                          :className "justify-content-end"}
       [:> Nav
        [common-navigation-links :hide-icon? true]
        [LanguageDropdown :hide-icon? true]
        [admin-dropdown]
        [user-navlink-dropdown]]]]]))

(defn qanda-navbar
  "Navbar for the Q&A view."
  []
  (when-not @(rf/subscribe [:ui/setting :hide-navbar])
    [:<>
     [:div.d-xl-none [mobile-navigation]]
     [:div.d-none.d-xl-block
      [:> Navbar {:bg :primary :variant :dark :expand :lg}
       [:> Container {:fluid true}
        [:> NavbarBrand {:href (toolbelt/current-overview-link) :aria-label (labels :nav/schnaqs)}
         [schnaqqi-white-brand]]
        [page-title]
        [:> NavbarToggle {:aria-controls "schnaq-navbar"}]
        [:> NavbarCollapse {:id "schnaq-navbar"
                            :className "justify-content-end"}
         [:> Nav {:className "align-items-center"}
          [statement-counter]
          [overview-page-button]
          [LanguageDropdown :props {:id "language-dropdown-desktop"}]
          [user-navlink-dropdown]]]]]]]))

(defn discussion-navbar
  "Default navbar for discussions and their views."
  []
  (when-not @(rf/subscribe [:ui/setting :hide-navbar])
    [:<>
     [:div.d-xl-none [mobile-navigation]]
     [:div.d-none.d-xl-block [split-navbar]]]))
