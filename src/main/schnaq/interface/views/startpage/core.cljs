(ns schnaq.interface.views.startpage.core
  "Defining the startpage of schnaq."
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [schnaq.interface.text.display-data :refer [labels img-path]]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.startpage.call-to-actions :as cta]
            [schnaq.interface.views.startpage.features :as startpage-features]
            [schnaq.interface.views.startpage.testimonials :as testimonials]))

(defn- mailchimp-form
  []
  [:section.row.pt-5.pb-5
   [:div.col-12.col-md-4
    [:img.img-fluid {:src (img-path :startpage/newsletter)}]]
   [:div.col-12.col-md-8
    [:h3.text-center (labels :startpage.newsletter/heading)]
    [:form
     {:target "_blank" :name "mc-embedded-subscribe-form" :method "post" :action
      "https://schnaq.us8.list-manage.com/subscribe?u=adbf5722068bcbcc4c7c14a72&id=407d47335d"}

     [:div.form-group
      [:input
       {:required true
        :placeholder (labels :startpage.newsletter/address-placeholder)
        :name "EMAIL" :defaultValue "" :type "email"
        :class "form-control"}]]

     [:div.form-group
      [:div.this-is-just-for-bots-do-not-fill-this-out
       {:aria-hidden "true" :style {:position "absolute" :left "-5000px"}}
       [:input {:defaultValue "" :tabIndex "-1" :type "text"
                :name "b_adbf5722068bcbcc4c7c14a72_407d47335d"}]]]

     [:div.form-group
      [:div.form-check
       [:input#nochmal-nachfragen.form-check-input {:type "checkbox" :required true}]
       [:label.form-check-label {:for "nochmal-nachfragen"}
        (labels :startpage.newsletter/consent)]
       [:a {:href "#" :type "button" :data-toggle "collapse" :data-target "#collapse-more-newsletter"
            :aria-expanded "false" :aria-controls "#collapse-more-newsletter"}
        (labels :startpage.newsletter/more-info-clicker)]
       [:div.collapse {:id "collapse-more-newsletter"}
        [:p.small (labels :startpage.newsletter/policy-disclaimer)
         [:br] (labels :startpage.newsletter/privacy-policy-lead)
         [:a {:href "https://disqtec.com/datenschutz"} (labels :startpage.newsletter/privacy-policy)] "."]]]]

     [:div.form-group
      [:input
       {:name "subscribe" :value (labels :startpage.newsletter/button) :type "submit" :readOnly true
        :class "btn btn-primary d-block mx-auto"}]]]]])


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
     [:a.button-secondary {:href (rfe/href :routes.schnaqs/public)}
      (labels :startpage.early-adopter.buttons/join-schnaq)]
     [:p.pt-4 (labels :startpage.early-adopter/or)]
     [:button.button-secondary
      {:type "button"
       :on-click #(rf/dispatch [:navigation/navigate :routes.schnaq/create])}
      (labels :schnaq.create.button/save)]]]
   [base/wavy-curve "scale(1.5,1)"]])

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
         :page/subheading (labels :startpage/subheading)
         :page/more-for-heading [cta/features-call-to-action]}]
    [pages/with-nav-and-header
     header
     [:<>
      [:section.container
       [startpage-features/feature-rows]
       [mailchimp-form]
       [testimonials/view]]
      [early-adopters]
      [:section.container
       [supporters]]]]))

(defn startpage-view
  "A view that represents the first page of schnaq participation or creation."
  []
  [startpage-content])