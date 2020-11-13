(ns schnaq.interface.views.feedback.admin
  (:require [clojure.string :as string]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.pages :as pages]
            [ajax.core :as ajax]))

(defn- list-feedbacks
  "Shows a list of all feedback."
  []
  [:div#feedback-list
   (let [feedbacks @(rf/subscribe [:feedbacks])]
     [:<>
      [:h4 (gstring/format "Es gibt %s Rückmeldungen 🥳 !" (count feedbacks))]
      [:table.table.table-striped
       [:thead
        [:tr
         [:th {:width "20%"} (labels :feedbacks.overview/contact-name)]
         [:th {:width "60%"} (labels :feedbacks.overview/description)]
         [:th {:width "20%"} (labels :feedbacks/screenshot)]]]
       [:tbody
        (for [feedback feedbacks]
          [:tr {:key (:db/id feedback)}
           [:td (:feedback/contact-name feedback)
            (when-not (string/blank? (:feedback/contact-mail feedback))
              [:a {:href (gstring/format "mailto:%s" (:feedback/contact-mail feedback))}
               [:i.far.fa-envelope.pl-1]])]
           [:td (:feedback/description feedback)]
           [:td.image
            (when (:feedback/has-image? feedback)
              (let [img-src (gstring/format "/media/feedbacks/screenshots/%s.png" (:db/id feedback))]
                [:a {:href img-src}
                 [:img.img-fluid.img-thumbnail {:src img-src}]]))]])]]])])

(defn- overview
  "Shows the page for an overview of all feedbacks."
  []
  (let [feedbacks @(rf/subscribe [:feedbacks])]
    (if (nil? feedbacks)
      (let [password (js/prompt "Enter password to see all Feedbacks")]
        (rf/dispatch [:feedbacks/fetch password]))
      [pages/with-nav-and-header {:page/heading (labels :feedbacks.overview/header)
                                  :page/subheading (labels :feedbacks.overview/subheader)}
       [:div.container.py-4 [list-feedbacks]]])))

(defn feedbacks-view []
  [overview])


;; -----------------------------------------------------------------------------

(rf/reg-event-db
  :feedbacks/store
  (fn [db [_ all-feedbacks]] (assoc db :feedbacks all-feedbacks)))

(rf/reg-event-fx
  :feedbacks/fetch
  (fn [_ [_ password]]
    {:fx [[:http-xhrio {:method :post
                        :uri (gstring/format "%s/feedbacks" (:rest-backend config))
                        :params {:password password}
                        :format (ajax/transit-request-format)
                        :response-format (ajax/transit-response-format)
                        :on-success [:feedbacks/store]
                        :on-failure [:ajax-failure]}]]}))