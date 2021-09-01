(ns schnaq.interface.views.schnaq.value
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [schnaq.interface.text.display-data :refer [fa img-path labels]]
            [schnaq.interface.views.pages :as pages]))

(defn- value [icon title text]
  [:<>
   [:div.bg-white.rounded-2.p-3.d-inline-block
    [:img.img-fluid
     {:src (img-path icon)}]]
   [:h5.mt-3 (labels title)]
   [:h6 (labels text)]])

(defn- values []
  [:<>
   [:div.row.my-5.pt-5
    [:div.col-4 [value :value/shield :schnaq.value.security/title :schnaq.value.security/text]]
    [:div.col-4 [value :value/cards :schnaq.value.cards/title :schnaq.value.cards/text]]
    [:div.col-4 [value :value/share :schnaq.value.share/title :schnaq.value.share/text]]]
   [:div.row.my-5.pt-5
    [:div.col-4 [value :value/bubble :schnaq.value.respect/title :schnaq.value.respect/text]]
    [:div.col-4 [value :value/book :schnaq.value.results/title :schnaq.value.results/text]]]])

(defn- next-button []
  (let [{:discussion/keys [share-hash]} @(rf/subscribe [:schnaq/selected])]
    [:div.row.px-1.pb-5
     [:a.btn.btn-dark-highlight.p-3.rounded-1.ml-auto.mb-5
      {:href (rfe/href :routes.schnaq/start {:share-hash share-hash})}
      (labels :schnaqs/continue-to-schnaq-button)
      [:i.ml-2 {:class (fa :arrow-right)}]]]))

(defn- value-content []
  [:<>
   [values]
   [next-button]])

(defn- create-value-page []
  [pages/with-nav-and-header
   {:page/heading (labels :schnaq.value/title)
    :page/subheading (labels :schnaq.value/subtitle)
    :page/title (labels :schnaq.create/title)
    :page/vertical-header? true
    :page/more-for-heading [value-content]
    :page/classes "base-wrapper bg-dark-blue"}])

(defn schnaq-value-view
  "Displays a view with schnaq's most important features"
  []
  [create-value-page])
