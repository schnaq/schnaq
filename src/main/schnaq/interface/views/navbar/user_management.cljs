(ns schnaq.interface.views.navbar.user-management
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.components.common :refer [pro-badge]]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.navbar :as nav-component]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.common :as common]))

(defn- name-input
  "An input, where the user can set their name. Happens automatically by typing."
  []
  (let [username @(rf/subscribe [:user/display-name])]
    [:form.dropdown-item
     {:on-submit
      (fn [e] (js-wrap/prevent-default e)
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
    :on-submit #(js-wrap/prevent-default %)}
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
      [:span.pr-1 indicator])))

(defn admin-dropdown
  "Show Admin pages when user is authenticated and has admin role."
  [button-class]
  (let [admin? @(rf/subscribe [:user/administrator?])
        ul-id "admin-dropdown"]
    (when admin?
      [:ul.navbar-nav.dropdown
       [:a#admin-dropdown.nav-link
        {:href "#" :role "button" :data-toggle "dropdown" :id ul-id
         :aria-haspopup "true" :aria-expanded "false"}
        [:button.btn.dropdown-toggle.rounded-2 {:class button-class}
         (labels :nav/admin)]]
       [:div.dropdown-menu.dropdown-menu-right {:aria-labelledby (str ul-id)}
        [:li.nav-item
         [:a.nav-link {:role "button" :href (reitfe/href :routes/admin-center)}
          (labels :router/admin-center)]]
        [:li.nav-item
         [:a.nav-link {:role "button" :href (reitfe/href :routes/feedbacks)}
          (labels :router/all-feedbacks)]]
        [:li.nav-item
         [:a.nav-link {:role "button" :href (reitfe/href :routes/analytics)}
          (labels :router/analytics)]]
        [:li.nav-item
         [:a.nav-link {:role "button" :href (reitfe/href :routes.admin/summaries)}
          (labels :router/summaries)]]]])))

(defn- profile-picture-in-nav
  "Show profile picture-element in the navbar."
  []
  (let [username @(rf/subscribe [:user/display-name])
        authenticated? @(rf/subscribe [:user/authenticated?])
        icon-size 32]
    [:<>
     [:div.mx-auto.rounded-2.overflow-hidden
      (if authenticated? [common/avatar icon-size] [common/identicon username icon-size])]
     [:p.small.text-nowrap.dropdown-toggle
      [role-indicator]
      (toolbelt/truncate-to-n-chars username 15)]]))

(defn- login-dropdown-items []
  [:<>
   [:a.dropdown-item {:href (reitfe/href :routes.user.manage/account)}
    (labels :user.profile/settings)]
   [:a.dropdown-item {:href "#"                             ;; For the :active states and pointer to behave
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
      [:div.dropdown-menu.dropdown-menu-right {:aria-labelledby "profile-dropdown"}
       (if authenticated?
         [login-dropdown-items]
         [:<>
          [namechange-menu-point]
          [:a.btn.dropdown-item {:href "#"
                                 :on-click #(rf/dispatch [:keycloak/login])}
           (labels :user/register)]])]]]))

(defn- register-button []
  [:a.btn.btn-sm.btn-dark
   {:href "#" :on-click #(rf/dispatch [:keycloak/login])}
   (labels :nav/register)])

(defn register-or-user-button
  "If not authenticated, show register button else show user menu"
  [button-class]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])]
    (if authenticated?
      [user-dropdown-button button-class]
      [nav-component/separated-button
       [register-button]])))
