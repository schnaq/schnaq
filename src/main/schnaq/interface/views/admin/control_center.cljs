(ns schnaq.interface.views.admin.control-center
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.pages :as pages]))

(rf/reg-event-fx
 :admin.schnaq/delete
 (fn [{:keys [db]} [_ share-hash]]
   {:fx [(http/xhrio-request db :delete "/admin/schnaq/delete" [:no-op]
                             {:share-hash share-hash} [:ajax.error/as-notification])]}))

(defn- private-schnaq-deletion-form
  "Easily delete any schnaq."
  []
  [:form#private-schnaq-form.form
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

(defn- migrate-survey-to-poll-form
  "Migrate the database idents of the survey to be a part of polls"
  []
  [:form#private-schnaq-form.form
   {:on-submit (fn [e]
                 (js-wrap/prevent-default e)
                 (rf/dispatch [:admin.schnaq.migrate/survey->polls]))}
   [:div.form-row.align-items-center
    [:div.col-auto
     [:button.btn.btn-secondary {:type "submit"} "Migriere Surveys zu Polls"]]]])

(rf/reg-event-fx
 :admin.schnaq.migrate/survey->polls
 (fn [{:keys [db]} _]
   {:fx [(http/xhrio-request db :post "/admin/schnaq/migrate/survey" [:no-op]
                             {}
                             [:ajax.error/as-notification])]}))

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
    [:h4 (labels :admin.center.delete.private/heading)]
    [private-schnaq-deletion-form]
    [:hr]
    [migrate-survey-to-poll-form]]])

(defn center-overview-route
  []
  [center-overview])
