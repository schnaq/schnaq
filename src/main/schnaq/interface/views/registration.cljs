(ns schnaq.interface.views.registration
  (:require [com.fulcrologic.guardrails.core :refer [=> >defn- ?]]
            [goog.string :refer [format]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.common :refer [schnaq-logo]]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.navbar :refer [language-dropdown]]
            [schnaq.interface.config :as config]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.pricing :as pricing-view]))

(defn- next-button [label attrs]
  [buttons/button label nil "btn-primary btn-lg w-100 mt-5" attrs])

(>defn- registration-card
  "Wrapper to build a common registration card for the complete registration
  process."
  [heading body footer {:keys [step class wide?]}]
  [string? :re-frame/component (? :re-frame/component) (? map?) => :re-frame/component]
  [:section.mx-auto.pt-5.position-relative {:class (or class "col-11 col-md-8 col-xxl-6")}
   [:div.common-card
    [:div.position-absolute.top-0.end-0 [language-dropdown]]
    [:div.col-6.col-md-4.mx-auto
     [:a {:href (navigation/href :routes.schnaqs/personal)}
      [schnaq-logo]]
     [:p.text-center.text-muted (format (labels :registration.steps/heading) step)]]
    [:section.mx-auto.px-3 {:class (when-not wide? "w-75")}
     [:h1.h4.text-center.py-3.py-5 heading]
     body]]
   footer])

;; -----------------------------------------------------------------------------

(defn- checkbox [id label icon-key]
  [:div.col-12.col-md-6.col-xxl-4.pb-2
   [:input.btn-check {:id id :type :checkbox :autoComplete :off :name id}]
   [:label.btn.btn-outline-dark.mx-1.w-100 {:for id}
    [icon icon-key "m-1" {:size :lg}]
    [:p.mb-0 label]]])

(defn- survey
  "Ask user where she wants to use schnaq for."
  []
  [:section
   [:div.row
    [checkbox "education" (labels :registration.survey.options/education) :graduation-cap]
    [checkbox "coachings" (labels :registration.survey.options/coachings) :university]
    [checkbox "seminars" (labels :registration.survey.options/seminars) :rocket]
    [checkbox "fairs" (labels :registration.survey.options/fairs) :briefcase]
    [checkbox "meetings" (labels :registration.survey.options/meetings) :laptop]
    [checkbox "other" (labels :registration.survey.options/other) :magic]]])

(defn- registration-step-2
  "First step in the registration process."
  []
  [registration-card
   (labels :registration.survey/heading)
   [:<>
    [:p.text-muted.mb-1 (labels :registration.survey/select-all)]
    [:form
     {:on-submit
      (fn [e] (.preventDefault e)
        (let [form (oget e [:target :elements])]
          (rf/dispatch [:registration/store-survey-selection form])))}
     [survey]
     [next-button (labels :registration.survey.input/submit-button)
      {:id "registration-second-step"}]]]
   nil
   {:step 2}])

(defn registration-step-2-view
  "Wrapped view for usage in routes."
  []
  [pages/fullscreen
   {:page/title (labels :registration/heading)}
   [registration-step-2]])

;; -----------------------------------------------------------------------------

(defn- list-item
  "Style a list item in the feature list."
  [content]
  [:li.list-group-item.border-0.p-0
   [:span.fa-li [icon :check/normal "text-primary me-2" {:size :xs}]]
   content])

(>defn- tier-card
  "Small tier cards, as a preview on the features."
  [title subtitle price button features additional-classes]
  [string? string? :re-frame/component :re-frame/component :re-frame/component (? string?) => :re-frame/component]
  [:article.col-12.col-xl-4.px-1.pb-2
   [:div.card.shadow-sm {:class additional-classes}
    [:div.card-body
     [:div {:style {:min-height "12rem"}}
      [:p.h5.card-title title]
      [:p.h6.card-subtitle.mb-3.text-muted subtitle]
      [:div.text-center.py-3 price]]
     [:div.text-center button]
     [:hr]
     [:small features]]]])

(defn- pro-tier-cta-button
  "Button to open the checkout page."
  []
  (let [price-id (:id @(rf/subscribe [:pricing.pro/yearly]))]
    [buttons/button (labels :registration.pricing/subscribe-pro)
     #(rf/dispatch [:subscription/create-checkout-session price-id])
     "btn-secondary"]))

