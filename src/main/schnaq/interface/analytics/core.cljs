(ns schnaq.interface.analytics.core
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn- >defn]]
            [goog.string :as gstring]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.pages :as pages]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]))

(defn- analytics-card
  "A single card containing a metric and a title."
  [title metric]
  [:div.card
   [:div.card-body
    [:h5.card-title title]
    [:p.card-text.display-1 metric]
    [:p.card-text [:small.text-muted "Last updated ..."]]]])

(>defn- multi-arguments-card
  "A card containing multiple sub-metrics that are related. Uses the keys of a map
  to make sub-headings."
  [title content]
  [string? map? :ret vector?]
  [:div.card
   [:div.card-body
    [:h5.card-title title]
    (for [[metric-name metric-value] content]
      [:div {:key metric-name}
       [:p.card-text [:strong (string/capitalize (name metric-name))]]
       [:p.card-text.display-1 metric-value]
       [:hr]])
    [:p.card-text [:small.text-muted "Last updated ..."]]]])

(defn- analytics-controls
  "The controls for the analytics view."
  []
  [:div
   [:form.form-inline
    {:on-submit (fn [e]
                  (js-wrap/prevent-default e)
                  (rf/dispatch [:analytics/load-all-with-time (oget e [:target :elements :days-input :value])]))}
    [:input#days-input.form-control.form-round-05.py-1.mr-sm-2
     {:type "number"
      :name "days-input"
      :placeholder "Stats for last X days"
      :autoFocus true
      :required true
      :defaultValue 7}]
    [:input.btn.btn-outline-primary.mt-1.mt-sm-0
     {:type "submit"
      :value (labels :analytics/fetch-data-button)}]]])

(defn- analytics-dashboard-view
  "The dashboard displaying all analytics."
  []
  [pages/with-nav-and-header
   {:page/heading (labels :analytics/heading)}
   [:<>
    (let [meetings-num @(rf/subscribe [:analytics/number-of-meetings-overall])
          usernames-num @(rf/subscribe [:analytics/number-of-usernames-overall])
          average-agendas @(rf/subscribe [:analytics/number-of-average-agendas])
          statements-num @(rf/subscribe [:analytics/number-of-statements-overall])
          active-users-num @(rf/subscribe [:analytics/number-of-active-users-overall])
          statement-lengths @(rf/subscribe [:analytics/statement-lengths-stats])
          argument-types @(rf/subscribe [:analytics/argument-type-stats])
          last-meeting @(rf/subscribe [:analytics/last-meeting-created])
          last-meeting-date (str (.getDate last-meeting) "." (.getMonth last-meeting) "."
                                 (.getFullYear last-meeting))]
      [:div.container.px-5.py-3
       [analytics-controls]
       [:div.card-columns
        [analytics-card (labels :analytics/overall-meetings) meetings-num]
        [analytics-card (labels :analytics/user-numbers) usernames-num]
        [analytics-card (labels :analytics/average-agendas-title) average-agendas]
        [analytics-card (labels :analytics/statements-num-title) statements-num]
        [analytics-card (labels :analytics/active-users-num-title) active-users-num]
        [analytics-card (labels :analytics/last-meeting-created-title) last-meeting-date]
        [multi-arguments-card (labels :analytics/statement-lengths-title) statement-lengths]
        [multi-arguments-card (labels :analytics/argument-types-title) argument-types]]])]])

(defn analytics-dashboard-entrypoint []
  [analytics-dashboard-view])

;; #### Events ####

(rf/reg-event-fx
  :analytics/load-dashboard
  (fn [_ _]
    {:fx [[:dispatch [:analytics/load-meeting-num]]
          [:dispatch [:analytics/load-usernames-num]]
          [:dispatch [:analytics/load-average-number-of-agendas]]
          [:dispatch [:analytics/load-statements-num]]
          [:dispatch [:analytics/load-active-users]]
          [:dispatch [:analytics/load-statement-length-stats]]
          [:dispatch [:analytics/load-argument-type-stats]]
          [:dispatch [:analytics/load-last-meeting-date]]]}))

(>defn fetch-with-password
  "Fetches something from an endpoint with the password."
  ([db path on-success-event]
   [map? string? keyword? :ret map?]
   (fetch-with-password db path on-success-event 9999))
  ([db path on-success-event days-since]
   [map? string? keyword? int? :ret map?]
   {:fx [[:http-xhrio {:method :post
                       :uri (str (:rest-backend config) path)
                       :format (ajax/transit-request-format)
                       :params {:password (-> db :admin :password)
                                :days-since days-since}
                       :response-format (ajax/transit-response-format)
                       :on-success [on-success-event]
                       :on-failure [:ajax.error/as-notification]}]]}))

(rf/reg-event-fx
  :analytics/load-all-with-time
  (fn [{:keys [db]} [_ days]]
    (fetch-with-password db "/analytics" :analytics/all-stats-loaded days)))

