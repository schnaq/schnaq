(ns schnaq.interface.views.pages
  "Defining page-layouts."
  (:require [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn >defn-]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.scheduler :as scheduler]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.navbar.for-discussions :as discussion-navbar]
            [schnaq.interface.views.navbar.for-pages :as navbar-pages]))

(declare with-nav-and-header)

(s/def :page/heading string?)
(s/def :page/subheading string?)
(s/def :page/title string?)
(s/def :page/vertical-header? boolean?)
(s/def :page/more-for-heading vector?)
(s/def :condition/needs-authentication? boolean?)
(s/def :condition/needs-administrator? boolean?)
(s/def ::page-options
  (s/keys :req [:page/heading]
          :opt [:page/subheading :page/title :page/more-for-heading :page/vertical-header?
                :condition/needs-authentication? :condition/needs-administrator?]))


;; -----------------------------------------------------------------------------

(defn login-page
  "Show a login page."
  []
  [with-nav-and-header
   {:condition/needs-authentication? true
    :page/heading (labels :page.login/heading)
    :page/subheading (labels :page.login/subheading)}
   (when @(rf/subscribe [:user/authenticated?])
     (rf/dispatch [:navigation/navigate :routes/startpage]))])

(defn- please-login
  "Default page indicating, that it is necessary to login."
  []
  [with-nav-and-header
   {:page/heading (labels :page.login/heading)
    :page/subheading (labels :page.login/subheading)}
   [:div.container.text-center.pt-5
    [:button.btn.btn-lg.btn-secondary
     {:on-click #(rf/dispatch [:keycloak/login])}
     (labels :user/login)]]])

(defn- beta-only
  "Default page indicating, that only beta users are allowed."
  []
  [with-nav-and-header
   {:page/heading (labels :page.beta/heading)
    :page/subheading (labels :page.beta/subheading)}
   [:div.container.text-center.pt-5
    [:p (labels :page.beta.modal/cta) " " [:a {:href "mailto:info@schnaq.com"} (tools/obfuscate-mail "info@schnaq.com")] "."]]])

(>defn- validate-conditions-middleware
  "Takes the conditions and returns either the page or redirects to other views."
  [{:condition/keys [needs-authentication? needs-administrator? needs-beta-tester?]} page]
  [::page-options (s/+ vector?) :ret vector?]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])
        admin? @(rf/subscribe [:user/administrator?])
        beta-tester? @(rf/subscribe [:user/beta-tester?])]
    (cond
      (and (or needs-authentication? needs-administrator? needs-beta-tester?)
           (not authenticated?)) [please-login]
      (and needs-administrator? (not admin?)) (rf/dispatch [:navigation/navigate :routes/forbidden-page])
      (and needs-beta-tester? (not beta-tester?)) [beta-only]
      :else page)))


;; -----------------------------------------------------------------------------
;; Page Segments

(>defn settings-panel
  "Construct a common panel for the middle in a feed."
  [heading body]
  [string? vector? :ret vector?]
  [:div.panel-white.p-5.mb-3
   [:h1.text-muted.mb-5 heading]
   body])


;; -----------------------------------------------------------------------------
;; Complete page layouts

(>defn with-nav-and-header
  "Default page with header and curly wave."
  [{:page/keys [title heading classes] :as options} body]
  [::page-options vector? :ret vector?]
  (common/set-website-title! (or title heading))
  [scheduler/middleware
   [validate-conditions-middleware
    options
    [:div {:class classes}
     [:div.masthead-layered
      [navbar-pages/navbar-transparent (:page/wrapper-classes options)]
      [base/header options]]
     body]]])

(>defn with-nav
  "Default page with header and curly wave."
  [{:page/keys [title heading] :as options} body]
  [::page-options (s/+ vector?) :ret vector?]
  (common/set-website-title! (or title heading))
  [scheduler/middleware
   [validate-conditions-middleware
    options
    [:<>
     [navbar-pages/navbar (or title heading)]
     body]]])

(>defn three-column-layout
  "Use three column layout to display page."
  [options left middle right]
  [::page-options vector? vector? vector? :ret vector?]
  [with-nav
   options
   [:section.container-fluid.p-3
    [:div.row
     [:div.col-12.col-md-3.px-0.px-md-3 left]
     [:div.col-12.col-md-6.px-0.px-md-3 middle]
     [:div.col-12.col-md-3.px-0.px-md-3 right]]]])

(>defn- with-header
  "Page layout with discussion header."
  [{:page/keys [title heading] :as options} body header]
  [::page-options (s/+ vector?) vector? :ret vector?]
  (common/set-website-title! (or title heading))
  [scheduler/middleware
   [validate-conditions-middleware
    options
    [:<>
     (if shared-config/embedded? [discussion-navbar/embeddable-header] header)
     body]]])

(>defn with-discussion-header
  "Page layout with discussion header."
  [options body]
  [::page-options (s/+ vector?) :ret vector?]
  [with-header options body [discussion-navbar/header]])

(>defn with-qanda-view-header
  "Page layout with discussion header."
  [options body]
  [::page-options (s/+ vector?) :ret vector?]
  [with-header options body [discussion-navbar/header-for-qanda-view]])

(>defn embeddable-view
  "A more dense view without footer and other shit "
  [options body]
  [::page-options (s/+ vector?) :ret vector?]
  [scheduler/middleware
   [validate-conditions-middleware
    options
    [:<>
     [discussion-navbar/embeddable-header]
     body]]])