(defn- free-tier-card
  "Show a free tier card."
  []
  (let [currency-symbol @(rf/subscribe [:user.currency/symbol])]
    [tier-card
     (labels :pricing.free-tier/title)
     (labels :pricing.free-tier/subtitle)
     [:span.display-6 (format "0 %s" currency-symbol)]
     [buttons/button
      (labels :registration.pricing/start-with-free)
      #(rf/dispatch [:schnaq.create/demo])
      "btn-primary"]
     [:ul.fa-ul.list-group.list-group-flush
      [list-item (format (labels :pricing.features/number-of-users) config/max-concurrent-users-free-tier)]
      [list-item (labels :registration.pricing.free/dynamic-qa)]
      [list-item (labels :registration.pricing.free/shareable)]]]))

(defn- pro-tier-card
  "Show the pro tier card."
  []
  [tier-card
   (labels :pricing.pro-tier/title)
   (labels :pricing.pro-tier/subtitle)
   [pricing-view/price-tag-pro-tier "display-6"]
   [pro-tier-cta-button]
   [:<>
    [:strong (labels :registration.pricing.pro/all-from-free)]
    [:ul.fa-ul.list-group.list-group-flush
     [list-item (format (labels :pricing.features/number-of-users) config/max-concurrent-users-pro-tier)]
     [list-item (labels :registration.pricing.pro/polls)]
     [list-item (labels :registration.pricing.pro/activations)]
     [list-item (labels :registration.pricing.pro/mods)]
     [list-item (labels :registration.pricing.pro/themes)]]]
   "border-primary shadow"])

(defn- enterprise-tier-card
  "Show the enterprise tier card."
  []
  [tier-card
   (labels :pricing.enterprise-tier/title)
   (labels :pricing.enterprise-tier/subtitle)
   [:span.display-6 (labels :pricing.enterprise-tier/on-request)]
   [pricing-view/enterprise-cta-button]
   [:<>
    [:strong (labels :registration.pricing.enterprise/all-from-pro)]
    [:ul.fa-ul.list-group.list-group-flush
     [list-item (labels :pricing.features.number-of-users/unlimited)]
     (for [label (rest (labels :pricing.features/enterprise))]
       (with-meta
         [list-item label]
         {:key (str "list-item-" label)}))]]])

(defn- registration-step-3
  "First step in the registration process."
  []
  [registration-card
   (labels :registration.pricing/heading)
   [:<>
    [:div.row
     [free-tier-card]
     [pro-tier-card]
     [enterprise-tier-card]]
    [:div.text-center
     [buttons/anchor
      (labels :registration.pricing/compare-plans)
      "https://schnaq.com/pricing" "btn-link"]]
    [:div.text-center
     [pricing-view/one-time-information :smaller]
     [:p.small (labels :pricing.billing/info-4-one-time)]]]
   nil
   {:step 3
    :wide? true}])

(defn registration-step-3-view
  "Wrapped view for usage in routes."
  []
  [pages/fullscreen
   {:page/title (labels :registration/heading)}
   [registration-step-3]])

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :registration/store-survey-selection
 (fn [{:keys [db]} [_ form]]
   (let [education (oget form [:education :checked])
         coachings (oget form [:coachings :checked])
         seminars (oget form [:seminars :checked])
         fairs (oget form [:fairs :checked])
         meetings (oget form [:meetings :checked])
         other (oget form [:other :checked])
         topics (cond-> []
                  education (conj :surveys.using-schnaq-for.topics/education)
                  coachings (conj :surveys.using-schnaq-for.topics/coachings)
                  seminars (conj :surveys.using-schnaq-for.topics/seminars)
                  fairs (conj :surveys.using-schnaq-for.topics/fairs)
                  meetings (conj :surveys.using-schnaq-for.topics/meetings)
                  other (conj :surveys.using-schnaq-for.topics/other))]
     {:fx [(http/xhrio-request db :post "/surveys/participate/using-schnaq-for"
                               [:registration.store-survey-selection/success]
                               {:topics topics})]})))

(rf/reg-event-fx
 :registration.store-survey-selection/success
 (fn []
   {:fx [[:dispatch [:navigation/navigate :routes.user.register/step-3]]]}))
