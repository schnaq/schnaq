(ns schnaq.interface.pages.umfrage-danke
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.pages :as pages]))

(rf/reg-sub
 :sub-params
 (fn [db _]
   (get-in db [:current-route :query-params :umfrage])))

(defn- subscription-form
  []
  (let [sub-param @(rf/subscribe [:sub-params])]
    [:section.row.pt-3.pb-5.text-left
     [:div.col
      [:form
       {:target "_blank" :name "mc-embedded-subscribe-form" :method "post" :action
        "https://schnaq.us8.list-manage.com/subscribe?u=adbf5722068bcbcc4c7c14a72&id=407d47335d"}

       [:div.form-group
        [:input
         {:required true
          :placeholder (labels :startpage.newsletter/address-placeholder)
          :name "EMAIL" :defaultValue "" :type "email"
          :class "form-control"}]]

       [:ul {:area-hidden "true" :style {:display "none"}}
        [:li [:input {:id "mce-group[295590]-295590-0" :checked (= "wima" sub-param) :type "checkbox" :value "1" :name "group[295590][1]" :tabIndex "-1"}]
         [:label {:for "mce-group[295590]-295590-0"} "Wissensmanagement"]]
        [:li [:input {:id "mce-group[295590]-295590-1" :checked (= "consulting" sub-param) :type "checkbox" :value "2" :name "group[295590][2]" :tabIndex "-1"}]
         [:label {:for "mce-group[295590]-295590-1"} "Consulting"]]
        [:li [:input {:id "mce-group[295590]-295590-2" :checked (= "e-learning" sub-param) :type "checkbox" :value "4" :name "group[295590][4]" :tabIndex "-1"}]
         [:label {:for "mce-group[295590]-295590-2"} "E-Learning"]]
        [:li [:input {:id "mce-group[295590]-295590-3" :checked (= "creator" sub-param) :type "checkbox" :value "8" :name "group[295590][8]" :tabIndex "-1"}]
         [:label {:for "mce-group[295590]-295590-3"} "Creator"]]]

       [:div.form-group
        [:div.this-is-just-for-bots-do-not-fill-this-out
         {:aria-hidden "true" :style {:position "absolute" :left "-5000px"}}
         [:input {:defaultValue "" :tabIndex "-1" :type "text"
                  :name "b_adbf5722068bcbcc4c7c14a72_407d47335d"}]]]

       [:div.form-group
        [:div.form-check
         [:input#nochmal-nachfragen.form-check-input {:type "checkbox" :required true}]
         [:label.form-check-label {:for "nochmal-nachfragen"}
          (labels :startpage.newsletter/consent)]
         [:a {:href "#" :type "button" :data-toggle "collapse" :data-target "#collapse-more-newsletter"
              :aria-expanded "false" :aria-controls "#collapse-more-newsletter" :data-reitit-handle-click false}
          (labels :startpage.newsletter/more-info-clicker)]
         [:div.collapse {:id "collapse-more-newsletter"}
          [:p.small (labels :startpage.newsletter/policy-disclaimer)
           [:br] (labels :startpage.newsletter/privacy-policy-lead) " "
           [:a {:href (reitfe/href :routes/privacy-extended)}
            (labels :privacy/note)] "."]]]]

       [:div.form-group
        [:input
         {:name "subscribe" :value (labels :startpage.newsletter/button) :type "submit" :readOnly true
          :class "btn btn-primary d-block mx-auto"}]]]]]))

(defn thanks-page []
  [pages/with-nav-and-header
   {:page/heading "Danke!"}
   [:div.container.text-center.mx-auto
    [:h2 "Danke für deine Teilnahme an der Umfrage!"]
    [:img.img-fluid.rounded.mb-3
     {:src "https://media2.giphy.com/media/l0ExhcMymdL6TrZ84/giphy.gif"}]
    [:p "Möchtest du über schnaq auf dem Laufenden gehalten werden? Dann trage dich gerne in unseren Newsletter ein."]
    [subscription-form]]])

(defn view []
  [thanks-page])
