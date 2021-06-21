(ns schnaq.interface.views.startpage.testimonials
  (:require [goog.string :as gstring]
            [schnaq.interface.text.display-data :refer [labels img-path]]))

(defn- testimonial-card
  "A single testimonial of a user."
  [img company-name company-body reference-name]
  [:div.card.p-4.testimonial.shadow
   [:img.card-img-top.p-4
    {:src (img-path img)
     :alt (gstring/format "A company logo of %s" (labels company-name))}]
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