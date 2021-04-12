(ns schnaq.interface.views.navbar.user-management
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))

(defn- name-input
  "An input, where the user can set their name. Happens automatically by typing."
  [username btn-class]
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
   [:input.btn.mt-1
    {:class btn-class
     :type "submit"
     :value (labels :user.button/set-name)}]])

(defn- change-name-button
  "Display button to change the user's nickname."
  []
  [:form.form-inline.clickable
   {:on-click #(rf/dispatch [:user/show-display-name-input])
    :on-submit #(js-wrap/prevent-default %)}
   [:input.btn.dropdown-item {:type "submit"
                              :value (labels :user.button/change-name)}]])

(defn- username-bar-view
  "A bar containing all user related utilities and information."
  [button-class]
  (let [username @(rf/subscribe [:user/display-name])
        show-input? @(rf/subscribe [:user/show-display-name-input?])]
    (if show-input?
      [name-input username button-class]
      [change-name-button])))

(defn- admin-star
  "Show a star icon if the user is logged in as admin."
  []
  (when @(rf/subscribe [:user/administrator?])
    [:i.pr-1 {:class (str "far " (fa :star))
              :title (labels :user.profile/star-tooltip)}]))

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
          (labels :router/analytics)]]]])))

(defn user-handling-dropdown
  "Dropdown menu to change user name, to log in, ..."
  [button-class]
  (let [username @(rf/subscribe [:user/display-name])
        authenticated? @(rf/subscribe [:user/authenticated?])
        login-logout-event (if authenticated? :keycloak/logout :keycloak/login)]
    [:ul.navbar-nav.dropdown
     [:a#profile-dropdown.nav-link
      {:href "#" :role "button" :data-toggle "dropdown"
       :aria-haspopup "true" :aria-expanded "false"}
      [:button.btn.dropdown-toggle.rounded-2 {:class button-class}
       [admin-star]
       username]]
     [:div.dropdown-menu.dropdown-menu-right {:aria-labelledby "profile-dropdown"}
      (when-not authenticated?
        [username-bar-view])
      (when authenticated?
        [:a.dropdown-item {:href (reitfe/href :routes.user.manage/account)}
         (labels :user.profile/settings)])
      [:button.dropdown-item {:on-click #(rf/dispatch [login-logout-event])}
       (if authenticated?
         (labels :user/logout)
         (labels :user/login))]]]))