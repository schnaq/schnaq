(ns schnaq.interface.views.startpage.core
  "Defining the startpage of schnaq."
  (:require [reitit.frontend.easy :as reitfe]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.wavy :as wavy]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.qa.inputs :as qanda]
            [schnaq.interface.views.startpage.call-to-actions :as cta]
            [schnaq.interface.views.startpage.features :as startpage-features]
            [schnaq.interface.views.startpage.testimonials :as testimonials]))

(defn- mailchimp-form
  []
  [:section#newsletter.row.pt-5.pb-5
   [:div.col-12.col-md-4.my-auto
    [:img.img-fluid {:src (img-path :schnaqqifant/mail)}]]
   [:div.col-12.col-md-8.my-auto
    [:h4.text-center (labels :startpage.newsletter/heading)]
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
       [:a {:href "#data" :type "button" :data-toggle "collapse" :data-target "#collapse-more-newsletter"
            :aria-expanded "false" :aria-controls "#collapse-more-newsletter" :data-reitit-handle-click false}
        (labels :startpage.newsletter/more-info-clicker)]
       [:div.collapse {:id "collapse-more-newsletter"}
        [:p.small (labels :startpage.newsletter/policy-disclaimer)
         [:br] (labels :startpage.newsletter/privacy-policy-lead) " "
         [:a {:href (reitfe/href :routes/privacy-extended)}
          (labels :privacy/note)] "."]]]]

     [:div.form-group
      [:input
       {:name "subscribe" :value (labels :startpage.newsletter/button) :type "submit" :readOnly true
        :class "btn btn-primary d-block mx-auto"}]]]]])

(defn- early-adopters
  "Present early-adopters section to catch up interest."
  []
  [:div.text-center.my-5
   [:p.h4.text-primary (labels :startpage.early-adopter/title)]
   [:p.lead.pb-3 (labels :startpage.early-adopter/body)]
   [:a.btn.btn-lg.button-secondary
    {:role "button"
     :href (reitfe/href :routes.schnaq/create)}
    (labels :schnaq.startpage.cta/button)]])

(defn supporters []
  [:<>
   [:div.text-center
    [:a {:href "https://ignitiondus.de"}
     [:img.w-50
      {:src (img-path :logos/ignition)
       :alt "ignition logo"}]]]
   [:div.text-center
    [:a {:href "https://www.digihub.de/"}
     [:img.w-50.pt-md-4
      {:src (img-path :logos/digihub)
       :alt "digihub logo"}]]]])

(defn- faq
  "Handle some still open questions from the user."
  []
  [:section.pt-5
   [wavy/top-and-bottom
    :primary
    [:div.container-fluid
     [:span.text-center.text-white
      [:h2 (labels :startpage.faq/title)]
      [:p.lead (labels :startpage.faq/subtitle)]]
     [qanda/question-field-and-search-results :dark]]]])

(defn- team-and-supporters
  "Give a last personal touch."
  []
  [:section.pb-5.text-center
   [:div.row
    [:div.col-12.col-lg-6
     [:img.img-fluid.mb-2 {:src (img-path :startpage/team-schnaq)
                           :style {:max-width "400px"}}]
     [:h2 [:a {:href (reitfe/href :routes/about-us)} (labels :startpage/team-schnaq-heading)]]]
    [:div.col-12.col-lg-6.my-lg-auto
     [:p.h5.mb-5 (labels :startpage/team-schnaq)]
     [:p.h4.text-primary.text-center (labels :supporters/heading)]
     [supporters]]]])

(defn- single-step
  "A single step to success."
  [lead heading image-key]
  [:div.col-12.col-lg-4
   [:p.leading-number.text-center.m-0 lead]
   [:div.text-center
    [:p.mb-1 heading]
    [:img.img-fluid.mt-2.shadow.rounded-1.startpage-step-image
     {:src (img-path image-key)}]]])

(defn- three-steps-to-success
  "A short three step explanation how schnaq leads to success. Could be expanded with a before / after persona."
  []
  [:div.row.mt-5
   [:div.col-12.text-center.h2 (labels :startpage.three-steps/heading)]
   [single-step "1."
    [:a {:href (reitfe/href :routes.schnaq/create)} (labels :startpage.three-steps/first)] :startpage/create-schnaq]
   [single-step "2." (labels :startpage.three-steps/second) :startpage/share-schnaq]
   [single-step "3." (labels :startpage.three-steps/third) :startpage/answer-schnaq]])

;; -----------------------------------------------------------------------------
(defn- startpage-content []
  [:div.overflow-hidden
   [pages/with-nav-and-header
    {:page/title (labels :startpage/heading)
     :page/wrapper-classes "container container-85"
     :page/vertical-header? true
     :page/more-for-heading (with-meta [cta/features-call-to-action] {:key "unique-cta-key"})}
    [:<>
     [:section.container
      [startpage-features/how-does-schnaq-work]
      [testimonials/highlights]
      [three-steps-to-success]
      [startpage-features/feature-rows]]
     [faq]
     [:section.container
      [:h2.text-center.mt-4 (labels :startpage.social-proof/companies)]
      [testimonials/testimonial-companies]
      [early-adopters]
      [mailchimp-form]
      [team-and-supporters]]]]])

(defn startpage-view
  "A view that represents the first page of schnaq participation or creation."
  []
  [startpage-content])
