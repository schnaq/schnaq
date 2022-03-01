(ns schnaq.interface.views.feed.overview
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.time :as util-time]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.feed.filters :as filters]
            [schnaq.interface.views.header-image :as header-image]
            [schnaq.interface.views.hub.common :as hub]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user :as user]))

(defn- no-schnaqs-found
  "Show error message when no meetings were loaded."
  []
  [motion/fade-in-and-out
   [:div.alert.alert-light.text-light.row.layered-wave-background.p-md-5.text-center.rounded-1
    [:div.col-2.py-md-5.d-flex
     [:div.display-1.align-self-center "🙈"]]
    [:div.col-10.py-md-5
     [:h2 (labels :schnaqs.not-found/alert-lead)]
     [:p (labels :schnaqs.not-found/alert-body)]
     [:a.btn.btn-outline-light.mt-1
      {:href (navigation/href :routes.schnaq/create)}
      (labels :nav.schnaqs/create-schnaq)]]]])

(defn sort-options
  "Displays the different sort options for feed elements."
  []
  (let [sort-method @(rf/subscribe [:feed/sort])]
    [:span.ps-2.text-end
     [:button.btn.btn-outline-primary.btn-sm.mx-1
      {:class (when (= sort-method :time) "active")
       :on-click #(rf/dispatch [:feed.sort/set :time])}
      (labels :badges.sort/newest)]
     [:button.btn.btn-outline-primary.btn-sm
      {:class (when (= sort-method :alphabetical) "active")
       :on-click #(rf/dispatch [:feed.sort/set :alphabetical])}
      (labels :badges.sort/alphabetical)]]))

(defn- schnaq-options
  "Adds a dropdown with deletion options to schnaqs, e.g. when displayed in the
  list of schnaqs in a hub."
  [schnaq]
  (let [options-id "options-dropdown-menu"
        dropdown-id "options-dropdown-elements"
        share-hash (:discussion/share-hash schnaq)]
    [:div.dropdown
     [:button.btn.btn-transparent
      {:id options-id :type "button" :data-bs-toggle "dropdown"
       :aria-haspopup "true" :aria-expanded "false"}
      [icon :dots-v]]
     [:div.dropdown-menu.dropdown-menu-end {:id dropdown-id :aria-labelledby options-id}
      [:button.dropdown-item {:type "button"
                              :title (labels :hub.remove.schnaq/tooltip)
                              :on-click (fn [_e] (when (js/confirm (labels :hub.remove.schnaq/prompt))
                                                   (rf/dispatch [:hub.remove/schnaq share-hash])))}
       (labels :hub.remove.schnaq/tooltip)]]]))

(defn- schnaq-title [title]
  [:div.schnaq-entry-title
   [:div.d-md-none (toolbelt/truncate-to-n-chars title 25)]
   [:div.d-none.d-md-block.d-lg-none (toolbelt/truncate-to-n-chars title 50)]
   [:div.d-none.d-lg-block.d-xl-none (toolbelt/truncate-to-n-chars title 25)]
   [:div.d-none.d-xl-block.d-xxl-none (toolbelt/truncate-to-n-chars title 35)]
   [:div.d-none.d-xxl-block.d-hd-none (toolbelt/truncate-to-n-chars title 50)]
   [:div.d-none.d-hd-block.d-qhd-none (toolbelt/truncate-to-n-chars title 70)]
   [:div.d-none.d-qhd-block (toolbelt/truncate-to-n-chars title 100)]])

(defn- schnaq-hub-icon
  "Display a the hub icon above a schnaq."
  [schnaq]
  (let [hubs @(rf/subscribe [:hubs/all])
        logo-map (map (fn [[_keycloak-name hub]]
                        (when (hub/hub-contains-schnaq? hub schnaq)
                          [:img.schnaq-header-hub-image {:src (:hub/logo hub)}])) hubs)
        logo (first (filter some? logo-map))]
    (when logo
      logo)))

(defn- schnaq-header-image [schnaq]
  (let [img-title (take 2 (:discussion/title schnaq))
        img-url (:discussion/header-image-url schnaq)
        theme-header-image (get-in schnaq [:discussion/theme :theme.images/header])
        image (or img-url theme-header-image)
        hubs @(rf/subscribe [:hubs/all])]
    [:div.d-flex.schnaq-header-image
     {:style {:background-image (str "url('" (header-image/check-for-header-img image) "')")}}
     (when hubs
       [schnaq-hub-icon schnaq])
     (when-not image
       [:div.display-4.m-auto.text-white img-title])]))

(defn- schnaq-entry
  "Displays a single schnaq of the schnaq list"
  [schnaq delete-from-hub?]
  [:article.schnaq-entry.d-flex
   [:a.d-flex.flex-row.flex-grow-1.text-reset.text-decoration-none
    {:href (navigation/href :routes.schnaq/start {:share-hash (:discussion/share-hash schnaq)})
     :on-click #(rf/dispatch [:schnaq/select-current schnaq])}
    [schnaq-header-image schnaq]
    [:div.ms-3.w-100.py-2
     [schnaq-title (:discussion/title schnaq)]
     [:div.d-flex.flex-row.mt-auto.pt-3
      [:div [badges/comments-info-badge schnaq]]
      [badges/read-only-badge schnaq]
      [:div.d-flex.flex-row.ms-auto
       [user/user-info-only (:discussion/author schnaq) 24]
       [:small.fw-light.d-inline.my-auto.ms-2
        [util-time/timestamp-with-tooltip (:discussion/created-at schnaq) @(rf/subscribe [:current-locale])]]]]]]
   (when delete-from-hub?
     [schnaq-options schnaq])])

(defn schnaq-list-view
  "Shows a list of schnaqs."
  ([subscription-vector]
   [schnaq-list-view subscription-vector false])
  ([subscription-vector show-delete-from-hub-button?]
   (let [schnaqs @(rf/subscribe subscription-vector)
         sort-method @(rf/subscribe [:feed/sort])
         active-filters @(rf/subscribe [:filters.discussion/active])
         filtered-schnaqs (filters/filter-discussions schnaqs active-filters)
         sorted-schnaqs (if (= :alphabetical sort-method)
                          (sort-by :discussion/title filtered-schnaqs)
                          (sort-by :discussion/created-at > filtered-schnaqs))]
     (if (empty? schnaqs)
       [no-schnaqs-found]
       [:div.panel-white.rounded-1.p-4
        [:div.d-flex.flex-row.mb-4
         [:h6.text-typography.d-none.d-md-block (labels :router/visited-schnaqs)]
         [:div.ms-auto
          [sort-options]
          [filters/filter-button]]]
        (for [schnaq sorted-schnaqs]
          [:div.py-3 {:key (:db/id schnaq)}
           [schnaq-entry schnaq show-delete-from-hub-button?]])]))))

(defn- feed-button
  "Create a button for the feed list."
  ([text image-div class-button route]
   [feed-button text image-div class-button route nil])
  ([text image-div button-class route route-params]
   [:a.btn.btn-link.text-start {:class button-class
                                :role "button"
                                :href (navigation/href route route-params)}
    [:div.d-flex.flex-row
     image-div
     [:div.my-auto.ps-2 text]]]))

(defn icon-and-label-feed-button
  [label icon-name route route-params]
  (let [current-route @(rf/subscribe [:navigation/current-route-name])
        button-class (if (= current-route route) "feed-button-focused" "feed-button")]
    [feed-button (labels label)
     [:div.mx-2.my-auto [icon icon-name "m-auto"]]
     button-class route route-params]))

(defn create-feed-button
  [label icon-name route route-params]
  (let [button-class "feed-button-create"]
    [feed-button (labels label)
     [:div.mx-2.my-auto [icon icon-name "m-auto"]]
     button-class route route-params]))

(defn hub-feed-button
  "Display a single hub."
  [{:hub/keys [keycloak-name name logo]}]
  (let [current-hub @(rf/subscribe [:hub/current])
        current-hub-name (:hub/keycloak-name current-hub)
        path-keycloak-name keycloak-name
        button-class (if (= current-hub-name path-keycloak-name) "feed-button-focused" "feed-button")]
    [feed-button name [hub/hub-logo logo name 32] button-class :routes/hub {:keycloak-name keycloak-name}]))

(defn feed-hubs []
  (when-let [hubs @(rf/subscribe [:hubs/all])]
    [:section
     [:h6.text-typography.pb-2.ms-4 (labels :hubs/heading)]
     [:div.d-flex.flex-column
      (for [[keycloak-name hub] hubs]
        (with-meta [hub-feed-button hub] {:key keycloak-name}))
      [icon-and-label-feed-button :router/visited-schnaqs :eye :routes.schnaqs/personal]]]))

(defn feed-navigation
  "Navigate between the feeds."
  []
  (let [{:discussion/keys [share-hash edit-hash]} @(rf/subscribe [:schnaq/last-added])
        hubs @(rf/subscribe [:hubs/all])]
    [:section
     [:div.d-flex.flex-column.panel-white.mx-0.mt-0.mb-4
      [create-feed-button :nav.schnaqs/create-schnaq :plus :routes.schnaq/create]
      (when-not (nil? edit-hash)
        [icon-and-label-feed-button :nav.schnaqs/last-added :arrow-left
         :routes.schnaq/admin-center {:share-hash share-hash :edit-hash edit-hash}])]
     (when hubs
       [:div.panel-white.mb-4
        [feed-hubs]])]))

(defn- outline-info-button
  "Generic outline button."
  [label href-link]
  [:article.w-100
   [:a.feed-button-outlined {:href href-link}
    (labels label)]])

(defn sidebar-info-links []
  [:section.panel-white.text-center
   [:div.btn-group {:role "group"}
    [:div.btn-group-vertical
     [outline-info-button :coc/heading (navigation/href :routes/code-of-conduct)]]]])

(defn- personal-discussions-view
  "Shows the page for an overview of schnaqs. Takes a subscription-key which
  must be a keyword referring to a subscription, which returns a collection of
  schnaqs."
  []
  [pages/three-column-layout
   {:page/heading (labels :schnaqs/header)
    :page/subheading (labels :schnaqs/subheader)}
   [feed-navigation]
   [schnaq-list-view [:schnaqs.visited/all]]
   [sidebar-info-links]])

(defn page []
  [personal-discussions-view])

(rf/reg-event-db
 :feed.sort/set
 (fn [db [_ method]]
   (assoc-in db [:feed :sort] method)))

(rf/reg-sub
 :feed/sort
 (fn [db _]
   (get-in db [:feed :sort] :time)))
