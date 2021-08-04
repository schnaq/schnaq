(ns schnaq.interface.views.hub.common
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.common :as common]))

(defn single-hub
  "Display a single hub."
  [{:hub/keys [keycloak-name name]}]
  [:article
   [:a.btn.btn-link {:href (reitfe/href :routes/hub {:keycloak-name keycloak-name})}
    [common/identicon name 32]
    [:span.pl-2 name]]])

(defn hub-list
  "Displays a list of hubs."
  []
  (let [hubs @(rf/subscribe [:hubs/all])]
    [:div
     (for [[keycloak-name hub] hubs]
       (with-meta
         [single-hub hub]
         {:key keycloak-name}))]))

(defn list-hubs-with-heading
  "Show all hubs for a user."
  []
  (when @(rf/subscribe [:hubs/all])
    [:section
     [:p.h5.text-muted.pb-2 (labels :hubs/heading)]
     [hub-list]]))

(defn logo
  "Get a hub's logo."
  [logo display-name size]
  [:div.avatar-image.p-0
   (if logo
     [:div.profile-pic-fill
      {:style {:height (str size "px") :width (str size "px")}}
      [:img.profile-pic-image {:src logo}]]
     [common/identicon display-name size])])