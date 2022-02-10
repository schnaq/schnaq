(ns schnaq.interface.views.product.pages
  (:require [reitit.frontend.easy :as rfe]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.videos :refer [video]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.pages :as pages]))

(defn- start-schnaq-button
  "Tell user to create a schnaq now."
  []
  [:section.mt-5
   [buttons/anchor-big
    (labels :schnaq.startpage.cta/button)
    (rfe/href :routes.schnaq/create)
    "btn-dark d-inline-block"]])

(defn product-above-the-fold
  "Displays a list of features with a call-to-action button to start a schnaq"
  [title subtitle]
  [:section.row.mt-3 {:key "HeaderExtras-Bullet-Points-and-Animation"}
   [:div.col-lg-6.my-auto
    [:h1 (labels title)]
    [:p.lead (labels subtitle)]
    [start-schnaq-button]]
   [:div.col-lg-6.pb-4
    [:img.product-page-ipad {:src (img-path :productpage.overview/ipad)}]]])

(defn- feature-text [title text]
  [:<>
   [:div.display-4.text-primary.mb-5 (labels title)]
   [:div.display-6.text-typography (labels text)]])

(defn- feature-image [image]
  [:div
   [:img.taskbar-background {:src (img-path :how-to/taskbar)}]
   [:img.product-page-feature-image.my-auto {:src (img-path image)}]])

(defn- feature-text-img-right [title text image]
  [:div.row.py-5.mt-5
   [:div.col-12.col-lg-6 [feature-text title text]]
   [:div.col-12.col-lg-6.mt-5.mt-lg-0 [:div.me-lg-n5 [feature-image image]]]])

(defn- feature-text-img-left [title text image]
  [:div.row.py-5.mt-5
   [:div.col-12.col-lg-6.d-none.d-lg-block [:div.ms-lg-n5 [feature-image image]]]
   [:div.col-12.col-lg-6 [feature-text title text]]
   [:div.col-12.d-lg-none.mt-5 [feature-image image]]])

(defn- try-schnaq
  "Present early-adopters section to catch up interest."
  []
  (let [cta-video [:video.product-page-cta-video
                   {:auto-play true :loop true :muted true :plays-inline true}
                   [:source {:src (video :register.point-right/webm) :type "video/webm"}]
                   [:source {:src (video :register.point-right/mp4) :type "video/mp4"}]]]
    [:section.container.container-85.mb-5
     [:div.d-flex.flex-row.justify-content-center
      [:div.mt-auto.me-3.d-none.d-lg-block cta-video]
      [:div
       [:div.display-5.text-white.mb-5 (labels :productpage/cta)]
       [:div.d-flex.flex-row
        [:div.mt-auto.me-3.d-lg-none cta-video]
        [:a.btn.btn-lg.btn-dark.my-auto.w-100
         {:role "button"
          :href (rfe/href :routes.schnaq/create)}
         (labels :schnaq.startpage.cta/button)]]]]]))

(defn- product-tour []
  [:div.overflow-hidden
   [pages/with-nav-and-header
    {:page/title (labels :startpage/heading)
     :page/wrapper-classes "container container-85"
     :page/vertical-header? true
     :page/more-for-heading (with-meta [product-above-the-fold
                                        :productpage.overview/title
                                        :productpage.overview/subtitle]
                              {:key "unique-cta-key"})}
    [:div.wave-background
     [:section.container.container-85
      [feature-text-img-right
       :productpage.overview.qa/title
       :productpage.overview.qa/text
       :productpage.overview/qa]
      [feature-text-img-left
       :productpage.overview.poll/title
       :productpage.overview.poll/text
       :productpage.overview/poll]
      [feature-text-img-right
       :productpage.overview.activation/title
       :productpage.overview.activation/text
       :productpage.overview/activation]
      [feature-text-img-left
       :productpage.overview.feedback/title
       :productpage.overview.feedback/text
       :productpage.overview/analysis]]
     [:div.wave-bottom-primary]
     [:div.bg-primary
      [try-schnaq]
      [:div.wave-bottom-typography]]]]])

(defn overview-view []
  [product-tour])
