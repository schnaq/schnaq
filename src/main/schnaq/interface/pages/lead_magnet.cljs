(ns schnaq.interface.pages.lead-magnet
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.views.pages :as pages]))

(rf/reg-event-fx
  :lead-magnet/subscribe
  (fn [{:keys [db]} [_ email]]
    {:fx [(http/xhrio-request db :post "/lead-magnet/subscribe" :lead-magnet.subscribe/success {:email email})]}))
;; TODO success case

(defn- subscription-form
  []
  [:form
   {:on-submit (fn [e]
                 (jq/prevent-default e)
                 (rf/dispatch [:lead-magnet/subscribe (oget e [:target :elements "EMAIL" :value])]))}

   [:div.form-group
    [:input
     {:required true
      :placeholder (labels :startpage.newsletter/address-placeholder)
      :name "EMAIL" :defaultValue "" :type "email"
      :class "form-control"}]]

   [:div.form-group
    [:div.form-check
     [:input#nochmal-nachfragen.form-check-input {:type "checkbox" :required true}]
     [:label.form-check-label {:for "nochmal-nachfragen"}
      (labels :startpage.newsletter/consent)]
     [:p
      [:a {:href "#" :type "button" :data-toggle "collapse" :data-target "#collapse-more-newsletter"
           :aria-expanded "false" :aria-controls "#collapse-more-newsletter"}
       (labels :startpage.newsletter/more-info-clicker)]]
     [:div.collapse {:id "collapse-more-newsletter"}
      [:p.small (labels :startpage.newsletter/policy-disclaimer)
       [:br] (labels :startpage.newsletter/privacy-policy-lead)
       [:a {:href (reitfe/href :routes/privacy-extended)}
        (labels :privacy/note)] "."]]]]

   [:div.form-group
    [:input
     {:name "subscribe" :value (labels :startpage.newsletter/button) :type "submit" :readOnly true
      :class "btn btn-primary d-block mx-auto"}]]])

(defn- lead-magnet
  []
  [pages/with-nav-and-header
   {:page/heading "Datenschutzkonform verteilt arbeiten"
    :page/subheading "Eine handliche Checkliste um in allen Bereichen gerüstet zu sein"}
   [:section.container
    [subscription-form]]])

(defn view []
  [lead-magnet])