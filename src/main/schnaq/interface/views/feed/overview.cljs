(ns schnaq.interface.views.feed.overview
  (:require [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.header-image :as header-image]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.utils.toolbelt :as toolbelt]))

(defn- no-schnaqs-found
  "Show error message when no meetings were loaded."
  []
  [:div.alert.alert-primary.text-center
   [:p.lead
    "🙈 "
    (labels :schnaqs.not-found/alert-lead)]
   [:p (labels :schnaqs.not-found/alert-body)]
   [:div.btn.btn-outline-primary
    {:on-click #(rf/dispatch [:navigation/navigate :routes.brainstorm/create])}
    (labels :nav.schnaqs/create-schnaq)]])

(defn- schnaq-entry
  "Displays a single schnaq of the schnaq list"
  [schnaq]
  (let [share-hash (:discussion/share-hash schnaq)
        title (:discussion/title schnaq)
        url (header-image/check-for-header-img (:discussion/header-image-url schnaq))]
    [:div.meeting-entry
     {:on-click (fn []
                  (rf/dispatch [:navigation/navigate :routes.schnaq/start
                                {:share-hash share-hash}])
                  (rf/dispatch [:schnaq/select-current schnaq]))}
     [:div [:img.meeting-entry-title-header-image {:src url}]]
     [:div.meeting-entry-title
      [:h5 title]]]))

(defn- schnaq-list-view
  "Shows a list of schnaqs."
  [subscription-key]
  [:div.meetings-list
   (let [schnaqs @(rf/subscribe [subscription-key])]
     (if (empty? schnaqs)
       [no-schnaqs-found]
       (for [schnaq schnaqs]
         [:div.pb-4 {:key (:db/id schnaq)}
          [schnaq-entry schnaq]])))])

(defn- feed-button [label on-click-fn]
  [:div
   [:button.feed-button
    {:type "button"
     :on-click on-click-fn}
    [:h5 (labels label)]]])

(defn- feed-button-navigate [label route]
  [feed-button label #(rf/dispatch [:navigation/navigate route])])

(defn- feed-navigation []
  ;; todo hier kommt jetzt eine discussion raus
  (let [{:meeting/keys [share-hash edit-hash]} @(rf/subscribe [:schnaq/last-added])]
    [:div
     [feed-button-navigate :router/my-schnaqs :routes.meetings/my-schnaqs]
     [feed-button-navigate :router/public-discussions :routes/public-discussions]
     (when-not (nil? edit-hash)
       [feed-button :nav.schnaqs/last-added #(rf/dispatch [:navigation/navigate
                                                           :routes.meeting/admin-center
                                                           {:share-hash share-hash :edit-hash edit-hash}])])
     (when-not toolbelt/production?
       [feed-button-navigate :nav.schnaqs/show-all :routes/meetings])
     [feed-button-navigate :nav.schnaqs/create-schnaq :routes.brainstorm/create]]))

(defn about-button [label href-link]
  [:div.my-3
   [:a.btn.btn-outline-primary {:href href-link}
    (labels label)]])

(defn- feed-extra-information []
  [:div.feed-extra-info
   [about-button :coc/heading (reitfe/href :routes/code-of-conduct)]
   [about-button :footer.buttons/about-us "https://disqtec.com/ueber-uns"]
   [about-button :nav/blog "https://schnaq.com/blog/"]
   [about-button :footer.buttons/legal-note "https://disqtec.com/impressum"]
   [about-button :router/privacy :routes/privacy]])

(defn- feed-page-dektop [subscription-key]
  [:div.row.px-0.mx-0.py-3
   [:div.col-3.py-3
    [feed-navigation]]
   [:div.col-6.py-3.px-5
    [schnaq-list-view subscription-key]]
   [:div.col-3.py-3
    [feed-extra-information]]])

(defn- feed-page-mobile [subscription-key]
  [:div.my-3
   [schnaq-list-view subscription-key]])

(>defn- schnaq-overview
  "Shows the page for an overview of schnaqs. Takes a subscription-key which
  must be a keyword referring to a subscription, which returns a collection of
  schnaqs."
  [subscription-key]
  [keyword? :ret vector?]
  [pages/with-nav
   {}
   [:div.container-fluid.px-0
    [toolbelt/desktop-mobile-switch
     [feed-page-dektop subscription-key]
     [feed-page-mobile subscription-key]]]])

(defn public-discussions-view
  "Render all public discussions."
  []
  [schnaq-overview :schnaqs/public])

(defn personal-discussions-view
  "Render all discussions in which the user participated."
  []
  [schnaq-overview :schnaqs.visited/all])