(ns schnaq.interface.views.startpage.testimonials
  (:require [goog.string :as gstring]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.translations :refer [labels]]))

(defn- testimonial-card
  "A single testimonial of a user."
  [company-name company-body reference-name avatar]
  [:div.p-4.testimonial
   (when avatar
     [:img.w-50.rounded-50
      {:src (img-path avatar)
       :alt (gstring/format "A picture of %s" (labels avatar))}])
   [:div.card-text.text-typography.my-3
    [:div.small (labels reference-name)]
    [:div.small.mt-3 (labels company-name)]]
   [:div.card-body.p-0
    [:div.d-flex.flex-row
     [:div.display-5.text-primary.mr-1.mt-n2 "\""]
     [:p.card-text.text-primary (labels company-body)]]]])

;; Testimonials can be used in the separate testimonials page soon
(defn- testimonial-column-1
  "Columns displaying the testimonials of our users."
  []
  [:div.card-deck
   [testimonial-card
    :testimonials.doctronic/company
    :testimonials.doctronic/quote
    :testimonials.doctronic/author
    :testimonial-picture/ingo-kupers]
   [testimonial-card
    :testimonials.leetdesk/company
    :testimonials.leetdesk/quote
    :testimonials.leetdesk/author
    :testimonial-picture/meiko-tse]
   [testimonial-card
    :testimonials.hhu/company
    :testimonials.bialon/quote
    :testimonials.bialon/author
    :testimonial-picture/raphael-bialon]])

(defn- testimonial-column-2
  "Columns displaying the second set of testimonials of our users."
  []
  [:div.card-deck
   [testimonial-card
    :testimonials.lokay/company
    :testimonials.lokay/quote
    :testimonials.lokay/author
    :testimonial-picture/lokay]
   [testimonial-card
    :testimonials.hck/company
    :testimonials.hck/quote
    :testimonials.hck/author
    :testimonial-picture/hck]
   [testimonial-card
    :testimonials.franky/company
    :testimonials.franky/quote
    :testimonials.franky/author
    :testimonial-picture/frank-stampa]])

(defn- testimonial-column-3
  "Columns displaying the third set of testimonials of our users."
  []
  [:div.card-deck
   [testimonial-card
    :testimonials.metro/company
    :testimonials.metro/quote
    :testimonials.metro/author
    :testimonial-picture/tobias-schroeder]
   [testimonial-card
    :testimonials.hhu/company
    :testimonials.bjorn/quote
    :testimonials.bjorn/author
    :testimonial-picture/bjorn]
   [testimonial-card
    :testimonials.bib/company
    :testimonials.bib/quote
    :testimonials.bib/author
    :testimonial-picture/frauke-kling]])

(defn- testimonial-column-4
  "Columns displaying the fourth set of testimonials of our users."
  []
  [:div.card-deck
   [testimonial-card
    :testimonials.eugenbialon/company
    :testimonials.eugenbialon/quote
    :testimonials.eugenbialon/author
    :testimonial-picture/eugen-bialon]
   [testimonial-card
    :testimonials.sensor/company
    :testimonials.sensor/quote
    :testimonials.sensor/author
    :testimonial-picture/florian-clever]])

(defn- company-logo [logo company-name]
  [:article
   [:img.testimonial-logo.p-4.w-100
    {:src (img-path logo)
     :alt (gstring/format "A company logo of %s" (labels company-name))}]])

(defn testimonial-companies []
  [:div.d-flex.flex-wrap.mb-5
   [company-logo :logos/doctronic :testimonials.doctronic/company]
   [company-logo :logos/franky :testimonials.franky/company]
   [company-logo :logos/metro :testimonials.metro/company]
   [company-logo :logos/hhu :testimonials.hhu/company]
   [company-logo :logos/leetdesk :testimonials.leetdesk/company]])

(defn highlights
  "A few highlight-testimonials featured on the startpage."
  []
  [:div.row.mt-5
   [:div.col-12.col-lg-6.p-2
    [:div.row
     [:div.col-4
      [:img.w-100.rounded-50
       {:src (img-path :testimonial-picture/frauke-kling)
        :alt (gstring/format "A picture of %s" (labels :testimonial-picture/frauke-kling))}]
      [:div.text-typography.my-3.text-center
       [:div.small (labels :testimonials.bib/author)]]]
     [:div.col-6.my-auto.mt-lg-0.mb-lg-auto
      [:div.card-body.p-0
       [:div.d-flex.flex-row
        [:div.display-5.text-primary.mr-1.mt-n2 "\""]
        [:p.card-text.text-primary (labels :testimonials.bib/quote)]]]]]]
   [:div.col-12.col-lg-6.p-2
    [:div.row
     [:div.col-6.my-auto.mb-lg-0.mt-lg-auto
      [:div.card-body.p-0
       [:div.d-flex.flex-row
        [:div.display-5.text-primary.mr-1.mt-n2 "\""]
        [:p.card-text.text-primary (labels :testimonials.lokay/quote)]]]]
     [:div.col-4.mt-lg-auto
      [:img.w-100.rounded-50
       {:src (img-path :testimonial-picture/lokay)
        :alt (gstring/format "A picture of %s" (labels :testimonial-picture/lokay))}]
      [:div.text-typography.mt-3.text-center
       [:div.small (labels :testimonials.lokay/author)]]]]]])
