(ns schnaq.interface.views.pages
  "Defining page-layouts."
  (:require [cljs.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn >defn- ? =>]]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.videos :refer [video]]
            [schnaq.interface.config :as config]
            [schnaq.interface.scheduler :as scheduler]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.loading :as loading]
            [schnaq.interface.views.navbar.for-discussions :as discussion-navbar]
            [schnaq.interface.views.navbar.for-pages :as navbar-pages]))

(declare with-nav-and-header)

(s/def ::string-or-component
  (s/or :string string? :not-yet nil? :component :re-frame/component))

(s/def :page/heading ::string-or-component)
(s/def :page/subheading ::string-or-component)
(s/def :page/title ::string-or-component)
(s/def :page/vertical-header? boolean?)
(s/def :page/more-for-heading vector?)
(s/def :condition/needs-authentication? boolean?)
(s/def :condition/needs-administrator? boolean?)
(s/def :condition/create-schnaq? boolean?)
(s/def ::page-options
  (s/keys :opt [:page/heading :page/subheading :page/title :page/more-for-heading :page/vertical-header?
                :condition/needs-authentication? :condition/needs-administrator? :condition/create-schnaq?]))

;; -----------------------------------------------------------------------------

(defn login-page
  "Show a login page."
  []
  [with-nav-and-header
   {:condition/needs-authentication? true
    :page/vertical-header? true
    :page/heading (labels :page.login/heading)
    :page/subheading (labels :page.login/subheading)}
   (when @(rf/subscribe [:user/authenticated?])
     (rf/dispatch [:navigation/navigate :routes.schnaqs/personal]))])

(defn- bullet-points
  "Short overview of free-features."
  []
  [:ul.fa-ul
   [:li.h4 [icon :check/normal "me-3"] (labels :page.login/feature-1)]
   [:li.h4 [icon :check/normal "me-3"] (gstring/format (labels :pricing.features/number-of-users) config/max-concurrent-users-free-tier)]
   [:li.h4 [icon :check/normal "me-3"] (labels :page.login/feature-3)]])

(defn- login-page-base
  "Basic login page for either registration or sign in."
  [heading subheading]
  [with-nav-and-header
   {:page/heading (labels heading)
    :page/subheading (labels subheading)
    :page/vertical-header? true
    :page/classes "base-wrapper bg-typography"
    :page/more-for-heading
    [:section.container {:style {:min-height "50vh"}}
     [:div.row.pt-lg-5
      [:div.col-12.col-lg-7.col-xl-6
       [:video.w-75.rounded-5.my-auto.d-none.d-lg-block
        {:auto-play true :loop true :muted true :plays-inline true}
        [:source {:src (video :register.point-right/webm) :type "video/webm"}]
        [:source {:src (video :register.point-right/mp4) :type "video/mp4"}]]]
      [:div.col-12.col-lg-5.col-xl-6
       [:div.my-5.my-lg-3.pt-lg-5
        [:div.text-center
         [:div
          [:button.btn.btn-lg.btn-dark.mb-3
           {:on-click #(rf/dispatch [:keycloak/register])}
           [:div.display-5 (labels :page.register/register)]]]
         (labels :page.login/or)
         [:div
          [:button.btn.btn-lg.btn-outline-white.mt-3
           {:on-click #(rf/dispatch [:keycloak/login])}
           (labels :page.login/login)]]]
        [:div.my-5 [bullet-points]]
        [:div.mt-3.text-center
         (labels :page.login.alert/text-1)
         [buttons/anchor
          (labels :page.login.alert/button)
          "https://schnaq.com/pricing"
          "btn-sm btn-outline-white mx-2"]
         (labels :page.login.alert/text-2)]
        [:img.w-50.align-self-center.d-lg-none {:src (img-path :schnaqqifant/three-d-bubble)
                                                :alt (labels :schnaqqi/pointing-right)}]]]]]}])

(defn- please-login
  "Default page indicating that it is necessary to login."
  []
  [login-page-base
   :page.login/heading
   :page.login/subheading])

(defn- register-cta
  "Default page indicating a first time user creates a schnaq."
  []
  [login-page-base
   :page.register/heading
   :page.login/subheading])

(defn- beta-only
  "Default page indicating, that only beta users are allowed."
  []
  [with-nav-and-header
   {:page/heading (labels :page.beta/heading)
    :page/subheading (labels :page.beta/subheading)}
   [:div.container.text-center.pt-5
    [:p (labels :page.beta.modal/cta) " " [:a {:href "mailto:info@schnaq.com"} (tools/obfuscate-text "info@schnaq.com")] "."]]])

(defn loading-page
  "Show a loading page."
  []
  [with-nav-and-header
   {:page/vertical-header? true
    :page/heading (labels :loading.page/heading)
    :page/subheading (labels :loading.page/subheading)}
   [:div.container
    [loading/loading-placeholder]]])

(>defn- validate-conditions-middleware
  "Takes the conditions and returns either the page or redirects to other views."
  [{:condition/keys
    [needs-authentication? needs-administrator? needs-beta-tester? create-schnaq? needs-analytics-admin?]}
   page]
  [::page-options (s/+ vector?) :ret vector?]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])
        admin? @(rf/subscribe [:user/administrator?])
        analytics-admin? @(rf/subscribe [:user/analytics-admin?])
        beta-tester? @(rf/subscribe [:user/beta-tester?])]
    (cond
      (and create-schnaq? (not authenticated?)) [register-cta]
      (and (or needs-authentication? needs-administrator? needs-beta-tester? needs-analytics-admin?)
           (not authenticated?)) [please-login]
      (and needs-administrator? (not admin?)) [loading-page]
      (and needs-beta-tester? (not beta-tester?)) [beta-only]
      (and needs-analytics-admin? (not analytics-admin?)) [loading-page]
      :else page)))

