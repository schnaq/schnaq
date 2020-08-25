(ns meetly.interface.views.errors
  (:require [cljs.pprint :refer [pprint]]
            [meetly.interface.text.display-data :refer [labels img-path]]
            [meetly.interface.views.base :as base]
            [re-frame.core :as rf]))

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
      :on-click #(rf/dispatch [:navigation/navigate :routes/startpage])}
     (labels :errors/navigate-to-startpage)]]])

(rf/reg-event-fx
  :ajax-failure
  (fn [{:keys [db]} [_ failure]]
    {:db (assoc db :error {:ajax failure})
     :dispatch [:notification/add
                #:notification{:title (labels :errors/generic)
                               :body [:pre
                                      [:code
                                       (with-out-str (pprint failure))]]
                               :context :danger
                               :stay-visible? true
                               :on-close-fn #(rf/dispatch [:clear-error])}]}))

(rf/reg-sub
  :error-occurred
  (fn [db]
    (:error db)))

(rf/reg-event-db
  :clear-error
  (fn [db _]
    (dissoc db :error)))