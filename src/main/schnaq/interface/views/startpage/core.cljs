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
  [:section.overflow-hidden.py-3.my-5
   [wavy/top-and-bottom
    :white
    [:div.container-lg.text-center
     [:p.h4 (labels :startpage.early-adopter/title)]
     [:p.lead.pb-3 (labels :startpage.early-adopter/body)]
     [:a.btn.btn-lg.button-secondary
      {:role "button"
       :href (reitfe/href :routes.schnaq/create)}
      (labels :schnaq.create.button/save)]]]])

(defn supporters []
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

(defn- faq
  "Handle some still open questions from the user."
  []
  [:section.pt-5
   [wavy/top-and-bottom
    :dark-blue
    [:div.container
     [:span.text-white.text-center
      [:h2 (labels :startpage.faq/title)]
      [:p.lead (labels :startpage.faq/subtitle)]]
     [qanda/question-field-and-search-results]]]])

(defn- founders-note
  "A personal note from the founders, to the visitor of the page. Give a last personal touch."
  []
  [:section.pb-5.text-center
   [:h4.text-center (labels :startpage.founders-note/title)]
   [:div.d-flex.align-items-center
    [:div
     [:div.flex-fill
      [:img.img-fluid.mb-2.shadow {:src (img-path :founders-note)}]]
     [:p [:strong "– Alexander, Christian und Michael"]]]
    [:div.flex-fill
     [:img.img-fluid.shadow.w-75 {:src (img-path :team/sitting-on-couches)}]]]])


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
      [startpage-features/feature-rows]]
     [faq]
     [testimonials/view]
     [:section.container
      [mailchimp-form]]
     [early-adopters]
     [:section.container
      [founders-note]
      [supporters]]]]])

(defn startpage-view
  "A view that represents the first page of schnaq participation or creation."
  []
  [startpage-content])