;; -----------------------------------------------------------------------------
;; Page Segments

(>defn settings-panel
  "Construct a common panel for the middle in a feed."
  [heading body]
  [string? vector? :ret vector?]
  [:div.panel-white.p-5
   [:h1.text-center heading]
   body])

;; -----------------------------------------------------------------------------
;; Complete page layouts

(>defn- page-builder
  "Build pages and page-layouts.
  Use this meta-component to build your pages and page-layouts. It sets the title,
  description, composes header, body and footer for a common look, validates 
  conditions and waits for the scheduler to finish."
  [{:page/keys [title description heading classes] :as options} header body footer]
  [::page-options (? :re-frame/component) (? :re-frame/component) (? :re-frame/component) :ret :re-frame/component]
  (common/set-website-title! (or title heading))
  (common/set-website-description! description)
  [scheduler/middleware
   [validate-conditions-middleware
    options
    [:div.d-flex.flex-column.min-vh-100 {:class classes}
     header
     [:div.flex-grow-1 body]
     footer]]])

(>defn with-nav-and-header
  "Default page with header and curly wave."
  [{:page/keys [wavy-footer?] :as options} body]
  [::page-options (? :re-frame/component) :ret :re-frame/component]
  [page-builder
   options
   [:div.masthead-layered
    [navbar-pages/navbar-transparent (:page/wrapper-classes options)]
    [base/header options]]
   body
   (if wavy-footer?
     [base/footer-with-wave]
     [base/footer])])

(>defn with-discussion-header
  "Page layout with discussion header."
  [options body]
  [::page-options (s/+ vector?) :ret vector?]
  [page-builder options [discussion-navbar/header] body [base/footer]])

(>defn with-qanda-header
  "Page layout with discussion header."
  [options body]
  [::page-options (s/+ vector?) :ret vector?]
  [page-builder options [discussion-navbar/qanda-header] body [base/footer-with-wave]])

(>defn three-column-layout
  "Use three column layout to display page."
  [{:page/keys [title heading] :as options} left middle right]
  [::page-options vector? vector? vector? :ret vector?]
  [page-builder
   options
   [navbar-pages/navbar (or title heading)]
   [:section.container-fluid.p-3
    [:div.row
     [:div.col-12.col-lg-3.px-0.px-md-3 left]
     [:div.col-12.col-lg-6.px-0.px-md-3 middle]
     [:div.col-12.col-lg-3.px-0.px-md-3 right]]]
   [base/footer]])

(>defn fullscreen
  "Page layout with no header and no footer."
  [options body]
  [::page-options :re-frame/component => :re-frame/component]
  [page-builder options nil body nil])
