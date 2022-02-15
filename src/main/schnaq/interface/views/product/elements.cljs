(ns schnaq.interface.views.product.elements
  (:require [reitit.frontend.easy :as rfe]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.components.videos :refer [video]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.pages :as pages]))

(defn- start-schnaq-button
  "Tell user to create a schnaq now."
  [button-label]
  [:section.mt-5
   [buttons/anchor-big
    (labels button-label)
    (rfe/href :routes.schnaq/create)
    "btn-dark d-inline-block"]])

(defn product-above-the-fold
  "Displays a list of features with a call-to-action button to start a schnaq"
  [title subtitle cta-button-label]
  [:section.row.mt-3 {:key "HeaderExtras-Bullet-Points-and-Animation"}
   [:div.col-lg-6.my-auto
    [:h1 (labels title)]
    [:p.lead (labels subtitle)]
    [start-schnaq-button cta-button-label]]
   [:div.col-lg-6.pb-4
    [:img.product-page-ipad {:src (img-path :productpage.overview/ipad)}]]])

(defn- feature-text [title text]
  [:<>
   [:div.display-4.text-primary.mb-5 (labels title)]
   [:div.display-6.text-typography (labels text)]])

(defn find-out-more-link [link]
  [:div.mt-5
   [:a.display-6 {:href (rfe/href link)}
    (labels :productpage/learn-more)]])

(defn available-soon
  "Displays an 'available soon' text."
  []
  [:div.display-6.text-muted.mt-5 (labels :productpage/available-soon)])

(defn- feature-image [image]
  [:div
   [:img.taskbar-background {:src (img-path :how-to/taskbar)}]
   [motion/zoom-image
    {:class "product-page-feature-image my-auto"
     :src (img-path image)}]])

(defn feature-text-img-right
  "Row with text and caption on the left and image on the right."
  [title text image more-content]
  [:div.row.py-5.mt-5
   [:div.col-12.col-lg-6 [feature-text title text] more-content]
   [:div.col-12.col-lg-6.mt-5.mt-lg-0 [:div.me-lg-n5 [feature-image image]]]])

(defn feature-text-img-left
  "Row with text and caption on the right and image on the left."
  [title text image more-content]
  [:div.row.py-5.mt-5
   [:div.col-12.col-lg-6.d-none.d-lg-block [:div.ms-lg-n5 [feature-image image]]]
   [:div.col-12.col-lg-6 [feature-text title text] more-content]
   [:div.col-12.d-lg-none.mt-5 [feature-image image]]])

(defn qa-feature-row
  "First Q&A feature row with a phone and website mock-up."
  []
  (let [title-focus :productpage.qa.focus/title
        text-focus :productpage.qa.focus/subtitle
        image-focus :productpage.qa/phone
        title-overview :productpage.qa.overview/title
        text-overview :productpage.qa.overview/subtitle
        image-overview :productpage.qa/overview
        subtitle-focus [:div.display-6.text-typography (labels text-focus)]
        subtitle-overview [:div.display-6.text-typography (labels text-overview)]]
    [:<>
     [:div.row.py-5.mt-5
      [:div.col-12.col-lg-6.px-5
       [:div.display-4.text-primary.mb-5 (labels title-focus)]
       [:div.d-lg-none subtitle-focus]]
      [:div.col-12.col-lg-6.px-5.mt-5.mt-lg-0
       [:div.display-4.text-primary.mb-5 (labels title-overview)]]
      [:div.col-12.col-lg-6.px-5
       subtitle-focus
       [:div.d-lg-none subtitle-overview]]
      [:div.col-12.col-lg-6.px-5.d-none.d-lg-block
       subtitle-overview]]
     [:div.row.mt-3.px-5
      [:div.col-3.d-flex
       [:div.align-self-end
        [:img.product-page-qa-phone {:src (img-path image-focus)}]]]
      [:div.col-9
       [feature-image image-overview]]]]))

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
        [:a.btn.btn-lg.btn-dark.my-auto
         {:role "button"
          :href (rfe/href :routes.schnaq/create)}
         (labels :schnaq.startpage.cta/button)]]]]]))

(defn product-page
  "Product page skeleton with a title and subtitle next to an tablet as ATF."
  [title subtitle cta-button-label content]
  [:div.overflow-hidden
   [pages/with-nav-and-header
    {:page/title (labels :startpage/heading)
     :page/wrapper-classes "container container-85"
     :page/vertical-header? true
     :page/more-for-heading (with-meta [product-above-the-fold
                                        title
                                        subtitle
                                        cta-button-label]
                              {:key "unique-cta-key"})}
    [:div.wave-background
     [:section.container.container-85
      content]
     [:div.wave-bottom-primary]
     [:div.bg-primary
      [try-schnaq]
      [:div.wave-bottom-typography]]]]])
