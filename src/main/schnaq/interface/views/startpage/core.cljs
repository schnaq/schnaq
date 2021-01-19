(ns schnaq.interface.views.startpage.core
  "Defining the startpage of schnaq."
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :as config]
            [schnaq.interface.text.display-data :refer [labels img-path]]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.startpage.call-to-actions :as cta]
            [schnaq.interface.views.startpage.features :as startpage-features]
            [schnaq.interface.views.startpage.testimonials :as testimonials]))

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
       :on-click #(rf/dispatch [:navigation/navigate :routes.brainstorm/create])}
      (labels :brainstorm.buttons/start-now)]]]
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

(defn- button-with-text-section
  "A button and text to navigate to the demo section"
  [button-label fn-navigation title body]
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-5.text-center
    [:button.btn.button-secondary.font-150
     {:on-click fn-navigation}
     (labels button-label)]]
   [:div.col-12.col-lg-6.offset-lg-1
    [:article.feature-text-box.pb-3
     [:h5 (labels title)]
     [:p (labels body)]]]])

(defn- how-to-section
  "A button and text to navigate to the demo section"
  []
  [button-with-text-section
   :how-to.startpage/button
   #(rf/dispatch [:navigation/navigate :routes/how-to])
   :how-to.startpage/title
   :how-to.startpage/body])

(defn- supporters []
  [:section.pb-5.pt-3
   [:p.display-6.text-center
    (labels :supporters/heading)]
   [:div.row.text-center
    [:div.col-md-6
     [:a {:href "https://ignitiondus.de"}
      [:img.w-75
       {:src (img-path :logos/ignition)
        :alt "ignition logo"}]]]
    [:div.col-md-6
     [:a {:href "https://www.digihub.de/"}
      [:img.w-75.pt-md-4
       {:src (img-path :logos/digihub)
        :alt "digihub logo"}]]]]])

;; -----------------------------------------------------------------------------

(defn- startpage-content []
  (let [header
        {:page/heading (labels :startpage/heading)
         :page/more-for-heading (labels :startpage/subheading)}]
    [pages/with-nav-and-header
     header
     [:<>
      [:section.container
       [cta/features-call-to-action]
       [cta/spotlight-discussions]
       [usage-of-schnaq-heading]
       [startpage-features/feature-rows]
       [how-to-section]
       [supporters]]
      [early-adopters]
      [:section.container
       [subscribe-to-mailinglist]
       [testimonials/view]]]]))

(defn startpage-view
  "A view that represents the first page of schnaq participation or creation."
  []
  [startpage-content])