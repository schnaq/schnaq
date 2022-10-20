(ns schnaq.interface.views.navbar.user-management
  (:require ["react-bootstrap" :refer [Alert Button]]
            ["react-bootstrap/NavDropdown" :as NavDropdown]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.icons :refer [icon stacked-icon]]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt :refer [session-storage-enabled?]]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.common :as common]
            [schnaq.links :as links]))

(def ^:private NavDropdownItem (oget NavDropdown :Item))
(def ^:private NavDropdownDivider (oget NavDropdown :Divider))

(defn- login-not-possible
  "Show a different component, if login is not possible."
  []
  [tooltip/html
   [:<> [icon :cookie-bite "me-2"] (labels :login.not-possible/tooltip)]
   [:> Alert {:variant "light" :class "m-0 p-2"}
    [icon :exclamation-triangle "me-2"] (labels :login.not-possible/text)]
   {:trigger :mouseenter}])

(defn- name-input
  "An input, where the user can set their name. Happens automatically by typing."
  []
  (let [username @(rf/subscribe [:user/display-name])]
    [:form.dropdown-item
     {:on-submit
      (fn [e] (.preventDefault e)
        (rf/dispatch [:user/set-display-name
                      (oget e [:target :elements :name-input :value])]))}
     [:input#name-input.form-control.form-round-05
      {:type "text"
       :autoFocus true
       :required true
       :defaultValue username
       :placeholder (labels :user.button/set-name-placeholder)}]
     [:input.btn.mt-1 {:type "submit"
                       :value (labels :user.button/set-name)}]]))

(defn- change-name-button
  "Display button to change the user's nickname."
  []
  [:form.clickable
   {:on-click #(rf/dispatch [:user/show-display-name-input])
    :on-submit #(.preventDefault %)}
   [:input.btn.dropdown-item {:type "submit"
                              :value (labels :user.button/change-name)
                              :on-click #(matomo/track-event "Active User", "Secondary Action", "Create User-Name")}]])

(defn- namechange-menu-point
  "A bar containing all user related utilities and information."
  []
  (if @(rf/subscribe [:user/show-display-name-input?])
    [name-input]
    [change-name-button]))

(defn admin-dropdown
  "Show Admin pages when user is authenticated and has admin role."
  [& {:keys [vertical? props]}]
  (let [admin? @(rf/subscribe [:user/administrator?])
        analytics-admin? @(rf/subscribe [:user/analytics-admin?])]
    ;; Analytics-Admin also is true when user is super-admin
    (when analytics-admin?
      [:> NavDropdown (merge {:title (r/as-element [:span.text-secondary [stacked-icon :vertical? vertical? :icon-key :ghost] "Admin"])
                              :align :end}
                             props)
       [:> NavDropdownItem {:href (navigation/href :routes/analytics)}
        (labels :router/analytics)]
       (when admin?
         [:<>
          [:> NavDropdownItem {:href (navigation/href :routes/admin-center)}
           (labels :router/admin-center)]
          [:> NavDropdownItem {:href (navigation/href :routes/feedbacks)}
           (labels :router/all-feedbacks)]
          [:> NavDropdownItem {:href (navigation/href :routes.admin/summaries)}
           (labels :router/summaries)]
          (when-not shared-config/production?
            [:> NavDropdownItem {:href (navigation/href :routes.playground/editor)}
             (labels :routes.playground/editor)])])])))

(defn- profile-picture-in-nav
  "Show profile picture-element in the navbar."
  [& {:keys [props vertical?]}]
  (let [username @(rf/subscribe [:user/display-name])
        authenticated? @(rf/subscribe [:user/authenticated?])
        pro? @(rf/subscribe [:user/pro?])
        icon-size 25] ;; wip 30
    [:span props
     (if authenticated?
       [common/avatar
        :props (when vertical? {:className "d-block mx-auto"})
        :size icon-size
        :inline? (not vertical?)]
       [:span {:className (when vertical? "d-flex mx-auto")}
        [common/identicon
         :props {:className (if vertical? "d-block mx-auto" "me-1")}
         :name username
         :size icon-size]])
     [:span.text-nowrap
      (when pro? [icon :star "me-1"])
      (toolbelt/truncate-to-n-chars username 15)]]))

(defn- login-dropdown-items []
  [:<>
   [:a.dropdown-item {:href (navigation/href :routes.user.manage/account)}
    (labels :user.profile/settings)]
   [:button.dropdown-item
    {:role "button"
     :on-click #(rf/dispatch [:keycloak/logout])}
    (labels :user/logout)]])

(defn user-navlink-dropdown
  [& {:keys [props vertical?]}]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])]
    [:> NavDropdown (merge {:title (r/as-element [profile-picture-in-nav :vertical? vertical?])
                            :align :end}
                           props)
     (if authenticated?
       [:<>
        [:> NavDropdownItem {:disabled true} [common/avatar :size 32]]
        [:> NavDropdownDivider]
        [:> NavDropdownItem {:href (navigation/href :routes.user.manage/account)}
         (labels :user.profile/settings)]
        [:> NavDropdownItem {:on-click #(rf/dispatch [:keycloak/logout])}
         (labels :user/logout)]]
       [:<>
        [namechange-menu-point]
        (if session-storage-enabled?
          [:> NavDropdownItem {:on-click #(rf/dispatch [:keycloak/login])}
           (labels :user/register)]
          [login-not-possible])])]))

(defn- separated-button
  "TODO REDUNDANT!"
  ([button-content]
   [separated-button button-content {}])
  ([button-content attributes]
   [separated-button button-content attributes nil])
  ([button-content attributes dropdown-content]
   [:<>
    [:button.btn.discussion-navbar-button.text-decoration-none
     (merge
      {:type "button"}
      attributes)
     button-content]
    dropdown-content]))

(defn user-dropdown-button
  "The default user dropdown. It displays the avatar and a name with a droppable menu.
  The menu depends on the login-state of the user. Must be used as a child of a .dropdown."
  [on-white-background?]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])]
    [:div.dropdown
     [separated-button
      [profile-picture-in-nav]
      {:class (when-not on-white-background? "text-white")
       :data-bs-toggle "dropdown"
       :aria-expanded "false"}
      [:div.dropdown-menu.dropdown-menu-end {:aria-labelledby "profile-dropdown"}
       (if authenticated?
         [login-dropdown-items]
         [:<>
          [namechange-menu-point]
          (if session-storage-enabled?
            [:> Button {:variant "link"
                        :on-click #(rf/dispatch [:keycloak/login])
                        :class "dropdown-item"}
             (labels :user/register)]
            [login-not-possible])])]]]))

(defn- login-button
  "Show a login button."
  [on-light-background?]
  [buttons/button (labels :nav/login) #(rf/dispatch [:keycloak/login])
   (if on-light-background? "btn-outline-dark" "btn-outline-white")])

(defn- register-button
  "Show registration button."
  [on-light-background?]
  [buttons/button (labels :nav/register)
   #(rf/dispatch [:keycloak/register (links/relative-to-absolute-url (navigation/href :routes.user.register/step-2))])
   (if on-light-background? "btn-outline-secondary ms-2" "btn-dark ms-2")])

(defn register-or-user-button
  "If not authenticated, show register button else show user menu."
  [on-light-background?]
  (if @(rf/subscribe [:user/authenticated?])
    [:<>
     [buttons/upgrade]
     [user-dropdown-button on-light-background?]]
    (if session-storage-enabled?
      [:<>
       [login-button on-light-background?]
       [register-button on-light-background?]]
      [login-not-possible])))
