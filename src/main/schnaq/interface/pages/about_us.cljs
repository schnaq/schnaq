(ns schnaq.interface.pages.about-us
  (:require [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.config :refer [marketing-num-schnaqs marketing-num-statements]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.pages :as pages]))

(defn- value-card
  [icon-key title body]
  [:div.card.p-3.mb-4.border-0
   [:div.card-img-top.text-center
    [icon icon-key "text-primary" {:size "3x"}]]
   [:div.card-body
    [:h5.card-title.text-center (labels title)]
    [:p.card-text (labels body)]]])

(defn- number-cell
  [number text]
  [:div.number-cell.text-center
   [:h2 number]
   [:h5 text]])

(defn- person-card [photo-keyword name & title]
  [:article.card.shadow
   [:img.img-fluid.card-img-top {:src (img-path photo-keyword) :alt (str "Picture of " name)}]
   [:div.card-body
    [:h6.card-title name]
    [:p.card-subtitle.text-muted [:i title]]]])

(def ^:private schnaq-unity
  [:section
   [:div.row.pt-3
    [:div.col-lg-5
     [:img.img-fluid.shadow {:src (img-path :team/at-table-with-laptop)}]]
    [:div.offset-lg-1.col-lg-6
     [:h2 (labels :about-us.unity/title)]
     (labels :about-us.unity/body)]]])

(def ^:private our-values
  [:section
   [:h2.text-center (labels :about-us.value/title)]
   [:h4.text-center.text-muted.pb-5 (labels :about-us.value/subtitle)]
   [:div.row.justify-content-around
    [:div.col-lg-4
     [value-card :envelope-open-text :about-us.honesty/title :about-us.honesty/body]]
    [:div.col-lg-4
     [value-card :handshake :about-us.collaborate/title :about-us.collaborate/body]]
    [:div.col-lg-4
     [value-card :location-arrow :about-us.action/title :about-us.action/body]]]

   [:div.row.justify-content-around
    [:div.col-lg-4
     [value-card :gem :about-us.quality/title :about-us.quality/body]]
    [:div.col-lg-4
     [value-card :user/group :about-us.diversity/title :about-us.diversity/body]]]])

(def ^:private schnaq-in-numbers
  [:section
   [:h2.text-center.pb-3 (labels :about-us.numbers/title)]
   [:div.row
    [:div.col [number-cell "6+" (labels :about-us.numbers/research)]]
    [:div.col [number-cell (str marketing-num-schnaqs "+") (labels :about-us.numbers/users)]]
    [:div.col [number-cell (str marketing-num-statements "+") (labels :about-us.numbers/statements)]]
    [:div.col [number-cell "12.000+" (labels :about-us.numbers/loc)]]]])

(def ^:private team-focus
  [:section
   [:h2.text-center (labels :about-us.team/title)]
   [:div.row.row-cols-1.row-cols-md-3.pb-5.pt-4
    [:div.col
     [person-card :team/alexander "Dr. Alexander Schneider" (labels :about-us.team/alexander)]]
    [:div.col
     [person-card :team/christian "Dr. Christian Meter" (labels :about-us.team/christian)]]
    [:div.col
     [person-card :team/mike "Michael Birkhoff" (labels :about-us.team/mike)]]]])

;; ----------------------------------------------------------------------------

(defn page [_request]
  [pages/with-nav-and-header
   {:page/heading (labels :about-us.page/heading)
    :page/subheading (labels :about-us.page/subheading)
    :page/vertical-header? true}
   [:div.container
    schnaq-unity
    [:hr.pb-4.mt-5]
    our-values
    [:hr.pb-4.mt-5]
    schnaq-in-numbers
    [:hr.pb-4.mt-5]
    team-focus]])
