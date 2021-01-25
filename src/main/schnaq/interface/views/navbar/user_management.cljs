(ns schnaq.interface.views.navbar.user-management
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :as config]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.toolbelt :as toolbelt]))

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
      [:button.btn.dropdown-toggle {:class button-class}
       username]]
     [:div.dropdown-menu.dropdown-menu-right {:aria-labelledby "profile-dropdown"}
      [username-bar-view]
      (when-not toolbelt/production?
        [:<>
         [:div.dropdown-divider]
         [:button.dropdown-item {:on-click #(rf/dispatch [login-logout-event])}
          (if authenticated?
            (labels :user/logout)
            (labels :user/login))]
         (when authenticated?
           [:a.dropdown-item {:href config/keycloak-profile-page}
            (labels :user.profile/settings)])])]]))