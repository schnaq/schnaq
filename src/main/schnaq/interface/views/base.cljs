(ns schnaq.interface.views.base
  (:require [goog.string :as gstring]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]))

(defn wavy-curve
  "Define a wavy curve."
  ([]
   [wavy-curve "" false])
  ([transformation]
   [wavy-curve transformation false])
  ([transformation gradient?]
   (let [svg-class (if gradient? "wavy-curve-gradient" "wavy-curve")]
     [:svg
      {:class svg-class
       :xmlSpace "preserve"
       :overflow :auto
       :viewBox "0 0 19 4"
       :preserveAspectRatio :none
       :style {:transform transformation
               :-webkit-transform transformation}
       :y "0px"
       :x "0px"}
      [:path {:d "M0 0 L 0 3 Q 3 3, 6 2 T 12 2 T 19 2 L 19 0"}]])))

(defn header
  "Build a header with a curly bottom for a page. Heading, subheading and more will be included in the header."
  [heading subheading gradient? & more]
  [:div
   [:div.text-white.masthead-inno
    [:div.container
     [:h1 heading]
     [:h2.display-6 subheading]
     more]
    [:div.wave-bottom-light]]])

(defn img-bullet-subtext
  "Create one icon in a grid"
  [path-to-img heading subheading]
  [:div.d-flex.flex-row.p-1
   [:div [:img {:src path-to-img}]]
   [:p.h4 heading]
   [:p subheading]])


;; -----------------------------------------------------------------------------
;; Footer

(defn- logo-and-slogan []
  [:<>
   [:img.footer-schnaqqifant
    {:src (img-path :logo-white)}]
   [:div.lead.font-italic.pb-1
    (labels :startpage/heading)]])

(defn- footer-nav []
  [:<>
   [:ul.list-inline
    [:li.list-inline-item
     [:a.btn.btn-sm.btn-outline-white {:href (reitfe/href :routes/how-to)}
      (labels :router/how-to)]]
    [:li.list-inline-item
     [:a.btn.btn-sm.btn-outline-white {:href (reitfe/href :routes/code-of-conduct)}
      (labels :coc/heading)]]
    [:li.list-inline-item
     [:a.btn.btn-sm.btn-outline-white {:href (reitfe/href :routes/about-us)}
      (labels :footer.buttons/about-us)]]
    [:li.list-inline-item
     [:a.btn.btn-sm.btn-outline-white {:href (reitfe/href :routes/press)}
      (labels :footer.buttons/press-kit)]]
    [:li.list-inline-item
     [:a.btn.btn-sm.btn-outline-white
      {:href (reitfe/href :routes/publications)}
      (labels :footer.buttons/publications)]]]
   [:ul.list-inline
    [:li.list-inline-item
     [:a.btn.btn-sm.btn-outline-white
      {:role "button" :href (reitfe/href :routes/privacy)}
      (labels :router/privacy)]]
    [:li.list-inline-item
     [:a.btn.btn-sm.btn-outline-white
      {:href (reitfe/href :routes/legal-note)}
      (labels :footer.buttons/legal-note)]]]])

(defn- developed-in-nrw []
  [:section.pt-3
   [:i {:class (str "fas " (fa :terminal))}]
   (labels :footer.tagline/developed-with)
   [:i {:class (str "m-auto fas " (fa :flask))}]
   (gstring/format " in NRW, Germany © schnaq %d" (.getFullYear (js/Date.)))])

(defn- social-media []
  [:section
   [:a.social-media-icon {:href "https://facebook.com/schnaq" :target :_blank}
    [:i.fa-2x.fab.fa-facebook]]
   [:a.social-media-icon {:href "https://instagram.com/schnaqqi" :target :_blank}
    [:i.fa-2x.fab.fa-instagram]]
   [:a.social-media-icon {:href "https://www.linkedin.com/company/schnaq" :target :_blank}
    [:i.fa-2x.fab.fa-linkedin]]
   [:a.social-media-icon {:href "https://twitter.com/getschnaq" :target :_blank}
    [:i.fa-2x.fab.fa-twitter]]
   [:a.social-media-icon {:href "https://github.com/schnaq" :target :_blank}
    [:i.fa-2x.fab.fa-github]]])

(defn- sponsors []
  [:section.sponsors
   [:small (labels :footer.sponsors/heading)]
   [:article
    [:a {:href "https://www.hetzner.com/cloud" :target :_blank}
     [:img {:src (img-path :logos/hetzner)}]]]])

(defn- registered-trademark []
  [:section
   [:small
    (labels :footer.registered/rights-reserved)
    ". schnaq" [:sup "®"] " "
    (labels :footer.registered/is-registered)
    "."]])


;; -----------------------------------------------------------------------------

(defn footer
  "Footer to display at the bottom the page."
  []
  [:footer
   [:div.container
    [:div.row
     [:div.col-md-4.col-12
      [logo-and-slogan]]
     [:div.col-md-8.col-12.text-md-right.pt-3.pt-md-0
      [footer-nav]]]
    [:div.row
     [:div.col-md-6.col-12
      [developed-in-nrw]
      [registered-trademark]]
     [:div.col-md-6.col-12.text-md-right.pt-3.pt-md-0
      [social-media]
      [sponsors]]]]])
