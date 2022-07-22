(ns schnaq.interface.views.user
  (:require [re-frame.core :as rf]
            [schnaq.interface.utils.time :as util-time]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.common :as common]
            [schnaq.user :as user-utils]))

(defn user-info
  "User info box displaying user's nickname, timestamp and the avatar."
  [statement avatar-size additional-classes]
  (let [locale @(rf/subscribe [:current-locale])
        user (:statement/author statement)
        created (:statement/created-at statement)
        display-name (tools/truncate-to-n-chars (user-utils/display-name user) 20)]
    [:div.d-flex {:class additional-classes}
     [common/avatar user avatar-size]
     [:div.mx-2.d-inline.my-auto [:span.text-sm.text-typography display-name]
      (when created
        [:div.small.fw-light.text-muted.d-inline.ms-2.my-auto
         [util-time/timestamp-with-tooltip created locale]])]]))

(defn user-info-only
  "User info box displaying user's nickname and the avatar."
  [user avatar-size]
  (let [authenticated? (:user.registered/keycloak-id user)
        display-name (user-utils/display-name user)
        name-class (if authenticated? "text-typography" "text-muted")]
    [:div.d-flex.flex-row.text-muted
     [:div.d-md-none
      [common/avatar user (* avatar-size 0.75)]]
     [:div.d-none.d-md-block
      [common/avatar user avatar-size]]
     [:small.mx-2.my-auto {:class name-class} display-name]]))

(defn current-user-info
  "Returns the current users profile picture and name as a component."
  [avatar-size name-class]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])
        picture-component (if authenticated? common/avatar common/automatic-identicon)]
    [:div.d-flex.flex-row
     [:div.d-md-none
      [picture-component (.floor js/Math (* avatar-size 0.75))]]
     [:div.d-none.d-md-block
      [picture-component avatar-size]]
     [:small.mx-2.my-auto {:class name-class} @(rf/subscribe [:user/display-name])]]))
