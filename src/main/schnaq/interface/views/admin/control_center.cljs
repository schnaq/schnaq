(ns schnaq.interface.views.admin.control-center
  (:require [ajax.core :as ajax]
            [oops.core :refer [oget]]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.pages :as pages]
            [re-frame.core :as rf]))

(rf/reg-sub
  :migration.starting-arguments/status
  (fn [db _]
    (get-in db [:migration :status :starting-arguments] "-")))

(rf/reg-event-db
  :migration.starting-arguments/success
  (fn [db _]
    (assoc-in db [:migration :status :starting-arguments] "Migration erfolgreich fertig gestellt")))

(rf/reg-event-fx
  :migration.starting-arguments/start
  (fn [{:keys [db]} _]
    (let [admin-pass (get-in db [:admin :password])]
      {:db (assoc-in db [:migration :status :starting-arguments] "Läuft… Bitte Warten")
       :fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/admin/migrations/starting-statements-a78stdgah23f-a9sd")
                          :params {:password admin-pass}
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:migration.starting-arguments/success]
                          :on-failure [:ajax.error/as-notification]}]]})))

(defn- migrate-starting-arguments-form
  "Migrates the starting-arguments to starting-statements."
  []
  [:form.form
   {:id "migrate-discussions-form"
    :on-submit (fn [e]
                 (js-wrap/prevent-default e)
                 (when (js/confirm "Arguments wirklich migrieren? Nicht nochmal klicken, wenn gestartet!")
                   (rf/dispatch [:migration.starting-arguments/start])))}
   [:button.btn.btn-danger {:type "submit"} "Migriere Starting Arguments JETZT!"]
   [:p "Status: " @(rf/subscribe [:migration.starting-arguments/status])]])

(rf/reg-event-db
  :admin.schnaq.delete/success
  (fn [db [_ {:keys [share-hash]}]]
    (let [public-schnaqs (get-in db [:schnaqs :public])
          new-public-schnaqs (remove #(= share-hash (:discussion/share-hash %)) public-schnaqs)]
      (assoc-in db [:schnaqs :public] new-public-schnaqs))))

(rf/reg-event-fx
  :admin.schnaq/delete
  (fn [{:keys [db]} [_ share-hash]]
    (let [admin-pass (get-in db [:admin :password])]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/admin/schnaq/delete")
                          :params {:share-hash share-hash
                                   :password admin-pass}
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:admin.schnaq.delete/success]
                          :on-failure [:ajax.error/as-notification]}]]})))


(defn- public-meeting-deletion-form
  "Easily delete one of the public meetings."
  []
  [:form.form
   {:id "public-meeting-form"
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

(defn- private-meeting-deletion-form
  "Easily delete any meetings."
  []
  [:form.form
   {:id "private-meeting-form"
    :on-submit (fn [e]
                 (js-wrap/prevent-default e)
                 (when (js/confirm (labels :admin.center.delete/confirmation))
                   (rf/dispatch [:admin.schnaq/delete
                                 (oget e [:target :elements :private-meeting-hash :value])])))}
   [:div.form-row.align-items-center
    [:div.col-auto
     [:label
      {:for "private-meeting-hash"} (labels :admin.center.delete.private/label)]]
    [:div.col-auto
     [:input.form-control
      {:id "private-meeting-hash"}]]
    [:div.col-auto
     [:button.btn.btn-secondary {:type "submit"} (labels :admin.center.delete.public/button)]]]])

(defn- center-overview
  "The startpage of the admin center."
  []
  [pages/with-nav-and-header
   {:page/title (labels :admin.center.start/title)
    :page/heading (labels :admin.center.start/heading)
    :page/subheading (labels :admin.center.start/subheading)}
   [:div.container
    [:div
     [:h2 (labels :admin.center.delete/heading)]
     [:h4 (labels :admin.center.delete.public/heading)]
     [public-meeting-deletion-form]
     [:h4 (labels :admin.center.delete.private/heading)]
     [private-meeting-deletion-form]]
    [:hr]
    [:div
     [:h4 "Migration"]
     [migrate-starting-arguments-form]]]])

(defn center-overview-route
  []
  [center-overview])