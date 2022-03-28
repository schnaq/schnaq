(ns schnaq.interface.views.presentation
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.common :refer [schnaq-logo]]
            [schnaq.interface.components.schnaq :as sc]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.loading :as loading]
            [schnaq.interface.views.schnaq.poll :as poll]))

(def ^:private direct-link "schnaq.com/hubbattle")

(defn- footer
  "Add footer links."
  []
  [:div.text-center
   [:hr.w-50.mx-auto]
   [:a.btn.btn-sm.btn-link.text-dark
    {:href (navigation/href :routes/legal-note)}
    (labels :footer.buttons/legal-note)]
   [:a.btn.btn-sm.btn-link.text-dark
    {:href (navigation/href :routes.privacy/complete)}
    (labels :router/privacy)]])

(defn- share-options
  "Show share-options, e.g. link and QR code."
  []
  [:section.text-center
   [:div.display-6.text-center.pb-3.pt-5
    "Gehe auf "
    [:a {:href (str "https://" direct-link)}
     [:u.fw-bolder direct-link]]
    " und nimm am Ranking teil!"]
   [sc/qr-code (str "https://" direct-link) 250 {:bgColor "transparent"}]])

(defn- fullscreen
  "Full screen view of a component."
  []
  (let [{:poll/keys [title] :as poll} @(rf/subscribe [:present/poll])
        admin-access? @(rf/subscribe [:schnaq/edit-hash])]
    [:div.container.pt-4 {:style {:min-height "100vh"}}
     [schnaq-logo {:style {:width "200px"}
                   :class "pb-3"}]
     [:h1.pb-3 title]
     (if poll
       (if admin-access?
         [:section.row
          [:div.col-12.col-md-3 [share-options]]
          [:div.offset-1.col-md-8 [poll/ranking-results poll]]]
         [:section.w-75.mx-auto
          [poll/ranking-results poll]])
       [:div.text-center [loading/spinner-icon]])
     [footer]]))

;; -----------------------------------------------------------------------------

(defn view []
  [fullscreen])

;; -----------------------------------------------------------------------------

(rf/reg-sub
 :present/poll
 (fn [db]
   (get-in db [:present :poll])))
