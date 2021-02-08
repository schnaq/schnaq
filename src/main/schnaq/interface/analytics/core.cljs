(ns schnaq.interface.analytics.core
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn- >defn]]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.auth :as auth]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.pages :as pages]))

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
     :value (labels :analytics/fetch-data-button)}]])

(defn- analytics-dashboard-view
  "The dashboard displaying all analytics."
  []
  [pages/with-nav-and-header
   {:condition/needs-administrator? true
    :page/heading (labels :analytics/heading)}
   [:<>
    (let [discussions-num @(rf/subscribe [:analytics/number-of-discussions-overall])
          usernames-num @(rf/subscribe [:analytics/number-of-usernames-overall])
          average-statements @(rf/subscribe [:analytics/number-of-average-statements])
          statements-num @(rf/subscribe [:analytics/number-of-statements-overall])
          active-users-num @(rf/subscribe [:analytics/number-of-active-users-overall])
          statement-lengths @(rf/subscribe [:analytics/statement-lengths-stats])
          argument-types @(rf/subscribe [:analytics/argument-type-stats])]
      [:div.container.px-5.py-3
       [analytics-controls]
       [:div.card-columns
        [analytics-card (labels :analytics/overall-discussions) discussions-num]
        [analytics-card (labels :analytics/user-numbers) usernames-num]
        [analytics-card (labels :analytics/average-statements-title) average-statements]
        [analytics-card (labels :analytics/statements-num-title) statements-num]
        [analytics-card (labels :analytics/active-users-num-title) active-users-num]
        [multi-arguments-card (labels :analytics/statement-lengths-title) statement-lengths]
        [multi-arguments-card (labels :analytics/argument-types-title) argument-types]]])]])

(defn analytics-dashboard-entrypoint []
  [analytics-dashboard-view])

;; #### Events ####

(rf/reg-event-fx
  :analytics/load-dashboard
  (fn [_ _]
    {:fx [[:dispatch [:analytics/load-discussions-num]]
          [:dispatch [:analytics/load-usernames-num]]
          [:dispatch [:analytics/load-average-number-of-statements]]
          [:dispatch [:analytics/load-statements-num]]
          [:dispatch [:analytics/load-active-users]]
          [:dispatch [:analytics/load-statement-length-stats]]
          [:dispatch [:analytics/load-argument-type-stats]]]}))

(>defn- fetch-statistics
  "Fetches something from an endpoint with an authentication header."
  ([db path on-success-event]
   [map? string? keyword? :ret map?]
   (fetch-statistics db path on-success-event 9999))
  ([db path on-success-event days-since]
   [map? string? keyword? int? :ret map?]
   (when (get-in db [:user :authenticated?])
     {:fx [[:http-xhrio {:method :post
                         :uri (str (:rest-backend config) path)
                         :format (ajax/transit-request-format)
                         :params {:days-since days-since}
                         :headers (auth/authentication-header db)
                         :response-format (ajax/transit-response-format)
                         :on-success [on-success-event]
                         :on-failure [:ajax.error/to-console]}]]})))

(rf/reg-event-fx
  :analytics/load-all-with-time
  (fn [{:keys [db]} [_ days]]
    (fetch-statistics db "/analytics" :analytics/all-stats-loaded days)))

(rf/reg-event-fx
  :analytics/load-discussions-num
  (fn [{:keys [db]} _]
    (fetch-statistics db "/analytics/discussions" :analytics/discussions-num-loaded)))

(rf/reg-event-fx
  :analytics/load-usernames-num
  (fn [{:keys [db]} _]
    (fetch-statistics db "/analytics/usernames" :analytics/usernames-num-loaded)))

(rf/reg-event-fx
  :analytics/load-average-number-of-statements
  (fn [{:keys [db]} _]
    (fetch-statistics db "/analytics/statements-per-discussion" :analytics/statements-per-discussion-loaded)))

(rf/reg-event-fx
  :analytics/load-statements-num
  (fn [{:keys [db]} _]
    (fetch-statistics db "/analytics/statements" :analytics/statements-num-loaded)))

(rf/reg-event-fx
  :analytics/load-active-users
  (fn [{:keys [db]} _]
    (fetch-statistics db "/analytics/active-users" :analytics/active-users-num-loaded)))

(rf/reg-event-fx
  :analytics/load-statement-length-stats
  (fn [{:keys [db]} _]
    (fetch-statistics db "/analytics/statement-lengths" :analytics/statement-length-stats-loaded)))

(rf/reg-event-fx
  :analytics/load-argument-type-stats
  (fn [{:keys [db]} _]
    (fetch-statistics db "/analytics/argument-types" :analytics/argument-type-stats-loaded)))

(rf/reg-event-db
  :analytics/discussions-num-loaded
  (fn [db [_ {:keys [discussions-num]}]]
    (assoc-in db [:analytics :discussions-num :overall] discussions-num)))

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
  :analytics/statements-per-discussion-loaded
  (fn [db [_ {:keys [average-statements]}]]
    (assoc-in db [:analytics :statements :average-per-discussion] (gstring/format "%.2f" average-statements))))

(rf/reg-event-db
  :analytics/statement-length-stats-loaded
  (fn [db [_ {:keys [statement-length-stats]}]]
    (assoc-in db [:analytics :statements :lengths] statement-length-stats)))

(rf/reg-event-db
  :analytics/argument-type-stats-loaded
  (fn [db [_ {:keys [argument-type-stats]}]]
    (assoc-in db [:analytics :arguments :types] argument-type-stats)))

(rf/reg-event-db
  :analytics/all-stats-loaded
  (fn [db [_ {:keys [stats]}]]
    (assoc db :analytics {:discussions-num {:overall (:discussions-num stats)}
                          :usernames-num {:overall (:usernames-num stats)}
                          :statements {:number {:overall (:statements-num stats)}
                                       :lengths (:statement-length-stats stats)
                                       :average-per-discussion (:average-statements stats)}
                          :active-users-num {:overall (:active-users-num stats)}
                          :arguments {:types (:argument-type-stats stats)}})))

;; #### Subs ####

(rf/reg-sub
  :analytics/number-of-discussions-overall
  (fn [db _]
    (get-in db [:analytics :discussions-num :overall])))

(rf/reg-sub
  :analytics/number-of-usernames-overall
  (fn [db _]
    (get-in db [:analytics :usernames-num :overall])))

(rf/reg-sub
  :analytics/number-of-average-statements
  (fn [db _]
    (get-in db [:analytics :statements :average-per-discussion])))

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
