(ns schnaq.interface.views.startpage.testimonials
  (:require [goog.string :as gstring]
            [schnaq.interface.text.display-data :refer [labels img-path]]))

(defn- doctronic
  "Add quote from doctronic."
  []
  [:div.row.testimonial
   [:div.col-md-8.col-12
    [:div.shadow.p-4.bg-light.rounded
     [:blockquote.blockquote.text-center
      [:p.mb-0
       (gstring/format "\"%s\"" (labels :testimonials.doctronic/quote))]
      [:div.blockquote-footer
       (labels :testimonials.doctronic/author)]]]]

   [:div.col-md-4.col-12.d-flex.align-items-center
    [:a {:href "https://doctronic.de" :target :_blank}
     [:img
      {:src (img-path :logos/doctronic)
       :alt "doctronic logo"}]]]])

(defn- testimonial-card
  "A single testimonial of a user."
  [img company-name company-body reference-name]
  [:div.card.p-4
   [:img.card-img-top {:src (img-path img) :alt "todo"}]
   [:div.card-body
    [:h5.card-title.text-center (labels company-name)]
    [:p.card-text (gstring/format "\"%s\"" (labels company-body))]
    [:p.card-text.text-right [:small.text-muted "- " (labels reference-name)]]]])

(defn- testimonial-columns
  "Columns displaying the testimonials of our users."
  []
  [:div.card-deck
   [testimonial-card
    :logos/doctronic
    :testimonials.doctronic/company
    :testimonials.doctronic/quote
    :testimonials.doctronic/author]
   [testimonial-card
    :logos/leetdesk
    :testimonials.leetdesk/company
    :testimonials.leetdesk/quote
    :testimonials.leetdesk/author]
   [testimonial-card
    :logos/hhu
    :testimonials.hhu/company
    :testimonials.hhu/quote
    :testimonials.hhu/author]])


;; -----------------------------------------------------------------------------

(defn view
  "Show all testimonials."
  []
  [:section.pb-5.pt-3
   [:p.h4.text-center.pb-4
    (labels :testimonials/heading)]
   [testimonial-columns]])