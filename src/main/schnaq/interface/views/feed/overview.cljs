(ns schnaq.interface.views.feed.overview
  (:require [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.header-image :as header-image]
            [schnaq.interface.views.hub.common :as hub]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.badges :as badges]))

(defn- no-schnaqs-found
  "Show error message when no meetings were loaded."
  []
  [common/delayed-fade-in
   [:div.alert.alert-primary.text-center
    [:p.lead
     "🙈 "
     (labels :schnaqs.not-found/alert-lead)]
    [:p (labels :schnaqs.not-found/alert-body)]
    [:button.btn.btn-outline-primary
     {:on-click #(rf/dispatch [:navigation/navigate :routes.schnaq/create])}
     (labels :nav.schnaqs/create-schnaq)]]])

(defn- schnaq-entry
  "Displays a single schnaq of the schnaq list"
  [schnaq]
  (let [share-hash (:discussion/share-hash schnaq)
        title (:discussion/title schnaq)
        url (header-image/check-for-header-img (:discussion/header-image-url schnaq))]
    [:article.meeting-entry
     {:on-click (fn []
                  (rf/dispatch [:navigation/navigate :routes.schnaq/start
                                {:share-hash share-hash}])
                  (rf/dispatch [:schnaq/select-current schnaq]))}
     [:div [:img.meeting-entry-title-header-image {:src url}]]
     [:div.px-4.d-flex
      [:div.meeting-entry-title
       [:h5 title]]
      [:div.ml-auto.mt-3
       [badges/read-only-badge schnaq]]]
     [:div.px-4
      [badges/static-info-badges schnaq]]]))

(defn schnaq-list-view
  "Shows a list of schnaqs."
  [subscription-vector]
  [:div.meetings-list
   (let [schnaqs @(rf/subscribe subscription-vector)]
     (if (empty? schnaqs)
       [no-schnaqs-found]
       (for [schnaq schnaqs]
         [:div.pb-4 {:key (:db/id schnaq)}
          [schnaq-entry schnaq]])))])

(defn- feed-button [label on-click-fn focused?]
  (let [button-class (if focused? "feed-button-focused" "feed-button")]
    [:article
     [:button
      {:class button-class :type "button"
       :on-click on-click-fn}
      [:span (labels label)]]]))

(defn- feed-button-navigate [label route focused?]
  [feed-button label #(rf/dispatch [:navigation/navigate route]) focused?])

(defn feed-navigation []
  (let [{:discussion/keys [share-hash edit-hash]} @(rf/subscribe [:schnaq/last-added])
        current-feed @(rf/subscribe [:feed/get-current])
        public-feed? (= current-feed :public)
        personal-feed? (= current-feed :personal)]
    [:<>
     [:section.row
      [:div.col-6.col-md-12
       [feed-button-navigate :router/my-schnaqs :routes.meetings/my-schnaqs personal-feed?]
       [feed-button-navigate :router/public-discussions :routes/public-discussions public-feed?]
       (when-not (nil? edit-hash)
         [feed-button :nav.schnaqs/last-added
          #(rf/dispatch [:navigation/navigate :routes.schnaq/admin-center
                         {:share-hash share-hash :edit-hash edit-hash}])])
       (when-not toolbelt/production?
         [feed-button-navigate :nav.schnaqs/show-all :routes/schnaqs])
       [feed-button-navigate :nav.schnaqs/create-schnaq :routes.schnaq/create]]
      [:div.col-md-12.col-6
       [:hr.d-none.d-md-block]
       [hub/list-hubs-with-heading]]]
     [:hr.d-block.d-md-none]]))

(defn- about-button [label href-link]
  [:div.btn-block
   [:a.btn.btn-outline-primary.rounded-2.w-100 {:href href-link}
    (labels label)]])

(defn feed-extra-information []
  [:div.feed-extra-info.text-right
   [:div.btn-group-vertical
    [about-button :coc/heading (reitfe/href :routes/code-of-conduct)]
    [about-button :how-to/button (reitfe/href :routes/how-to)]]])

(>defn- schnaq-overview
  "Shows the page for an overview of schnaqs. Takes a subscription-key which
  must be a keyword referring to a subscription, which returns a collection of
  schnaqs."
  [subscription-vector page-header]
  [keyword? keyword? :ret vector?]
  [pages/three-column-layout
   {:page/heading (labels page-header)
    :page/subheading (labels :schnaqs/subheader)}
   [feed-navigation]
   [schnaq-list-view subscription-vector]
   [feed-extra-information]])

(defn public-discussions-view
  "Render all public discussions."
  []
  [schnaq-overview [:schnaqs/public] :schnaqs.all/header])

(defn personal-discussions-view
  "Render all discussions in which the user participated."
  []
  [schnaq-overview [:schnaqs.visited/all] :schnaqs/header])

;; events

(rf/reg-event-db
  :feed/store-current
  (fn [db [_ feed-type]]
    ;; store either :personal or :public feed
    (assoc-in db [:feed :current] feed-type)))

(rf/reg-sub
  :feed/get-current
  (fn [db _]
    (get-in db [:feed :current])))
