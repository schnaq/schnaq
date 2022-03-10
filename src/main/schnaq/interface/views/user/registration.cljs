(ns schnaq.interface.views.user.welcome
  (:require [schnaq.interface.components.common :refer [next-step]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.pages :as pages]))

(defn- welcome-free-user
  "Welcome free user."
  []
  [pages/with-nav-and-header
   {:page/heading (str (labels :user.registration.success/heading) " 🎉")
    :page/subheading (labels :user.registration.success/subheading)
    :page/vertical-header? true
    :page/classes "base-wrapper bg-typography"
    :page/more-for-heading
    [:section.container {:style {:min-height "50vh"}}
     [:div.pt-5.mt-md-5
      [:div.row
       [:div.col-4
        [next-step :rocket
         (labels :user.registration.success.next-1/title)
         (labels :user.registration.success.next-1/lead)
         (labels :user.registration.success.next-1/button)
         :routes.schnaqs/personal]]
       [:div.col-4
        [next-step :id-card
         (labels :user.registration.success.next-2/title)
         (labels :user.registration.success.next-2/lead)
         (labels :user.registration.success.next-2/button)
         :routes.user.manage/account]]
       [:div.col-4
        [next-step :bell
         (labels :user.registration.success.next-3/title)
         (labels :user.registration.success.next-3/lead)
         (labels :user.registration.success.next-3/button)
         :routes.user.manage/notifications]]]
      [:img.pt-5 {:src (img-path :schnaqqifant/rocket)}]]]}])

;; -----------------------------------------------------------------------------

(defn welcome-free-user-view []
  [welcome-free-user])
