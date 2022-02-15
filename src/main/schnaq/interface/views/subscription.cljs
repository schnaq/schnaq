(ns schnaq.interface.views.subscription
  (:require [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.icons :refer [icon-card]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.pages :as pages]))

(defn- next-step [icon title body button-text route-name]
  (let [href (navigation/href route-name)]
    [:article.pb-3.pe-3
     [:a {:href href} [icon-card icon "text-typography" {:size :lg}]]
     [:p.fw-bold.my-2 title]
     [:p body]
     [buttons/anchor button-text href :btn-white]]))

(defn- success
  "Celebrating and welcoming the new pro user."
  []
  [pages/with-nav-and-header
   {:page/heading (str (labels :subscription.page.success/heading) " 🎉")
    :page/subheading (labels :subscription.page.success/subheading)
    :page/vertical-header? true
    :page/classes "base-wrapper bg-typography"
    :page/more-for-heading
    [:section.container {:style {:min-height "50vh"}}
     [:div.pt-5.mt-md-5
      [:div.d-flex.flex-md-row.flex-column
       [next-step :rocket
        (labels :subscription.page.success.next-1/title)
        (labels :subscription.page.success.next-1/lead)
        (labels :subscription.page.success.next-1/button)
        :routes.schnaqs/personal]
       [next-step :sliders-h
        (labels :subscription.page.success.next-2/title)
        (labels :subscription.page.success.next-2/lead)
        (labels :subscription.page.success.next-2/button)
        :routes.user.manage/account]]
      [:img.pt-5 {:src (img-path :schnaqqifant/rocket)}]]]}])

(defn- cancel
  "Page if a user cancels the checkout process."
  []
  [pages/with-nav-and-header
   {:page/heading "Vorgang abgebrochen"}
   [:section.container
    [:h3.pb-3 (labels :subscription.page.cancel/title)]
    [:p.lead (labels :subscription.page.cancel/lead)]
    [:p (labels :subscription.page.cancel/body)]
    [buttons/anchor (labels :subscription.page.cancel/button) (navigation/href :routes/pricing)]]])

;; -----------------------------------------------------------------------------

(defn success-view
  "Wrapping the success page."
  []
  [success])

(defn cancel-view
  "Wrapping the cancel page."
  []
  [cancel])
