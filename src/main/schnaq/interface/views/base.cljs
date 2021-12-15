(ns schnaq.interface.views.base
  (:require [clojure.string :as str]
            [goog.string :as gstring]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as jw]
            [schnaq.interface.views.feedback.collect :as feedback]))

(defn header
  "Build a header with a curly bottom for a page. Heading, subheading and more will be included in the header."
  [{:page/keys [heading subheading classes more-for-heading vertical-header? wrapper-classes]}]
  [:<>
   [:div
    {:class (if (str/blank? wrapper-classes) "container" wrapper-classes)}
    (if vertical-header?
      [:<> [:h1 heading] [:h4.display-6 subheading]]
      [:div.row.mt-5.mb-2
       ;; If split header is configured, but the screen is too small, display
       ;; the headings one below the other
       [:div.col-12.col-md-6 [:h1 heading]]
       [:div.col-12.col-md-6 [:h2.h4 subheading]]])
    more-for-heading]
   (cond
     (gstring/contains (str classes) "bg-white") [:div.wave-bottom-white]
     (gstring/contains (str classes) "bg-primary") [:div.wave-bottom-primary]
     (gstring/contains (str classes) "bg-typography") [:div.wave-bottom-typography]
     (gstring/contains (str classes) "bg-dark") [:div.wave-bottom-dark]
     :else [:div.wave-bottom-light])])

;; -----------------------------------------------------------------------------
;; Footer

(defn- logo-and-slogan []
  [:<>
   [:img.footer-schnaqqifant
    {:src (img-path :logo-white)}]
   [:div.lead.font-italic.pb-1
    (labels :startpage/heading)]])

(defn- footer-button
  [route-name content-label]
  [:li.list-inline-item
   [:a.btn.btn-sm.btn-outline-white {:href (reitfe/href route-name)}
    (labels content-label)]])

(defn- footer-nav []
  [:<>
   [:ul.list-inline
    [footer-button :routes/code-of-conduct :coc/heading]
    [footer-button :routes/about-us :footer.buttons/about-us]
    [footer-button :routes/press :footer.buttons/press-kit]
    [footer-button :routes/publications :footer.buttons/publications]]
   [:ul.list-inline
    [:li.list-inline-item
     [:button.btn.btn-sm.btn-outline-white {:on-click feedback/show-feedback-modal}
      (labels :feedbacks/button)]]
    [footer-button :routes/privacy :router/privacy]
    [footer-button :routes/legal-note :footer.buttons/legal-note]
    [footer-button :routes/alphazulu :router/alphazulu]]])

(defn- developed-in-nrw []
  [:section.pt-3
   [icon :terminal]
   " " (labels :footer.tagline/developed-with) " "
   [icon :flask "m-auto"]
   (gstring/format " in NRW, Germany © schnaq %d" jw/get-date-year)])

(defn- social-media []
  [:section
   [:a.social-media-icon {:href "https://facebook.com/schnaq" :target :_blank}
    [icon :facebook "" {:size "2x"}]]
   [:a.social-media-icon {:href "https://instagram.com/schnaqqi" :target :_blank}
    [icon :instagram "" {:size "2x"}]]
   [:a.social-media-icon {:href "https://www.linkedin.com/company/schnaq" :target :_blank}
    [icon :linkedin "" {:size "2x"}]]
   [:a.social-media-icon {:href "https://twitter.com/getschnaq" :target :_blank}
    [icon :twitter "" {:size "2x"}]]
   [:a.social-media-icon {:href "https://github.com/schnaq" :target :_blank}
    [icon :github "" {:size "2x"}]]])

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
