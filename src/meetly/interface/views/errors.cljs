(ns meetly.interface.views.errors
  (:require [meetly.interface.text.display-data :refer [labels img-path fa]]
            [meetly.interface.views.base :as base]
            [re-frame.core :as rf]
            ["framer-motion" :refer [motion AnimatePresence]]))


(defn upper-error-box
  "The error box, that is displayed on top of all pages, when an error occurs."
  [error]
  [:> AnimatePresence
   (when error
     [:> (.-div motion)
      {:initial {:opacity 0}
       :animate {:opacity 1}
       :exit {:opacity 0}}
      [:div.alert.alert-danger.alert-dismissible
       "Error: " error
       [:button.close {:type "button"
                       :on-click #(rf/dispatch [:clear-error])}
        [:span {:aria-hidden "true"}
         [:i {:class (str " m-auto fas fa-lg " (fa :delete-icon))}]]]]])])

(defn- educate-element []
  [:div
   [:div.single-image-span-container.pb-4 [:img {:src (img-path :elephant-stop)}]]
   [:div
    [:h1 (labels :errors/comic-relief)]
    [:h4 (labels :errors/insufficient-access-rights)]]])

(defn invalid-admin-link-view
  "Shall tell the user they have no rights to view the content, they are trying to access."
  []
  [:div.text-center
   [base/nav-header]
   [:div.container.px-5.py-3
    [:div.pb-4
     [educate-element]]
    [:button.btn.button-primary.btn-lg.center-block
     {:role "button"
      :on-click #(rf/dispatch [:navigate :routes/startpage])}
     (labels :errors/navigate-to-startpage)]]])

(rf/reg-event-db
  :ajax-failure
  (fn [db [_ failure]]
    (assoc db :error {:ajax failure})))

(rf/reg-sub
  :error-occurred
  (fn [db]
    (:error db)))

(rf/reg-event-db
  :clear-error
  (fn [db _]
    (dissoc db :error)))