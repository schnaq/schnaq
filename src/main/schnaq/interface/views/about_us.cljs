(ns schnaq.interface.views.about-us
  (:require [goog.string :as gstring]
            [schnaq.interface.text.display-data :refer [labels img-path]]
            [schnaq.interface.views.pages :as pages]))

(defn- value-card
  [fa-icon title body]
  [:div.card.value-card.p-3.mb-4
   [:div.card-img-top.text-center [:i {:class (gstring/format "text-primary fas %s fa-3x fa-fw" fa-icon)}]]
   [:div.card-body
    [:h5.card-title.text-center (labels title)]
    [:p.card-text (labels body)]]])

(defn- number-cell
  [number text]
  [:div.number-cell.text-center
   [:h2 number]
   [:h5 text]])

(defn ^:private person-card
  [photo-keyword name & title]
  [:div.card.person-card.shadow
   [:div.person-card-inner
    [:img.img-fluid.card-img-top {:src (img-path photo-keyword) :alt (str "Picture of " name)}]
    [:div.card-body
     [:p.card-text name]
     [:p.card-text [:i title]]]]])

(def ^:private schnaq-unity
  [:section
   [:div.row.pt-3
    [:div.col-lg-4
     [:img.img-fluid {:src (img-path :stock/team)}]
     [:small.text-muted "Photo by Javier Allegue Barros on Unsplash"]]
    [:div.offset-lg-1.col-lg-7
     [:h2 (labels :about-us.unity/title)]
     (labels :about-us.unity/body)]]])

(def ^:private our-values
  [:section
   [:h2.text-center (labels :about-us.value/title)]
   [:h4.text-center.text-muted (labels :about-us.value/subtitle)]

   [:div.pb-5]

   [:div.row.justify-content-around
    [:div.col-lg-4
     (value-card "fa-envelope-open-text" :about-us.honesty/title :about-us.honesty/body)]
    [:div.col-lg-4
     (value-card "fa-handshake" :about-us.collaborate/title :about-us.collaborate/body)]
    [:div.col-lg-4
     (value-card "fa-location-arrow" :about-us.action/title :about-us.action/body)]]

   [:div.row.justify-content-around
    [:div.col-lg-4
     (value-card "fa-gem" :about-us.quality/title :about-us.quality/body)]
    [:div.col-lg-4
     (value-card "fa-users" :about-us.diversity/title :about-us.diversity/body)]]])


(def ^:private schnaq-in-numbers
  [:section
   [:h2.text-center.pb-3 "schnaq in Zahlen"]
   [:div.row
    [:div.col (number-cell "6+" (labels :about-us.numbers/research))]
    [:div.col (number-cell "400+" (labels :about-us.numbers/users))]
    [:div.col (number-cell "3.000+" (labels :about-us.numbers/statements))]
    [:div.col (number-cell "12.000+" (labels :about-us.numbers/loc))]]])

(def ^:private team-focus
  [:section
   [:h2.text-center "Team im Fokus"]
   [:div.card-deck.pb-5
    [:div.col
     (person-card :team/alexander "Dr. Alexander Schneider" (labels :about-us.team/alexander))]
    [:div.col
     (person-card :team/christian "Dr. Christian Meter" (labels :about-us.team/christian))]
    [:div.col
     (person-card :team/mike "Michael Birkhoff" (labels :about-us.team/mike))]]])


;; ----------------------------------------------------------------------------

(defn page [_request]
  (pages/with-nav-and-header
    {:page/heading (labels :about-us.page/heading)
     :page/subheading (labels :about-us.page/subheading)}
    [:div.container
     schnaq-unity
     [:hr.pb-4.mt-5]
     our-values
     [:hr.pb-4.mt-5]
     schnaq-in-numbers
     [:hr.pb-4.mt-5]
     team-focus]))