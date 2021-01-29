(ns schnaq.interface.views.admin.control-center
  (:require [ajax.core :as ajax]
            [oops.core :refer [oget]]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.pages :as pages]
            [re-frame.core :as rf]))

(rf/reg-sub
  :migration.discussions/status
  (fn [db _]
    (get-in db [:migration :status :discussions] "-")))

(rf/reg-event-db
  :migration.discussions/success
  (fn [db _]
    (assoc-in db [:migration :status :discussions] "Migration erfolgreich fertig gestellt")))

(rf/reg-event-fx
  :migration.discussions/start
  (fn [{:keys [db]} _]
    (let [admin-pass (get-in db [:admin :password])]
      {:db (assoc-in db [:migration :status :discussions] "Läuft… Bitte Warten")
       :fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/admin/migrations/discussion-129083uehwe78fh87asd3")
                          :params {:password admin-pass}
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:migration.discussions/success]
                          :on-failure [:ajax.error/as-notification]}]]})))

(defn- migrate-discussions-form
  "Migrates the discussions to the new format."
  []
  [:form.form
   {:id "migrate-discussions-form"
    :on-submit (fn [e]
                 (js-wrap/prevent-default e)
                 (when (js/confirm "Diskussionen wirklich migrieren? Nicht nochmal klicken, wenn gestartet!")
                   (rf/dispatch [:migration.discussions/start])))}
   [:button.btn.btn-danger {:type "submit"} "Migriere Diskussionen JETZT!"]
   [:p "Status: " @(rf/subscribe [:migration.discussions/status])]])

(rf/reg-sub
  :migration.users/status
  (fn [db _]
    (get-in db [:migration :status :users] "-")))

(rf/reg-event-db
  :migration.users/success
  (fn [db _]
    (assoc-in db [:migration :status :users] "Migration erfolgreich fertig gestellt")))

(rf/reg-event-fx
  :migration.users/start
  (fn [{:keys [db]} _]
    (let [admin-pass (get-in db [:admin :password])]
      {:db (assoc-in db [:migration :status :users] "Läuft… Bitte Warten")
       :fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/admin/migrations/users-89hjasd-123897dha")
                          :params {:password admin-pass}
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:migration.users/success]
                          :on-failure [:ajax.error/as-notification]}]]})))

(defn- migrate-users-form
  "Migrates the users to the new format."
  []
  [:form.form
   {:id "migrate-users-form"
    :on-submit (fn [e]
                 (js-wrap/prevent-default e)
                 (when (js/confirm "Nutzer wirklich migrieren? Nicht nochmal klicken, wenn gestartet!")
                   (rf/dispatch [:migration.users/start])))}
   [:button.btn.btn-danger {:type "submit"} "Migriere Nutzer JETZT!"]
   [:p "Status: " @(rf/subscribe [:migration.users/status])]])

(rf/reg-event-db
  :admin.schnaq.delete/success
  (fn [db [_ {:keys [share-hash]}]]
    (let [public-schnaqs (get-in db [:meetings :public])
          new-public-schnaqs (remove #(= share-hash (:meeting/share-hash %)) public-schnaqs)]
      (assoc-in db [:meetings :public] new-public-schnaqs))))

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
                                 (oget e [:target :elements :public-meeting-select :value])])))}
   [:div.form-row.align-items-center
    [:div.col-auto
     [:label
      {:for "public-meeting-select"} (labels :admin.center.delete.public/label)]]
    (let [public-meetings @(rf/subscribe [:meetings/public])]
      [:div.col-auto
       [:select.form-control
        {:id "public-meeting-select"}
        (for [meeting public-meetings]
          [:option
           {:key (:meeting/share-hash meeting)
            :value (:meeting/share-hash meeting)}
           (:meeting/title meeting)])]])
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
     [migrate-users-form]
     [migrate-discussions-form]]]])

(defn center-overview-route
  []
  [center-overview])