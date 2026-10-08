(ns schnaq.interface.views.base
  (:require [clojure.string :as str]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.config :as config]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.feedback.collect :refer [feedback-modal]]
            [schnaq.links :as links]))

(defn header
  "Build a header with a curly bottom for a page. Heading, subheading and more will be included in the header."
  [{:page/keys [heading subheading classes more-for-heading vertical-header? wrapper-classes]}]
  [:<>
   [:div
    {:class (if (str/blank? wrapper-classes) "container container-85" wrapper-classes)}
    (if vertical-header?
      [:<> (when heading [:h1 heading]) (when subheading [:h2.display-6 subheading])]
      [:div.row.mt-md-5.mb-2
       ;; If split header is configured, but the screen is too small, display
       ;; the headings one below the other
       [:div.col-12.col-md-6 (when heading [:h1 heading])]
       [:div.col-12.col-md-6 (when subheading [:h2.h4 subheading])]])
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

(defn- footer-link
  [url content-label]
  [:li.list-inline-item
   [:a {:href url}
    (labels content-label)]])

(defn- footer-nav [locale]
  [:ul.list-inline
   [footer-link (str "https://schnaq.com/" (links/site-language locale) "/about") :footer.buttons/about-us]
   [:li.list-inline-item
    [feedback-modal
     (fn [props] [:button.btn.btn-link props (labels :feedbacks/button)])]]
   [footer-link (links/privacy-policy locale) :router/privacy]
   [footer-link (links/legal-note locale) :footer.buttons/legal-note]])

(defn- developed-in-nrw []
  [:section.pt-3
   [icon :terminal]
   " " (labels :footer.tagline/developed-with) " "
   [icon :flask "m-auto"]
   " " (labels :footer.tagline/location)
   ;; Keep the notice in one piece, so the © never ends a line on its own.
   " " [:span.text-nowrap (gstring/format "© schnaq GmbH %d" (.getFullYear (js/Date.)))]])

(defn- social-link
  "Icon-only link to one of our profiles, named by the brand."
  [href brand icon-key]
  [:a.social-media-icon {:href href :target :_blank :rel "noopener noreferrer" :aria-label brand}
   [icon icon-key "" {:size "2x"}]])

(defn- social-media []
  [:section
   [social-link "https://www.linkedin.com/company/schnaq" "LinkedIn" :linkedin]
   [social-link "https://github.com/schnaq" "GitHub" :github]])

(defn- versions
  "Show the deployed versions. Frontend and backend are released separately."
  []
  (let [backend-version @(rf/subscribe [:app.version/backend])]
    [:section.pt-2
     ;; Muted by opacity: the footer is dark, .text-muted would be navy on navy.
     [:small.opacity-75
      "Version " config/app-version
      (when backend-version (str " · API " backend-version))]]))

(defn- registered-trademark []
  [:section
   [:small
    (labels :footer.registered/rights-reserved)
    ". schnaq" [:sup "®"] " "
    (labels :footer.registered/is-registered)
    "."]])

;; -----------------------------------------------------------------------------

(defn- footer-common []
  (let [locale @(rf/subscribe [:current-locale])]
    [:footer.footer
     [:div.container-fluid.px-md-5
      [:div.row
       [:div.col-md-6.col-xl-3.col-12
        [logo-and-slogan]]
       [:div.col-md-6.col-xl-9.col-12.text-xl-end.pt-3.pt-md-0
        [footer-nav locale]]]
      [:div.row
       [:div.col-md-6.col-12
        [developed-in-nrw]
        [registered-trademark]
        [versions]]
       [:div.col-md-6.col-12.text-md-end.pt-3.pt-md-0
        [social-media]]]]]))

(defn footer
  "Footer to display at the bottom the page."
  []
  (when-not (or config/in-iframe? @(rf/subscribe [:ui/setting :hide-footer]))
    [footer-common]))

(defn footer-with-wave []
  (when-not (or config/in-iframe? @(rf/subscribe [:ui/setting :hide-footer]))
    [:<>
     [:div.wave-bottom-typography]
     [footer-common]]))

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :app.version/load
 (fn [{:keys [db]} _]
   {:fx [(http/xhrio-request db :get "/version" [:app.version/store-backend])]}))

(rf/reg-event-db
 :app.version/store-backend
 (fn [db [_ {:keys [version]}]]
   (assoc-in db [:app :backend-version] version)))

(rf/reg-sub
 :app.version/backend
 (fn [db _]
   (get-in db [:app :backend-version])))
