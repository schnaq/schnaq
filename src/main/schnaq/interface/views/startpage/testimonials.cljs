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
