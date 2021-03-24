(ns schnaq.interface.views.base
  (:require [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]))

(defn wavy-curve
  "Define a wavy curve."
  ([]
   [wavy-curve ""])
  ([transformation]
   ;; bezier curves
   [:svg.wavy-curve
    {:xmlSpace "preserve"
     :overflow :auto
     :viewBox "0 0 1440 87"
     :style {:transform transformation
             :-webkit-transform transformation}
     :y "0px"
     :x "0px"}
    [:path {:d "M0,87h48c48,0,144,0,240-11.6c96-11.8,192-34.6,288-43.5c96-8.5,192-3.1,288,8.7c96,11.6,192,29,288,29 s192-17.4,240-26.1l48-8.7V0h-48c-48,0-144,0-240,0S960,0,864,0S672,0,576,0S384,0,288,0S96,0,48,0H0V87z"}]]))

(defn header
  "Build a header with a curly bottom for a page. Heading, subheading and more will be included in the header."
  [heading subheading & more]
  [:div.pb-5
   [:header.masthead.text-white
    [:div.container
     [:h1 heading]
     [:h2.display-6 subheading]
     more]]
   [wavy-curve]])

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
  [:ul.list-inline
   [:li.list-inline-item
    [:a.btn.btn-outline-white {:href (reitfe/href :routes/code-of-conduct)}
     (labels :coc/heading)]]
   [:li.list-inline-item
    [:a.btn.btn-outline-white {:href "https://disqtec.com/ueber-uns"}
     (labels :footer.buttons/about-us)]]
   [:li.list-inline-item
    [:a.btn.btn-outline-white
     {:role "button" :href (reitfe/href :routes/privacy)}
     (labels :router/privacy)]]
   [:li.list-inline-item
    [:a.btn.btn-outline-white
     {:href "https://disqtec.com/impressum"}
     (labels :footer.buttons/legal-note)]]])

(defn- developed-in-nrw []
  [:p.pt-3
   [:i {:class (str "fas " (fa :terminal))}]
   (labels :footer.tagline/developed-with)
   [:i {:class (str "m-auto fas " (fa :flask))}]
   " in NRW, Germany. "
   "© " [:a {:href "https://disqtec.com"
             :target :_blank} "DisqTec"]
   " "
   (.getFullYear (js/Date.))])

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

(defn footer
  "Footer to display at the bottom the page."
  []
  [:footer
   [:div.container
    [:div.row
     [:div.col-md-4.col-12
      [logo-and-slogan]]
     [:div.col-md-8.col-12.text-md-right.pt-3.pt-md-0
      [footer-nav]
      [social-media]]]
    [:div.row
     [:div.col-md-6.col-12
      [developed-in-nrw]]
     [:div.col-md-6.col-12.text-md-right.pt-3.pt-md-0
      [sponsors]]]]])
