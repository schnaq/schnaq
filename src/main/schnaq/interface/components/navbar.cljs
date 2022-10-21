(ns schnaq.interface.components.navbar
  (:require ["react-bootstrap/Button" :as Button]
            ["react-bootstrap/ButtonGroup" :as ButtonGroup]
            ["react-bootstrap/Container" :as Container]
            ["react-bootstrap/Nav" :as Nav]
            ["react-bootstrap/Navbar" :as Navbar]
            [goog.string :refer [format]]
            [oops.core :refer [oget oset!]]
            [re-frame.core :as rf]
            [schnaq.interface.components.colors :refer [colors]]
            [schnaq.interface.components.common :as common-components :refer [schnaq-logo-white schnaqqi-white]]
            [schnaq.interface.components.icons :refer [icon stacked-icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.config :as config]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.discussion.share :refer [share-schnaq-modal]]
            [schnaq.interface.views.navbar.elements :refer [graph-settings-notification
                                                            LanguageDropdown txt-export-request
                                                            admin-dropdown
                                                            user-navlink-dropdown]]
            [schnaq.links :as links]))

(def ^:private NavbarBrand (oget Navbar :Brand))
(def ^:private NavbarText (oget Navbar :Text))
(def ^:private NavbarToggle (oget Navbar :Toggle))
(def ^:private NavbarCollapse (oget Navbar :Collapse))
(def ^:private NavLink (oget Nav :Link))

(defn- upgrade-button
  "Show an upgrade button for non-pro users."
  [& {:keys [props vertical?]}]
  (when (and @(rf/subscribe [:user/authenticated?])
             (not @(rf/subscribe [:user/pro?])))
    [tooltip/text
     (labels :pricing.upgrade-nudge/tooltip)
     [:> NavLink (merge {:bsPrefix "btn btn-outline-secondary"
                         :on-click #(rf/dispatch [:navigation.redirect/follow {:redirect "https://schnaq.com/pricing"}])}
                        props)
      [icon :star (if vertical? "d-block mx-auto" "me-1") {:size :sm}]
      (labels :pricing.upgrade-nudge/button)]]))

(defn- common-navigation-links
  "Show default navigation links."
  [& {:keys [props vertical? hide-icon?]}]
  [:<>
   [tooltip/text
    (labels :nav/schnaqs-tooltip)
    [:> NavLink (merge {:href (toolbelt/current-overview-link)} props)
     (when-not hide-icon? [stacked-icon :vertical? vertical? :icon-key :comments])
     (labels :nav/schnaqs)]]
   [tooltip/text
    (labels :router/pricing-tooltip)
    [:> NavLink (merge {:href "https://schnaq.com/pricing"} props)
     (when-not hide-icon? [stacked-icon :vertical? vertical? :icon-key :award])
     (labels :router/pricing)]]
   [tooltip/text
    (labels :router/privacy-tooltip)
    [:> NavLink (merge {:href "https://schnaq.com/privacy"} props)
     (when-not hide-icon? [stacked-icon :vertical? vertical? :icon-key :lock])
     (labels :router/privacy)]]
   [tooltip/text
    (labels :nav/blog-tooltip)
    [:> NavLink (merge {:href "https://schnaq.com/blog/"} props)
     (when-not hide-icon? [stacked-icon :vertical? vertical? :icon-key :newspaper])
     (labels :nav/blog)]]])

(def ^:private discussion-views
  "Collection containing the discussion views."
  {:routes.schnaq/start {:icon :icon-cards-dark
                         :label (labels :discussion.button/text)}
   :routes/graph-view {:icon :icon-graph-dark
                       :label (labels :graph.button/text)}
   :routes.schnaq/qanda {:icon :icon-qanda-dark
                         :label (labels :qanda.button/text)}
   :routes.schnaq/dashboard {:icon :icon-summary-dark
                             :label (labels :summary.link.button/text)}})

(defn- links-to-discussion-views
  "Toggle between different views in a discussion."
  [& {:keys [props]}]
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])
        href #(navigation/href % {:share-hash share-hash})
        img (fn [img-key] [:img {:height 25
                                 :class "navbar-icon"
                                 :src (img-path img-key)}])]
    [:<>
     [:> NavLink {:disabled true} (labels :discussion.navbar/views)]
     (doall
      (for [[route {:keys [icon label]}] discussion-views]
        [:> NavLink (merge {:key (str "discussion-view-element-" route)
                            :class "ms-3" :href (href route)}
                           props)
         [img icon] label]))]))

