(ns schnaq.interface.views.startpage.core
  "Defining the startpage of schnaq."
  (:require [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.wavy :as wavy]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.qa.inputs :as qanda]
            [schnaq.interface.views.startpage.call-to-actions :as cta]
            [schnaq.interface.views.startpage.features :as startpage-features]
            [schnaq.interface.views.startpage.testimonials :as testimonials]))

(defn- cleverreach-form
  "A form to subscribe to the schnaq newsletter with cleverreach."
  []
  [:section#newsletter.row.pt-5.pb-5
   [:div.col-12.col-md-4.my-auto
    [:img.img-fluid {:src (img-path :schnaqqifant/mail)
                     :alt (labels :schnaqqifant.mail/alt-text)}]]
   [:div.col-12.col-md-8.my-auto
    [:h4.text-center (labels :startpage.newsletter/heading)]
    [:form
     {:action "https://seu2.cleverreach.com/f/314219-322401/wcs/" :method "post" :target "_blank"}
     [:div.cr_body.cr_page.cr_font.formbox
      [:div.form-floating.mb-3
       [:input#text7214965.form-control {:type "email" :placeholder "name@example.com"}]
       [:label {:for "text7214965"} (labels :startpage.newsletter/address-placeholder)]]
      [:button.btn.btn-primary.d-block.mx-auto {:type "submit"}
       (labels :startpage.newsletter/button)]
      [:noscript [:a {:href "http://www.cleverreach.de"} "www.CleverReach.de"]]]]]])

(defn- early-adopters
  "Present early-adopters section to catch up interest."
  []
  [:div.text-center.my-5
   [:p.h4.text-primary (labels :startpage.early-adopter/title)]
   [:p.lead.pb-3 (labels :startpage.early-adopter/body)]
   [:a.btn.btn-lg.button-secondary
    {:role "button"
     :href (navigation/href :routes.schnaq/create)}
    (labels :schnaq.startpage.cta/button)]])

(defn supporters []
  [:<>
   [:div.text-center
    [:a {:href "https://ignitiondus.de"}
     [:img.w-50
      {:src (img-path :logos/ignition)
       :alt "ignition logo"}]]]])

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
     [qanda/question-field-and-search-results :dark]]
    :primary-and-secondary]])

(defn- team-and-supporters
  "Give a last personal touch."
  []
  [:section.pb-5.text-center
   [:div.row
    [:div.col-12.col-lg-6
     [:img.img-fluid.mb-2 {:src (img-path :startpage/team-schnaq)
                           :alt "Alexander Schneider, Michael Birkhoff, Christian Meter"
                           :style {:max-width "400px"}}]
     [:h2 [:a {:href (navigation/href :routes/about-us)} (labels :startpage/team-schnaq-heading)]]]
    [:div.col-12.col-lg-6.my-lg-auto
     [:p.h5.mb-5 (labels :startpage/team-schnaq)]
     [:p.h4.text-primary.text-center (labels :supporters/heading)]
     [supporters]]]])

(defn- single-step
  "A single step to success."
  [heading image-key padding-class]
  [:div.col-12.col-lg-4
   {:class padding-class}
   [:div.text-center
    [:div.display-6.mb-1 heading]
    [:img.img-fluid.mt-2.startpage-step-image
     {:src (img-path image-key)
      :alt ""
      :role "presentation"}]]])

(defn- three-steps-to-success
  "A short three step explanation how schnaq leads to success. Could be expanded with a before / after persona."
  []
  [:div.row
   [:div.col-12.text-center.h2.mb-5 (labels :startpage.three-steps/heading)]
   [single-step
    [:a.text-decoration-none {:href (navigation/href :routes.schnaq/create)} (labels :startpage.three-steps/first)]
    :startpage.schnaqqifant/create-schnaq
    "startpage-step-1"]
   [single-step (labels :startpage.three-steps/second)
    :startpage.schnaqqifant/share-schnaq
    "startpage-step-2"]
   [single-step (labels :startpage.three-steps/third)
    :startpage.schnaqqifant/knowledge-card
    "startpage-step-3"]])

;; -----------------------------------------------------------------------------
(defn- startpage-content []
  [:div.overflow-hidden
   [pages/with-nav-and-header
    {:page/title (labels :startpage/title)
     :page/description (labels :startpage/description)
     :page/vertical-header? true
     :page/wavy-footer? true
     :page/more-for-heading (with-meta [cta/features-call-to-action] {:key "unique-cta-key"})}
    [:div.wave-background
     [:section.container.mb-5
      [startpage-features/how-does-schnaq-work]
      [testimonials/highlights]
      [three-steps-to-success]
      [startpage-features/feature-rows]]
     [faq]
     [:section.container
      [:h2.text-center.mt-4 (labels :startpage.social-proof/companies)]
      [testimonials/testimonial-companies]
      [early-adopters]
      [cleverreach-form]
      [team-and-supporters]]]]])

(defn startpage-view
  "A view that represents the first page of schnaq participation or creation."
  []
  [startpage-content])
