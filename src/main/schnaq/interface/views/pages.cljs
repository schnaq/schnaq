(ns schnaq.interface.views.pages
  "Defining page-layouts."
  (:require [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn >defn-]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.navbar :as navbar]))

(declare with-nav-and-header)

(s/def :page/heading string?)
(s/def :page/subheading string?)
(s/def :page/title string?)
(s/def :page/more-for-heading vector?)
(s/def :condition/needs-authentication? boolean?)
(s/def :condition/needs-administrator? boolean?)
(s/def ::page-options
  (s/keys :req [:page/heading]
          :opt [:page/subheading :page/title :page/more-for-heading
                :condition/needs-authentication? :condition/needs-administrator?]))


;; -----------------------------------------------------------------------------

(defn- please-login
  "Default page indicating, that it is necessary to login."
  []
  [with-nav-and-header
   {:page/heading (labels :page.login/heading)
    :page/subheading (labels :page.login/subheading)}
   [:div.container.text-center
    [:button.btn.btn-lg.btn-secondary
     {:on-click #(rf/dispatch [:keycloak/login])}
     (labels :user/login)]]])

(>defn- with-validated-conditions
  "Takes the conditions and returns either the page or redirects to other views."
  [{:condition/keys [needs-authentication? needs-administrator?]} page]
  [::page-options (s/+ vector?) :ret vector?]
  (if (or needs-administrator? needs-authentication?)
    (if @(rf/subscribe [:user/is-authenticated?])
      (if needs-administrator?
        (if @(rf/subscribe [:user/has-administrator-role?])
          page (rf/dispatch [:navigation/navigate :routes/forbidden-page]))
        page)
      [please-login])
    page))


;; -----------------------------------------------------------------------------

(>defn with-nav-and-header
  "Default page with header and curly wave."
  [{:page/keys [title heading subheading more-for-heading] :as options} body]
  [::page-options (s/+ vector?) :ret vector?]
  (common/set-website-title! (or title heading))
  (with-validated-conditions
    options
    [:<>
     [navbar/navbar]
     [base/header heading subheading more-for-heading]
     body]))

(>defn with-nav
  "Default page with header and curly wave."
  [{:page/keys [title heading] :as options} body]
  [::page-options (s/+ vector?) :ret vector?]
  (common/set-website-title! (or title heading))
  (with-validated-conditions
    options
    [:<>
     [navbar/navbar]
     body]))
