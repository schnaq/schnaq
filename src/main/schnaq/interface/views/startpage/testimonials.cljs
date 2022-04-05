(ns schnaq.interface.views.startpage.testimonials
  (:require [goog.string :as gstring]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.translations :refer [labels]]))

(defn- company-logo [logo company-name]
  [:article
   [:img.testimonial-logo.p-4.w-100
    {:src (img-path logo)
     :alt (gstring/format "A company logo of %s" (labels company-name))}]])

(defn testimonial-companies []
  [:div.d-flex.flex-wrap.mb-5.justify-content-center
   [company-logo :logos/digihub :testimonials.digihub/company]
   [company-logo :logos/hhu :testimonials.hhu/company]
   [company-logo :logos/metro :testimonials.metro/company]
   [company-logo :logos/franky :testimonials.franky/company]
   [company-logo :logos/muetze :testimonials.muetze/company]
   [company-logo :logos/doctronic :testimonials.doctronic/company]
   [company-logo :logos/leetdesk :testimonials.leetdesk/company]])

(defn highlights
  "A few highlight-testimonials featured on the startpage."
  []
  [:div.row.mt-5.py-lg-5
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
        [:div.display-5.text-primary.me-1.mt-n2 "\""]
        [:p.card-text.text-primary (labels :testimonials.bib/quote)]]]]]]
   [:div.col-12.col-lg-6.p-2
    [:div.row.startpage-step-3
     [:div.col-6.my-auto.mb-lg-0.mt-lg-auto
      [:div.card-body.p-0
       [:div.d-flex.flex-row
        [:div.display-5.text-primary.me-1.mt-n2 "\""]
        [:p.card-text.text-primary (labels :testimonials.lokay/quote)]]]]
     [:div.col-4.mt-lg-auto
      [:img.w-100.rounded-50
       {:src (img-path :testimonial-picture/lokay)
        :alt (gstring/format "A picture of %s" (labels :testimonial-picture/lokay))}]
      [:div.text-typography.mt-3.text-center
       [:div.small (labels :testimonials.lokay/author)]]]]]])
