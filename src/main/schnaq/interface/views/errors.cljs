(ns schnaq.interface.views.errors
  (:require [cljs.pprint :refer [pprint]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels img-path]]
            [schnaq.interface.views.base :as base]))

(defn- educate-element []
  [:div
   [:div.single-image-span-container.pb-4 [:img {:src (img-path :elephant-stop)}]]
   [:div
    [:h1 (labels :errors/comic-relief)]
    [:h4 (labels :errors/insufficient-access-rights)]]])

(defn- invalid-admin-link-view
  "Shall tell the user they have no rights to view the content, they are trying to access."
  []
  [:div.text-center
   [base/nav-header]
   [:div.container.px-5.py-3
    [:div.pb-4
     [educate-element]]
    [:button.btn.button-primary.btn-lg.center-block
     {:role "button"
      :on-click #(rf/dispatch [:navigation/navigate :routes/startpage])}
     (labels :errors/navigate-to-startpage)]]])

(defn invalid-admin-link-view-entrypoint []
  [invalid-admin-link-view])

(defn not-found-view-stub []
  [])

(defn true-404-page
  "The 404 page the user gets to see."
  []
  [:<>
   [base/nav-header]
   [:div.container.py-3.text-center
    [:img {:src (img-path :elephant-stop)
           :style {:max-width "60%"}}]
    [:div.alert.alert-danger.mt-4 {:role "alert"}
     [:h4.alert-heading (labels :error.404/heading)]
     [:hr]
     [:p (labels :error.404/body-text)]]]])

(defn true-404-entrypoint []
  "404 view wrapper for routes."
  [true-404-page])

(rf/reg-event-fx
  :ajax-failure
  (fn [{:keys [db]} [_ failure]]
    {:db (assoc db :error {:ajax failure})
     :fx [[:dispatch [:notification/add
                      #:notification{:title (labels :errors/generic)
                                     :body [:pre
                                            [:code
                                             (with-out-str (pprint failure))]]
                                     :context :danger
                                     :stay-visible? true
                                     :on-close-fn #(rf/dispatch [:clear-error])}]]]}))

(rf/reg-sub
  :error-occurred
  (fn [db]
    (:error db)))

(rf/reg-event-db
  :clear-error
  (fn [db _]
    (dissoc db :error)))