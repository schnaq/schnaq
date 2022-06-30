(ns schnaq.interface.views.base
  (:require [clojure.string :as str]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.config :as config]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.feedback.collect :as feedback]))

(defn header
  "Build a header with a curly bottom for a page. Heading, subheading and more will be included in the header."
  [{:page/keys [heading subheading classes more-for-heading vertical-header? wrapper-classes]}]
  [:<>
   [:div
    {:class (if (str/blank? wrapper-classes) "container container-85" wrapper-classes)}
    (if vertical-header?
      [:<> [:h1 heading] [:h2.display-6 subheading]]
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
    {:src (img-path :logo-white)
     :alt "schnaq logo"}]
   [:div.lead.fst-italic.pb-1
    (labels :startpage/slogan)]])

(defn- footer-button
  [url content-label]
  [:li.list-inline-item
   [:a.btn.btn-sm.btn-outline-white {:href url}
    (labels content-label)]])

(defn- footer-nav []
  [:<>
   [:ul.list-inline
    [footer-button "https://schnaq.com/code-of-conduct" :coc/heading]
    [footer-button "https://schnaq.com/about" :footer.buttons/about-us]
    [footer-button "https://schnaq.com/press" :footer.buttons/press-kit]
    [footer-button "https://schnaq.com/publications" :footer.buttons/publications]]
   [:ul.list-inline
    [:li.list-inline-item
     [:button.btn.btn-sm.btn-outline-white {:on-click feedback/show-feedback-modal}
      (labels :feedbacks/button)]]
    [footer-button "https://schnaq.com/privacy" :router/privacy]
    [footer-button "https://schnaq.com/legal-note" :footer.buttons/legal-note]]])

(defn- developed-in-nrw []
  [:section.pt-3
   [icon :terminal]
   " " (labels :footer.tagline/developed-with) " "
   [icon :flask "m-auto"]
   (gstring/format " in NRW, Germany © schnaq GmbH %d" (.getFullYear (js/Date.)))])

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
     [:img {:src (img-path :logos/hetzner)
            :alt "Logo of Hetzner"}]]]])

(defn- registered-trademark []
  [:section
   [:small
    (labels :footer.registered/rights-reserved)
    ". schnaq" [:sup "®"] " "
    (labels :footer.registered/is-registered)
    "."]])

(defn- product-use-cases
  "Show schnaq use-cases for the users. Only in german."
  []
  [:section.px-2
   ;; Remove hardcode, when there are english versions around!
   [:h3.h5 "schnaq Lösungen"]
   [:ul {:style {:list-style :none
                 :padding-left 0}}
    [:li
     [:a.btn.btn-link.text-white {:href "https://schnaq.com/blog/de/online-meetings-moderieren/"}
      "für Meetings"]]
    [:li
     [:a.btn.btn-link.text-white {:href "https://schnaq.com/blog/de/online-diskussionsplattform/"}
      "für Diskussionen"]]
    [:li
     [:a.btn.btn-link.text-white {:href "https://schnaq.com/blog/de/ama-ask-me-anything-fragerunden/"}
      "für AMAs"]]
    [:li
     [:a.btn.btn-link.text-white {:href "https://schnaq.com/blog/de/online-workshop-moderation/"}
      "für Online Workshops"]]]])

(defn- alternatives
  "Show schnaq use-cases for the users. Only in german."
  []
  [:section.px-2
   ;; Remove hardcode, when there are german versions around!
   [:h3.h5 "schnaq vs."]
   [:p
    [:a.btn.btn-link.text-white {:href "https://schnaq.com/blog/en/best-alternative-to-slido/"}
     "Alternative to Slido"]]])

;; -----------------------------------------------------------------------------

(defn- footer-common []
  (let [locale @(rf/subscribe [:current-locale])]
    [:footer
     [:div.container-fluid.px-5
      [:div.row
       [:div.col-md-6.col-xl-3.col-12
        [logo-and-slogan]]
       [:div.col-md-6.col-xl-3.col-12
        (if (= :en locale)
          [alternatives]
          [product-use-cases])]
       [:div.col-md-6.col-xl-6.col-12.text-xl-end.pt-3.pt-md-0
        [footer-nav]]]
      [:div.row
       [:div.col-md-6.col-12
        [developed-in-nrw]
        [registered-trademark]]
       [:div.col-md-6.col-12.text-md-end.pt-3.pt-md-0
        [social-media]
        [sponsors]]]]]))

(defn footer
  "Footer to display at the bottom the page."
  []
  (when-not config/in-iframe?
    [footer-common]))

(defn footer-with-wave []
  (when-not config/in-iframe?
    [:<>
     [:div.wave-bottom-typography]
     [footer-common]]))
