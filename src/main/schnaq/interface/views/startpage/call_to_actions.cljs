(ns schnaq.interface.views.startpage.call-to-actions
  (:require [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]))

(defn- header-video
  "Display header screenshot of an example discussion."
  []
  [:section.rounded-5.shadow
   [:div.d-inline-block.bg-ipad
    [:video.video-header
     {:auto-play true :loop true :muted true :plays-inline true}
     [:source {:src (labels :startpage.above-the-fold/webm) :type "video/webm"}]
     [:source {:src (labels :startpage.above-the-fold/mp4) :type "video/mp4"}]]]])

(defn- start-schnaq-button
  "Tell user to create a schnaq now."
  []
  [:section.mt-5.text-center
   [buttons/anchor-big
    (labels :schnaq.startpage.cta/button)
    (navigation/href :routes.schnaq/create)
    "btn-secondary d-inline-block"]])

(defn- social-proof-abtf
  "Social proof above the fold, showing other people use schnaq."
  []
  [:div.d-flex.mt-5.mb-5.mb-lg-0
   [:img.rounded-circle.social-proof-img.me-2 {:src (img-path :testimonial-picture/bjorn)
                                               :alt (labels :testimonial-picture/alt-text)}]
   [:img.rounded-circle.social-proof-img.me-2 {:src (img-path :testimonial-picture/frauke-kling)
                                               :alt (labels :testimonial-picture/alt-text)}]
   [:img.rounded-circle.social-proof-img.me-2 {:src (img-path :testimonial-picture/lokay)
                                               :alt (labels :testimonial-picture/alt-text)}]
   [:img.rounded-circle.social-proof-img.me-2 {:src (img-path :testimonial-picture/frank-stampa)
                                               :alt (labels :testimonial-picture/alt-text)}]
   [icon :plus "my-auto me-2"]
   [:div.border-end.me-2.d-inline-block]
   [:p.small.my-auto (labels :startpage.social-proof/teaser)]])

(defn- trust-badges
  "An assortment of badges, that exude trust to the visiting user."
  []
  [:div.row.pt-5
   [:div.col-3
    [:a {:href "https://www.capterra.com/reviews/245761/schnaq?utm_source=vendor&utm_medium=badge&utm_campaign=capterra_reviews_badge"}
     [:img.img-fluid
      {:src "https://assets.capterra.com/badge/003723140a5adf622bfdb2e12c2118d7.svg?v=2203283&p=245761"
       :alt (labels :startpage.trust/capterra)}]]]
   [:div.col-3.d-flex.align-items-center
    [:img.img-fluid
     {:src (img-path :startpage.trust/germany-100-white)
      :alt (labels :startpage.trust/germany-100)}]]])

(defn features-call-to-action
  "Displays a list of features with a call-to-action button to start a schnaq"
  []
  [:section.row.mt-3 {:key "HeaderExtras-Bullet-Points-and-Animation"}
   [:div.col-lg-6.col-xxxl-7.py-lg-4.pe-lg-5.my-auto
    [:h1 (labels :startpage/slogan)]
    [:p.lead (labels :startpage/hook)]
    [start-schnaq-button]
    [social-proof-abtf]
    [trust-badges]]
   [:div.col-lg-6.col-xxxl-4.pb-4
    [header-video]]])
