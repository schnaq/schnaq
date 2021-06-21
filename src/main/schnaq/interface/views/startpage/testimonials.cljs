(ns schnaq.interface.views.startpage.testimonials
  (:require [goog.string :as gstring]
            [schnaq.interface.text.display-data :refer [labels img-path]]))

(defn- testimonial-card
  "A single testimonial of a user."
  [img company-name company-body reference-name avatar]
  [:div.card.p-4.testimonial.shadow
   (when img
     [:img.testimonial-logo.card-img-top.p-4
      {:src (img-path img)
       :alt (gstring/format "A company logo of %s" (labels company-name))}])
   [:div.card-body
    [:h5.card-title.text-center (labels company-name)]
    [:p.card-text (gstring/format "\"%s\"" (labels company-body))]
    [:div.row
     (when avatar
       [:div.col-5
        [:img.w-100.rounded-50
         {:src (img-path avatar)
          :alt (gstring/format "A picture of %s" (labels avatar))}]])
     [:div.col
      [:p.card-text [:small.text-muted (labels reference-name)]]]]]])

(defn- testimonial-column-1
  "Columns displaying the testimonials of our users."
  []
  [:div.card-deck
   [testimonial-card
    :logos/doctronic
    :testimonials.doctronic/company
    :testimonials.doctronic/quote
    :testimonials.doctronic/author
    :testimonial-picture/ingo-kupers]
   [testimonial-card
    :logos/leetdesk
    :testimonials.leetdesk/company
    :testimonials.leetdesk/quote
    :testimonials.leetdesk/author
    :testimonial-picture/meiko-tse]
   [testimonial-card
    :logos/hhu
    :testimonials.hhu/company
    :testimonials.bialon/quote
    :testimonials.bialon/author
    :testimonial-picture/raphael-bialon]])

(defn- testimonial-column-2
  "Columns displaying the second set of testimonials of our users."
  []
  [:div.card-deck
   [testimonial-card
    :logos/lokay
    :testimonials.lokay/company
    :testimonials.lokay/quote
    :testimonials.lokay/author
    :testimonial-picture/lokay]
   [testimonial-card
    :logos/hck
    :testimonials.hck/company
    :testimonials.hck/quote
    :testimonials.hck/author
    :testimonial-picture/hck]
   [testimonial-card
    :logos/franky
    :testimonials.franky/company
    :testimonials.franky/quote
    :testimonials.franky/author
    :testimonial-picture/frank-stampa]])

(defn- testimonial-column-3
  "Columns displaying the third set of testimonials of our users."
  []
  [:div.card-deck
   [testimonial-card
    :logos/metro
    :testimonials.metro/company
    :testimonials.metro/quote
    :testimonials.metro/author
    :testimonial-picture/tobias-schroeder]
   [testimonial-card
    :logos/hhu
    :testimonials.hhu/company
    :testimonials.bjorn/quote
    :testimonials.bjorn/author
    :testimonial-picture/bjorn]
   [testimonial-card
    :logos/frauke
    :testimonials.bib/company
    :testimonials.bib/quote
    :testimonials.bib/author
    :testimonial-picture/frauke-kling]])

(defn- testimonial-column-4
  "Columns displaying the fourth set of testimonials of our users."
  []
  [:div.card-deck
   [testimonial-card
    :logos/bialon
    :testimonials.eugenbialon/company
    :testimonials.eugenbialon/quote
    :testimonials.eugenbialon/author
    :testimonial-picture/eugen-bialon]
   [testimonial-card
    :logos/sensor
    :testimonials.sensor/company
    :testimonials.sensor/quote
    :testimonials.sensor/author]])

(defn testimonial-carousel []
  [:div#carouselTestimonialIndicators.carousel-testimonial.carousel.slide {:data-ride "carousel"}
   [:ol.carousel-indicators.carousel-indicator-testimonial
    [:li.active {:data-target "#carouselTestimonialIndicators" :data-slide-to "0"}]
    [:li {:data-target "#carouselTestimonialIndicators" :data-slide-to "1"}]
    [:li {:data-target "#carouselTestimonialIndicators" :data-slide-to "2"}]
    [:li {:data-target "#carouselTestimonialIndicators" :data-slide-to "3"}]]
   [:div.carousel-inner.p-md-4
    [:div.carousel-item.active
     [testimonial-column-1]]
    [:div.carousel-item
     [testimonial-column-2]]
    [:div.carousel-item
     [testimonial-column-3]]
    [:div.carousel-item
     [testimonial-column-4]]]
   [:a.carousel-control-prev.carousel-control-testimonial {:href "#carouselTestimonialIndicators" :role "button" :data-slide "prev"}
    [:span.carousel-control-prev-icon.carousel-control-color {:aria-hidden "true"}]
    [:span.sr-only "Previous"]]
   [:a.carousel-control-next.carousel-control-testimonial {:href "#carouselTestimonialIndicators" :role "button" :data-slide "next"}
    [:span.carousel-control-next-icon.carousel-control-color {:aria-hidden "true"}]
    [:span.sr-only "Next"]]])


;; -----------------------------------------------------------------------------

(defn view
  "Show all testimonials."
  []
  [:section.pb-5.pt-3
   [:p.h4.text-center.pb-4
    (labels :testimonials/heading)]
   [testimonial-carousel]])