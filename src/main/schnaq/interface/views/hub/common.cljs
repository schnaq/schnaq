(ns schnaq.interface.views.hub.common
  (:require [re-frame.core :as rf]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.common :as common]))

(defn hub-logo
  "Get a hub's logo. Refactor this function to reduce redundant code with `avatar`."
  [logo display-name size]
  (if logo
    [:span.profile-pic-fill
     {:style {:height (str size "px") :width (str size "px")}}
     [:img.profile-pic-image {:src logo}]]
    [common/identicon display-name size]))

(defn hub-logo-with-name
  "Hub logo with the name on the right side."
  [logo display-name size]
  [:div.d-flex.flex-row
   [hub-logo logo display-name size]
   [:div.pt-1.ps-2 display-name]])

(defn single-hub
  "Display a single hub."
  [{:hub/keys [keycloak-name name logo]}]
  [:article
   [:a.btn.btn-link {:href (navigation/href :routes/hub {:keycloak-name keycloak-name})}
    [hub-logo-with-name logo name 32]]])

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
     [:hr.mt-3]
     [:h3.text-muted.pb-2 (labels :hubs/heading)]
     [hub-list]]))

(defn hub-contains-schnaq?
  "Check if a hub contains a schnaq and if so returns true"
  [hub schnaq]
  (true? (some #(= (:db/id %) (:db/id schnaq)) (:hub/schnaqs hub))))
