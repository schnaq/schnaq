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

(defn not-found-view
  "The view that is displayed with a 404"
  []
  (.replace (.-location js/window) "/404/"))

(defn not-found-view-entrypoint []
  [not-found-view])

(defn true-404-page
  "The 404 page the user gets to see."
  []
  [:<>
   [base/nav-header]
   [:div
    [:h1 "Seite nicht vorhanden"]
    [:p "Der Link dem Sie gefolgt sind existiert leider nicht."]]])

(defn true-404-entrypoint []
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