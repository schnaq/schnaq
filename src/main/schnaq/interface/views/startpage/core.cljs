(ns schnaq.interface.views.startpage.core
  "Defining the startpage of schnaq."
  (:require [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [labels img-path]]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.pages :as pages]
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
         [:br] (labels :startpage.newsletter/privacy-policy-lead)
         [:a {:href (reitfe/href :routes/privacy-extended)}
          (labels :privacy/note)] "."]]]]

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
     [:img.early-adopter-schnaqqifant.d-none.d-md-inline
      {:src (img-path :schnaqqifant/white)}]
     [:p.h4 (labels :startpage.early-adopter/title)]
     [:p.lead.pb-3 (labels :startpage.early-adopter/body)]
     [:a.btn.button-secondary {:role "button"
                               :href "mailto:info@schnaq.com"}
      (labels :startpage.early-adopter/test)]
     [:p.pt-4 (labels :startpage.early-adopter/or)]
     [:a.btn.button-secondary
      {:role "button"
       :href (reitfe/href :routes.schnaq/create)}
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

(defn- faq
  "Handle some still open questions from the user."
  []
  [:section.pt-5
   [:h4.text-center (labels :startpage.faq/title)]
   [:p [:strong (labels :startpage.faq.data/question)] " "
    (labels :startpage.faq.data/answer-1)
    [:a {:href (reitfe/href :routes/privacy)} (labels :startpage.faq.data/link-name)]
    (labels :startpage.faq.data/answer-2)]
   [:p [:strong (labels :startpage.faq.integration/question)] " " (labels :startpage.faq.integration/answer)
    [:a {:href "#newsletter"} (labels :startpage.faq.integration/link-name)]]
   [:p [:strong (labels :startpage.faq.costs/question)] " " (labels :startpage.faq.costs/answer)]
   [:p [:strong (labels :startpage.faq.start/question)] " " (labels :startpage.faq.start/answer)
    [:a {:href (reitfe/href :routes.schnaq/create)} (labels :startpage.faq.start/link-name)]]
   [:p [:strong (labels :startpage.faq.why/question)] " " (labels :startpage.faq.why/answer)]])

(defn- founders-note
  "A personal note from the founders, to the visitor of the page. Give a last personal touch."
  []
  [:section.pb-5.text-center
   [:h4.text-center (labels :startpage.founders-note/title)]
   [:div.d-flex.align-items-center
    [:div
     [:div.flex-fill
      [:img.img-fluid.mb-2.shadow {:src (img-path :founders-note)}]]
     [:p [:strong "– Alexander, Christian, Michael und Philip"]]]
    [:div.flex-fill
     [:img.img-fluid.shadow.w-75 {:src (img-path :team/sitting-on-couches)}]]]])


;; -----------------------------------------------------------------------------
(defn- startpage-content []
  [pages/with-nav-and-header
   {:page/heading (labels :startpage/heading)
    :page/subheading (labels :startpage/subheading)
    :page/vertical-header? true
    :page/more-for-heading (with-meta [cta/features-call-to-action] {:key "unique-cta-key"})}
   [:<>
    [:section.container
     [startpage-features/feature-rows]
     [testimonials/view]
     [mailchimp-form]
     [faq]]
    [early-adopters]
    [:section.container
     [founders-note]
     [supporters]]]])

(defn startpage-view
  "A view that represents the first page of schnaq participation or creation."
  []
  [startpage-content])