(rf/reg-event-fx
  :analytics/load-meeting-num
  (fn [{:keys [db]} _]
    (fetch-with-password db "/analytics/meetings" :analytics/meeting-num-loaded)))

(rf/reg-event-fx
  :analytics/load-last-meeting-date
  (fn [{:keys [db]} _]
    (fetch-with-password db "/analytics/last-meetings" :analytics/last-meeting-created-success)))

(rf/reg-event-fx
  :analytics/load-usernames-num
  (fn [{:keys [db]} _]
    (fetch-with-password db "/analytics/usernames" :analytics/usernames-num-loaded)))

(rf/reg-event-fx
  :analytics/load-average-number-of-agendas
  (fn [{:keys [db]} _]
    (fetch-with-password db "/analytics/agendas-per-meeting" :analytics/agendas-per-meeting-loaded)))

(rf/reg-event-fx
  :analytics/load-statements-num
  (fn [{:keys [db]} _]
    (fetch-with-password db "/analytics/statements" :analytics/statements-num-loaded)))

(rf/reg-event-fx
  :analytics/load-active-users
  (fn [{:keys [db]} _]
    (fetch-with-password db "/analytics/active-users" :analytics/active-users-num-loaded)))

(rf/reg-event-fx
  :analytics/load-statement-length-stats
  (fn [{:keys [db]} _]
    (fetch-with-password db "/analytics/statement-lengths" :analytics/statement-length-stats-loaded)))

(rf/reg-event-fx
  :analytics/load-argument-type-stats
  (fn [{:keys [db]} _]
    (fetch-with-password db "/analytics/argument-types" :analytics/argument-type-stats-loaded)))

(rf/reg-event-db
  :analytics/meeting-num-loaded
  (fn [db [_ {:keys [meetings-num]}]]
    (assoc-in db [:analytics :meetings-num :overall] meetings-num)))

(rf/reg-event-db
  :analytics/usernames-num-loaded
  (fn [db [_ {:keys [usernames-num]}]]
    (assoc-in db [:analytics :usernames-num :overall] usernames-num)))

(rf/reg-event-db
  :analytics/statements-num-loaded
  (fn [db [_ {:keys [statements-num]}]]
    (assoc-in db [:analytics :statements :number :overall] statements-num)))

(rf/reg-event-db
  :analytics/active-users-num-loaded
  (fn [db [_ {:keys [active-users-num]}]]
    (assoc-in db [:analytics :active-users-num :overall] active-users-num)))

(rf/reg-event-db
  :analytics/agendas-per-meeting-loaded
  (fn [db [_ {:keys [average-agendas]}]]
    (assoc-in db [:analytics :agendas :average-per-meeting] (gstring/format "%.2f" average-agendas))))

(rf/reg-event-db
  :analytics/statement-length-stats-loaded
  (fn [db [_ {:keys [statement-length-stats]}]]
    (assoc-in db [:analytics :statements :lengths] statement-length-stats)))

(rf/reg-event-db
  :analytics/argument-type-stats-loaded
  (fn [db [_ {:keys [argument-type-stats]}]]
    (assoc-in db [:analytics :arguments :types] argument-type-stats)))

(rf/reg-event-db
  :analytics/last-meeting-created-success
  (fn [db [_ {:keys [last-created]}]]
    (assoc-in db [:analytics :meetings :last-created] last-created)))

(rf/reg-event-db
  :analytics/all-stats-loaded
  (fn [db [_ {:keys [stats]}]]
    (assoc db :analytics {:meetings-num {:overall (:meetings-num stats)}
                          :usernames-num {:overall (:usernames-num stats)}
                          :statements {:number {:overall (:statements-num stats)}
                                       :lengths (:statement-length-stats stats)}
                          :active-users-num {:overall (:active-users-num stats)}
                          :agendas {:average-per-meeting (:average-agendas stats)}
                          :arguments {:types (:argument-type-stats stats)}})))

;; #### Subs ####

(rf/reg-sub
  :analytics/number-of-meetings-overall
  (fn [db _]
    (get-in db [:analytics :meetings-num :overall])))

(rf/reg-sub
  :analytics/number-of-usernames-overall
  (fn [db _]
    (get-in db [:analytics :usernames-num :overall])))

(rf/reg-sub
  :analytics/number-of-average-agendas
  (fn [db _]
    (get-in db [:analytics :agendas :average-per-meeting])))

(rf/reg-sub
  :analytics/number-of-statements-overall
  (fn [db _]
    (get-in db [:analytics :statements :number :overall])))

(rf/reg-sub
  :analytics/number-of-active-users-overall
  (fn [db _]
    (get-in db [:analytics :active-users-num :overall])))

(rf/reg-sub
  :analytics/statement-lengths-stats
  (fn [db _]
    (get-in db [:analytics :statements :lengths])))

(rf/reg-sub
  :analytics/argument-type-stats
  (fn [db _]
    (get-in db [:analytics :arguments :types])))

(rf/reg-sub
  :analytics/last-meeting-created
  (fn [db _]
    (get-in db [:analytics :meetings :last-created])))