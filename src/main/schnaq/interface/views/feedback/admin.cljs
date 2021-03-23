(ns schnaq.interface.views.feedback.admin
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [schnaq.interface.auth :as auth]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.time :as time]
            [schnaq.interface.views.loading :as loading]
            [schnaq.interface.views.pages :as pages]))

(defn- list-feedbacks
  "Shows a list of all feedback."
  []
  [:div.container.py-4
   (let [feedbacks @(rf/subscribe [:feedbacks])
         locale @(rf/subscribe [:current-locale])]
     (if feedbacks
       [:<>
        [:h4 (gstring/format (labels :feedbacks.overview/table-header) (count feedbacks))]
        [:table.table.table-striped
         [:thead
          [:tr
           [:th {:width "10%"} (labels :feedbacks.overview/contact-name)]
           [:th {:width "10%"} (labels :feedbacks.overview/when?)]
           [:th {:width "60%"} (labels :feedbacks.overview/description)]
           [:th {:width "20%"} (labels :feedbacks/screenshot)]]]
         [:tbody
          (for [feedback feedbacks]
            [:tr {:key (:db/id feedback)}
             [:td (:feedback/contact-name feedback)
              (when-not (string/blank? (:feedback/contact-mail feedback))
                [:a {:href (gstring/format "mailto:%s" (:feedback/contact-mail feedback))}
                 [:i.far.fa-envelope.pl-1]])]
             [:td (time/format-distance (:db/txInstant feedback) locale)]
             [:td (:feedback/description feedback)]
             [:td.image
              (when (:feedback/has-image? feedback)
                (let [img-src (gstring/format "/media/feedbacks/screenshots/%s.png" (:db/id feedback))]
                  [:a {:href img-src}
                   [:img.img-fluid.img-thumbnail {:src img-src}]]))]])]]]
       [loading/loading-placeholder]))])

(defn- overview
  "Shows the page for an overview of all feedbacks."
  []
  [pages/with-nav-and-header
   {:condition/needs-administrator? true
    :page/heading (labels :feedbacks.overview/header)
    :page/subheading (labels :feedbacks.overview/subheader)}
   [list-feedbacks]])

(defn feedbacks-view []
  [overview])


;; -----------------------------------------------------------------------------

(rf/reg-event-db
  :feedbacks/store
  (fn [db [_ all-feedbacks]] (assoc db :feedbacks all-feedbacks)))

(rf/reg-event-fx
  :feedbacks/fetch
  (fn [{:keys [db]} _]
    (when (get-in db [:user :authenticated?])
      {:fx [(http/xhrio-request db :get "/admin/feedbacks" [:feedbacks/store])]})))