(defn- active-button? [current-route asked-route]
  (if (= asked-route :routes.schnaq/start)
    (or (= current-route asked-route) (= current-route :routes.schnaq.select/statement))
    (= current-route asked-route)))

(defn- discussion-view-button-image
  "Prepare the image for the discussion view button."
  [& {:keys [props img-key]}]
  [:img (merge {:height 25
                :className "d-block mx-auto bg-white p-1 rounded-1"
                :src (img-path img-key)}
               props)])

(defn- discussion-view-group
  "Switch between different discussion views."
  [& {:keys [props]}]
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])
        current-route @(rf/subscribe [:navigation/current-route-name])
        href #(navigation/href % {:share-hash share-hash})]
    [:> ButtonGroup (merge {:aria-label "Cycle discussion views" :size :sm} props)
     (doall
      (for [[route {:keys [icon label]}] discussion-views]
        [:> Button {:key (str "discussion-view-element-" route)
                    :variant (if (active-button? current-route route) :primary :outline-primary)
                    :className "clickable"
                    :href (href route)}
         [discussion-view-button-image :img-key icon] [:small label]]))]))

(defn download-schnaq-button
  "Button to download a schnaq."
  [& {:keys [props vertical?]}]
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [tooltip/text
     (labels :schnaq.export/as-text)
     [:> NavLink (merge {:on-click #(txt-export-request share-hash @(rf/subscribe [:schnaq/title]))}
                        props)
      [stacked-icon :vertical? vertical? :icon-key :file-download] (labels :discussion.navbar/download)]]))

(defn share-schnaq-button
  "Share schnaq button opening a modal."
  [& {:keys [props vertical?]}]
  [share-schnaq-modal
   (fn [modal-props]
     [tooltip/text
      (labels :sharing/tooltip)
      [:> NavLink (merge modal-props props)
       [stacked-icon :vertical? vertical? :icon-key :share] (labels :discussion.navbar/share)]])])

(defn manage-schnaq-button
  "Button to navigate to schnaq management page."
  [& {:keys [props vertical?]}]
  (when @(rf/subscribe [:user/moderator?])
    (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
      [tooltip/text
       (labels :schnaq.admin/tooltip)
       [:> NavLink (merge {:href (navigation/href :routes.schnaq/moderation-center {:share-hash share-hash})}
                          props)
        [stacked-icon :vertical? vertical? :icon-key :sliders-h] (labels :schnaq.moderation.edit/administrate-short)]])))

(defn overview-page-button
  "Return to the overview page."
  [& {:keys [props]}]
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])
        {:keys [icon label]} (:routes.schnaq/start discussion-views)]
    [tooltip/text
     (labels :schnaq.export/as-text)
     [:> NavLink (merge {:className "pt-2 mt-1"
                         :href (navigation/href :routes.schnaq/start {:share-hash share-hash})}
                        props)
      [discussion-view-button-image :img-key icon] label]]))

