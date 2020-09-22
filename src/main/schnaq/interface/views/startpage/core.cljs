(ns schnaq.interface.views.startpage.core
  "Defining the startpage of schnaq."
  (:require [schnaq.interface.views.base :as base]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.views.startpage.features :as startpage-features]
            [re-frame.core :as rf]
            [schnaq.interface.config :as config]))

(defn- header-animation
  "Display header animation video"
  []
  [:div.col-lg-6
   [:img.w-100 {:src (img-path :animation-discussion)}]])

(defn- header []
  [base/header
   (labels :startpage/heading)
   (labels :startpage/subheading)
   [:div.pt-5 {:key "HeaderExtras-Bullet-Points-and-Animation"}
    [:div.row
     [:div.col-lg-6.icon-bullets
      (base/icon-bullet (img-path :icon-community) (labels :startpage.heading-list/community))
      (base/icon-bullet (img-path :icon-robot) (labels :startpage.heading-list/exchange))
      (base/icon-bullet (img-path :icon-reports) (labels :startpage.heading-list/reports))]
     [header-animation]]]])

(defn- start-schnaq-button
  "Tell user to create a schnaq now."
  []
  [:section
   [:button.button-call-to-action
    {:type "button"
     :on-click #(rf/dispatch [:navigation/navigate :routes.meeting/create])}
    (labels :startpage.button/create-schnaq)]])

(defn- under-construction
  []
  [:div.icon-bullets-larger
   (base/img-bullet-subtext (img-path :icon-crane)
                            (labels :startpage.under-construction/heading)
                            (labels :startpage.under-construction/body))])

(defn- icons-grid
  "Display features in a grid."
  []
  [:section.features-icons.text-center
   [:div.container
    [:div.row
     (base/icon-in-grid (fa :laptop) (labels :startpage.grid/innovative) (labels :startpage.grid/innovative-body))
     (base/icon-in-grid (fa :comment) (labels :startpage.grid/communicative) (labels :startpage.grid/communicative-body))
     (base/icon-in-grid (fa :carry) (labels :startpage.grid/cooperative) (labels :startpage.grid/cooperative-body))]]])

(defn- usage-of-schnaq-heading
  "Heading introducing the features of schnaq."
  []
  [:div.d-flex.d-row.justify-content-center.py-3
   [:p.display-5 (labels :startpage.usage/lead)]
   [:img.pl-3.d-md-none.d-lg-block
    {:style {:max-height "3rem"}
     :src (img-path :schnaqqifant/original)}]])

(def wavy-top
  [:svg {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 1440 320"} [:path {:fill "#1292ee" :fill-opacity "1" :d "M0,96L48,96C96,96,192,96,288,85.3C384,75,480,53,576,69.3C672,85,768,139,864,154.7C960,171,1056,149,1152,154.7C1248,160,1344,192,1392,208L1440,224L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"}]])

(defn- early-adopters
  "Present early-adopters section to catch up interest."
  []
  [:section.overflow-hidden.py-3
   [base/wavy-curve "scale(1.5,-1)"]
   [:div.early-adopter
    [:div.container.text-center.early-adopter-schnaqqifant-wrapper
     [:img.early-adopter-schnaqqifant.pull-right.d-none.d-md-inline
      {:src (img-path :schnaqqifant/white)}]
     [:p.h4 (labels :startpage.early-adopter/title)]
     [:p.lead.pb-3 (labels :startpage.early-adopter/body)]
     [:a.button-secondary {:href config/demo-discussion-link}
      (labels :startpage.early-adopter.buttons/join-schnaq)]
     [:p.pt-4 (labels :startpage.early-adopter/or)]
     [:button.button-secondary
      {:type "button"
       :on-click #(rf/dispatch [:navigation/navigate :routes.meeting/create])}
      (labels :startpage.button/create-schnaq)]]]
   [base/wavy-curve "scale(1.5,1)"]])

(defn- subscribe-to-mailinglist
  "Add possibility to subscribe to our mailing list."
  []
  [:section.container.text-center.subscribe-to-mailinglist
   [:p.h4 (labels :startpage.mailing-list/title)]
   [:p.lead.pb-3 (labels :startpage.mailing-list/body)]
   [:a.button-primary {:href "https://disqtec.com/newsletter"
                       :target "_blank"}
    (labels :startpage.mailing-list/button)]])


;; -----------------------------------------------------------------------------

(defn- startpage-content []
  [:<>
   [base/nav-header]
   [header]
   [:section.container
    [:div.row.mt-5
     [:div.col-12.col-lg-6.pb-3.pb-lg-0
      [under-construction]]
     [:div.col-12.col-lg-6.text-center
      [start-schnaq-button]]]
    [icons-grid]
    [usage-of-schnaq-heading]
    [startpage-features/feature-rows]]
   [early-adopters]
   [subscribe-to-mailinglist]])

(defn startpage-view
  "A view that represents the first page of schnaq participation or creation."
  []
  [startpage-content])