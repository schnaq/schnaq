(ns schnaq.interface.views.navbar.user-management
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.common :refer [pro-badge]]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.navbar :as nav-component]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.common :as common]))

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
       :name "name-input"
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
                              :value (labels :user.button/change-name)}]])

(defn- namechange-menu-point
  "A bar containing all user related utilities and information."
  []
  (let [show-input? @(rf/subscribe [:user/show-display-name-input?])]
    (if show-input?
      [name-input]
      [change-name-button])))

(defn- role-indicator
  "Show an icon if the user has special roles."
  []
  (let [admin? @(rf/subscribe [:user/administrator?])
        beta-tester? @(rf/subscribe [:user/beta-tester?])
        pro-user? @(rf/subscribe [:user/pro-user?])
        indicator (cond
                    admin? [icon :star]
                    beta-tester? [icon :rocket]
                    (and pro-user? (not beta-tester?)) [pro-badge])]
    (when indicator
      [:span.pe-1 indicator])))

(defn admin-dropdown
  "Show Admin pages when user is authenticated and has admin role."
  [button-class]
  (let [admin? @(rf/subscribe [:user/administrator?])
        ul-id "admin-dropdown"]
    (when admin?
      [:ul.navbar-nav.dropdown
       [:button#admin-dropdown.nav-link.btn.dropdown-toggle
        {:role "button" :data-bs-toggle "dropdown" :id ul-id
         :class button-class
         :aria-haspopup "true" :aria-expanded "false"}
        (labels :nav/admin)]
       [:ul.dropdown-menu.dropdown-menu-end {:aria-labelledby (str ul-id)}
        [:li.dropdown-item
         [:a.btn {:role "button" :href (navigation/href :routes/admin-center)}
          (labels :router/admin-center)]]
        [:li.dropdown-item
         [:a.btn {:role "button" :href (navigation/href :routes/feedbacks)}
          (labels :router/all-feedbacks)]]
        [:li.dropdown-item
         [:a.btn {:role "button" :href (navigation/href :routes/analytics)}
          (labels :router/analytics)]]
        [:li.dropdown-item
         [:a.btn {:role "button" :href (navigation/href :routes.admin/summaries)}
          (labels :router/summaries)]]]])))

(defn- profile-picture-in-nav
  "Show profile picture-element in the navbar."
  []
  (let [username @(rf/subscribe [:user/display-name])
        authenticated? @(rf/subscribe [:user/authenticated?])
        icon-size 32]
    [:div.d-flex.flex-column
     [:div.mx-auto.rounded-2.overflow-hidden
      (if authenticated? [common/avatar icon-size] [common/identicon username icon-size])]
     [:p.small.text-nowrap.dropdown-toggle
      [role-indicator]
      (toolbelt/truncate-to-n-chars username 15)]]))

(defn- login-dropdown-items []
  [:<>
   [:a.dropdown-item {:href (navigation/href :routes.user.manage/account)}
    (labels :user.profile/settings)]
   [:button.dropdown-item
    {:role "button"
     :on-click #(rf/dispatch [:keycloak/logout])}
    (labels :user/logout)]])

(defn user-dropdown-button
  "The default user dropdown. It displays the avatar and a name with a droppable menu.
  The menu depends on the login-state of the user. Must be used as a child of a .dropdown."
  [button-class]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])]
    [:div.dropdown
     [nav-component/separated-button
      [profile-picture-in-nav]
      {:class button-class
       :data-bs-toggle "dropdown"
       :aria-expanded "false"}
      [:div.dropdown-menu.dropdown-menu-end {:aria-labelledby "profile-dropdown"}
       (if authenticated?
         [login-dropdown-items]
         [:<>
          [namechange-menu-point]
          [:button.btn.dropdown-item
           {:role "button"
            :on-click #(rf/dispatch [:keycloak/login])}
           (labels :user/register)]])]]]))

(defn- register-button []
  [:button.btn.btn-sm.btn-dark
   {:role "button" :on-click #(rf/dispatch [:keycloak/login])}
   (labels :nav/register)])

(defn register-or-user-button
  "If not authenticated, show register button else show user menu"
  [button-class]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])]
    (if authenticated?
      [user-dropdown-button button-class]
      [register-button])))
