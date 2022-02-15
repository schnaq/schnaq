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
   [:img.rounded-circle.social-proof-img.me-2 {:src (img-path :testimonial-picture/bjorn)}]
   [:img.rounded-circle.social-proof-img.me-2 {:src (img-path :testimonial-picture/frauke-kling)}]
   [:img.rounded-circle.social-proof-img.me-2 {:src (img-path :testimonial-picture/lokay)}]
   [:img.rounded-circle.social-proof-img.me-2 {:src (img-path :testimonial-picture/frank-stampa)}]
   [icon :plus "my-auto me-2"]
   [:div.border-end.me-2.d-inline-block]
   [:p.small.my-auto (labels :startpage.social-proof/teaser)]])

(defn features-call-to-action
  "Displays a list of features with a call-to-action button to start a schnaq"
  []
  [:section.row.mt-3 {:key "HeaderExtras-Bullet-Points-and-Animation"}
   [:div.col-lg-6.col-xxxl-7.py-lg-4.pe-lg-5.my-auto
    [:h1 (labels :startpage/subheading)]
    [:p.lead (labels :startpage/hook)]
    [start-schnaq-button]
    [social-proof-abtf]]
   [:div.col-lg-6.col-xxxl-4.pb-4
    [header-video]]])