(defn login-register-buttons [& {:keys [props vertical?]}]
  [:<>
   [tooltip/text
    (labels :nav/login-tooltip)
    [:> NavLink (merge {:bsPrefix "btn btn-sm btn-outline-dark me-2"
                        :on-click #(rf/dispatch [:keycloak/login])}
                       props)
     [icon :sign-in (if vertical? "d-block mx-auto" "me-1") {:size :sm}]
     (labels :nav/login)]]
   [tooltip/text
    (labels :nav/register-tooltip)
    [:> NavLink (merge {:bsPrefix "btn btn-sm btn-outline-secondary"
                        :on-click #(rf/dispatch [:keycloak/register (links/relative-to-absolute-url (navigation/href :routes.user.register/step-2))])}
                       props)
     [icon :user-plus (if vertical? "d-block mx-auto" "me-1") {:size :sm}]
     (labels :nav/register)]]])

(defn- schnaq-settings
  "Show the schnaq settings, export and share links."
  []
  [:<>
   [:> NavLink {:disabled true} (labels :discussion.navbar/settings)]
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
  "Show graph settings."
  [& {:keys [props vertical?]}]
  [:<>
   [tooltip/text
    (labels :graph.download/as-png)
    [:> NavLink (merge {:on-click download-graph-as-png} props)
     [stacked-icon :vertical? vertical? :icon-key :file-export] (labels :graph.download/button)]]
   [tooltip/text
    (labels :graph.settings/title)
    [:> NavLink (merge {:on-click graph-settings-notification} props)
     [stacked-icon :vertical? vertical? :icon-key :sliders-h] (labels :graph.settings/button)]]])

(defn- page-title
  "Display the current title either of the schnaq or the page in the navbar."
  [& {:keys [props]}]
  (let [title (or @(rf/subscribe [:schnaq/title]) @(rf/subscribe [:page/title]))]
    [:> NavbarText (merge {:class "navbar-title"} props)
     [:h1.h6.mb-0 title]]))

(defn statement-counter
  "A counter showing all statements and pulsing live."
  []
  (let [number-of-questions @(rf/subscribe [:schnaq.selected/statement-number])]
    [:> NavbarText {}
     [:div.d-flex.flex-row.p-3
      [motion/pulse-once [icon :comment/alt]
       [:schnaq.qa.new-question/pulse?]
       [:schnaq.qa.new-question/pulse false]
       (:white colors)
       (:secondary colors)]
      [:div.ms-2 number-of-questions]]]))

;; -----------------------------------------------------------------------------

(defn- mobile-navigation
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

(defn- split-navbar
  "Navbar for discussions."
  [& {:keys [props]}]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])
        share-hash @(rf/subscribe [:schnaq/share-hash])]
    [:> Navbar (merge {:bg :transparent :variant :light :expand :lg :className "small text-nowrap"} props)
     [:> Container {:fluid true}
      [:div.d-flex.align-items-center.panel-white-sm.py-0.ps-0.me-2
       [:> NavbarBrand {:className "p-0" :href (toolbelt/current-overview-link)}
        [:div.schnaq-logo-container
         [schnaqqi-white :props {:className "img-fluid" :width 50}]]]
       [page-title]]
      [:> NavbarToggle {:aria-controls "schnaq-navbar-big"}]
      [:> NavbarCollapse {:id "schnaq-navbar-big"
                          :className "justify-content-end"}
       (when @(rf/subscribe [:navigation/current-route? :routes/graph-view])
         [:> Nav {:className "panel-white-sm me-2"}
          [graph-settings :vertical? true]])
       (when share-hash
         [:> Nav {:className "panel-white-sm p-1 me-2"}
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
        (if (or share-hash authenticated?)
          [user-navlink-dropdown :vertical? true :props {:className "nav-link-no-padding"}]
          [login-register-buttons :vertical? true])]]]]))

;; -----------------------------------------------------------------------------

(defn page-navbar
  "Navbar for the static pages."
  []
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

(defn qanda-navbar
  "Navbar for the Q&A view."
  []
  [:> Navbar {:bg :primary :variant :dark :expand :lg}
   [:> Container {:fluid true}
    [:> NavbarBrand {:href (toolbelt/current-overview-link)}
     [schnaq-logo-white :props {:className "img-fluid" :width 150}]]
    [page-title]
    [:> NavbarToggle {:aria-controls "schnaq-navbar"}]
    [:> NavbarCollapse {:id "schnaq-navbar"
                        :className "justify-content-end"}
     [:> Nav
      [statement-counter]
      [overview-page-button]
      [LanguageDropdown :vertical? true]
      [user-navlink-dropdown :vertical? true]]]]])

(defn discussion-navbar
  "Default navbar for discussions and their views."
  []
  (when-not @(rf/subscribe [:ui/setting :hide-navbar])
    [:<>
     [:div.d-xl-none [mobile-navigation]]
     [:div.d-none.d-xl-block [split-navbar]]]))
