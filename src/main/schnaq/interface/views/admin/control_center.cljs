(ns schnaq.interface.views.admin.control-center
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.pages :as pages]))

(rf/reg-event-db
  :admin.schnaq.delete/success
  (fn [db [_ {:keys [share-hash]}]]
    (let [public-schnaqs (get-in db [:schnaqs :public])
          new-public-schnaqs (remove #(= share-hash (:discussion/share-hash %)) public-schnaqs)]
      (assoc-in db [:schnaqs :public] new-public-schnaqs))))

(rf/reg-event-fx
  :admin.schnaq/delete
  (fn [{:keys [db]} [_ share-hash]]
    {:fx [(http/xhrio-request db :delete "/admin/schnaq/delete" [:admin.schnaq.delete/success]
                              {:share-hash share-hash} [:ajax.error/as-notification])]}))

(defn- public-schnaq-deletion-form
  "Easily delete one of the public schnaqs."
  []
  [:form.form
   {:id "public-schnaq-form"
    :on-submit (fn [e]
                 (js-wrap/prevent-default e)
                 (when (js/confirm (labels :admin.center.delete/confirmation))
                   (rf/dispatch [:admin.schnaq/delete
                                 (oget e [:target :elements :public-schnaq-select :value])])))}
   [:div.form-row.align-items-center
    [:div.col-auto
     [:label
      {:for "public-schnaq-select"} (labels :admin.center.delete.public/label)]]
    (let [public-schnaqs @(rf/subscribe [:schnaqs/public])]
      [:div.col-auto
       [:select.form-control
        {:id "public-schnaq-select"}
        (for [schnaq public-schnaqs]
          [:option
           {:key (:discussion/share-hash schnaq)
            :value (:discussion/share-hash schnaq)}
           (:discussion/title schnaq)])]])
    [:div.col-auto
     [:button.btn.btn-secondary {:type "submit"} (labels :admin.center.delete.public/button)]]]])

(defn- private-schnaq-deletion-form
  "Easily delete any schnaq."
  []
  [:form.form#private-schnaq-form
   {:on-submit (fn [e]
                 (js-wrap/prevent-default e)
                 (when (js/confirm (labels :admin.center.delete/confirmation))
                   (rf/dispatch [:admin.schnaq/delete
                                 (oget e [:target :elements :private-meeting-hash :value])])))}
   [:div.form-row.align-items-center
    [:div.col-auto
     [:label
      {:for "private-schnaq-hash"} (labels :admin.center.delete.private/label)]]
    [:div.col-auto
     [:input.form-control
      {:id "private-schnaq-hash"}]]
    [:div.col-auto
     [:button.btn.btn-secondary {:type "submit"} (labels :admin.center.delete.public/button)]]]])

(defn- center-overview
  "The startpage of the admin center."
  []
  [pages/with-nav-and-header
   {:condition/needs-administrator? true
    :page/title (labels :admin.center.start/title)
    :page/heading (labels :admin.center.start/heading)
    :page/subheading (labels :admin.center.start/subheading)}
   [:div.container
    [:h2 (labels :admin.center.delete/heading)]
    [:h4 (labels :admin.center.delete.public/heading)]
    [public-schnaq-deletion-form]
    [:h4 (labels :admin.center.delete.private/heading)]
    [private-schnaq-deletion-form]]])

(defn center-overview-route
  []
  [center-overview])
