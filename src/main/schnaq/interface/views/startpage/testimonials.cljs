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


;; -----------------------------------------------------------------------------

(defn view
  "Show all testimonials."
  []
  [:section.pb-5.pt-3
   [:p.h4.text-center.pb-4
    (labels :testimonials/heading)]
   [doctronic]])