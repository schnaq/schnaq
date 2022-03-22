(ns schnaq.interface.analytics.core
  (:require [clojure.string :as string]
            [com.fulcrologic.guardrails.core :refer [>defn-]]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.analytics.charts :as chart]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.clipboard :as clipboard]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.pages :as pages]))

(defn- analytics-card
  "A single card containing a metric and a title."
  [title metric]
  (let [stats @(rf/subscribe [metric])]
    [:div.col
     [:div.card
      [:div.card-body
       [:h5.card-title title]
       [:p.card-text.display-1 stats]
       [:p.card-text [:small.text-muted "Last updated ..."]]]]]))

(defn- percentage-change
  "Calculate the percentage change between two values. Color positive changes green and negative red."
  [initial-value changed-value]
  (let [change (* 100 (/ (- changed-value initial-value) initial-value))]
    [:span {:class (if (< change 0) "text-warning" "text-success")}
     (gstring/format "%s %%" change)]))

(defn- registered-users-table
  "Show registered users."
  []
  (let [users @(rf/subscribe [:analytics/registered-users])]
    [:div.card.w-100 {:style {:height "500px"
                              :overflow :auto
                              :display :inline-block}}
     [:div.card-body
      [:h5.card-title (labels :analytics.users/title)]
      [:button.btn.btn-sm.btn-outline-primary.me-3
       {:data-bs-toggle "collapse"
        :data-bs-target "#registered-users-table"
        :aria-expanded false
        :aria-controls "registered-users-table"}
       [icon :eye "me-1"] (labels :analytics.users/toggle-button)]
      [:button.btn.btn-sm.btn-outline-primary
       {:on-click #(clipboard/copy-to-clipboard! (string/join ", " (map :user.registered/email users)))}
       (labels :analytics.users/copy-button)]
      [:table#registered-users-table.table.table-striped.collapse.mt-3
       [:thead
        [:tr
         [:th (labels :analytics.users.table/name)]
         [:th (labels :analytics.users.table/email)]]]
       [:tbody
        (for [user users]
          [:tr {:key (str "registered-users-table-" (:db/id user))}
           [:td (:user.registered/display-name user)]
           [:td (:user.registered/email user)]])]]]]))

(defn- statements-stats
  "A single card containing statement-growth metrics."
  []
  (let [statements-total @(rf/subscribe [:analytics/number-of-statements-overall])
        statements-series @(rf/subscribe [:analytics/number-of-statements-series])
        values (map second statements-series)
        penultimate (last (butlast values))
        ultimate (last values)]
    [:div.card
     [:div.card-body
      [:h5.card-title (labels :analytics/statements-num-title)]
      [:p.card-text.display-5 "Overall: " statements-total " — change: " [percentage-change penultimate ultimate]]
      [chart/regular "Statements" (map first statements-series) values]
      [:p.card-text [:small.text-muted "Last updated ..."]]]]))

(>defn- multi-arguments-card
  "A card containing multiple sub-metrics that are related. Uses the keys of a map
  to make sub-headings."
  [title metric]
  [string? keyword? :ret vector?]
  (let [content @(rf/subscribe [metric])]
    [:div.col
     [:div.card
      [:div.card-body
       [:h5.card-title title]
       (for [[metric-name metric-value] content]
         [:div {:key metric-name}
          [:p.card-text [:strong (string/capitalize (name metric-name))]]
          [:p.card-text.display-1 metric-value]
          [:hr]])
       [:p.card-text [:small.text-muted "Last updated ..."]]]]]))

(defn- analytics-controls
  "The controls for the analytics view."
  []
  [:form.row
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (rf/dispatch [:analytics/load-all-with-time (oget e [:target :elements :days-input :value])]))}
   [:div.col
    [:input#days-input.form-control.form-round-05.me-sm-2
     {:type "number"
      :name "days-input"
      :placeholder "Stats for last X days"
      :autoFocus true
      :required true
      :defaultValue 30}]]
   [:div.col
    [:input.btn.btn-outline-primary.mt-1.mt-sm-0
     {:type "submit"
      :value (labels :analytics/fetch-data-button)}]]])

(defn- analytics-dashboard-view
  "The dashboard displaying all analytics."
  []
  [pages/with-nav-and-header
   {:condition/needs-analytics-admin? true
    :page/heading (labels :analytics/heading)}
   [:<>
    [:div.container.px-5.py-3
     [analytics-controls]]
    [:div.container-fluid
     [:div.row.mb-3
      [:div.col-12.col-lg-6
       [statements-stats]]
      [:div.col-12.col-lg-6
       [registered-users-table]]]
     [:div.row.row-cols-1.row-cols-lg-3.g-3
      [analytics-card (labels :analytics/overall-discussions) :analytics/number-of-discussions-overall]
      [analytics-card (labels :analytics/user-numbers) :analytics/number-of-usernames-anonymous]
      [analytics-card (labels :analytics/registered-users-numbers) :analytics/number-of-users-registered]
      [analytics-card (labels :analytics/pro-users-numbers) :analytics/number-of-users-pro]
      [analytics-card (labels :analytics/average-statements-title) :analytics/number-of-average-statements]
      [multi-arguments-card (labels :analytics/active-users-num-title) :analytics/number-of-active-users-overall]
      [multi-arguments-card (labels :analytics/statement-lengths-title) :analytics/statement-lengths-stats]
      [multi-arguments-card (labels :analytics/statement-types-title) :analytics/statement-type-stats]
      [analytics-card (labels :analytics/labels-stats) :analytics/marked-answers]]]]])

(defn analytics-dashboard-entrypoint []
  [analytics-dashboard-view])

;; #### Events ####

(rf/reg-event-fx
 :analytics/load-dashboard
 (fn [_ _]
   {:fx [[:dispatch [:analytics/load-all-with-time 30]]]}))

(>defn- fetch-statistics
  "Fetches something from an endpoint with an authentication header."
  ([db path on-success-event]
   [map? string? keyword? :ret map?]
   (fetch-statistics db path on-success-event 9999))
  ([db path on-success-event days-since]
   [map? string? keyword? int? :ret map?]
   (when (get-in db [:user :authenticated?])
     {:fx [(http/xhrio-request db :get path [on-success-event] {:days-since days-since})]})))

(rf/reg-event-fx
 :analytics/load-all-with-time
 (fn [{:keys [db]} [_ days]]
   (fetch-statistics db "/admin/analytics" :analytics/all-stats-loaded (js/parseInt days))))

(rf/reg-event-db
 :analytics/all-stats-loaded
 (fn [db [_ {:keys [statistics]}]]
   (assoc db :analytics {:discussions-sum {:overall (:discussions-sum statistics)}
                         :users-num {:anonymous (:usernames-sum statistics)
                                     :registered (:registered-users-num statistics)
                                     :pro (:pro-users-num statistics)}
                         :statements {:number (:statements-num statistics)
                                      :lengths (:statement-length-stats statistics)
                                      :average-per-discussion (:average-statements-num statistics)
                                      :types (:statement-type-stats statistics)}
                         :active-users-nums (:active-users-num statistics)
                         :labels (:labels-stats statistics)
                         :users {:registered (:users statistics)}})))

;; #### Subs ####

(rf/reg-sub
 :analytics/number-of-discussions-overall
 (fn [db _]
   (get-in db [:analytics :discussions-sum :overall])))

(rf/reg-sub
 :analytics/number-of-usernames-anonymous
 (fn [db _]
   (get-in db [:analytics :users-num :anonymous])))

(rf/reg-sub
 :analytics/number-of-users-registered
 (fn [db _]
   (get-in db [:analytics :users-num :registered])))

(rf/reg-sub
 :analytics/number-of-users-pro
 (fn [db _]
   (get-in db [:analytics :users-num :pro])))

(rf/reg-sub
 :analytics/number-of-average-statements
 (fn [db _]
   (get-in db [:analytics :statements :average-per-discussion])))

(rf/reg-sub
 :analytics/number-of-statements-overall
 (fn [db _]
   (get-in db [:analytics :statements :number :overall])))

(rf/reg-sub
 :analytics/number-of-statements-series
 (fn [db _]
   (sort-by first (get-in db [:analytics :statements :number :series]))))

(rf/reg-sub
 :analytics/number-of-active-users-overall
 (fn [db _]
   (get-in db [:analytics :active-users-nums])))

(rf/reg-sub
 :analytics/statement-lengths-stats
 (fn [db _]
   (get-in db [:analytics :statements :lengths])))

(rf/reg-sub
 :analytics/statement-type-stats
 (fn [db _]
   (get-in db [:analytics :statements :types])))

(rf/reg-sub
 :analytics/labels-stats
 (fn [db _]
   (get-in db [:analytics :labels])))

(rf/reg-sub
 :analytics/marked-answers
 :<- [:analytics/labels-stats]
 (fn [{:keys [check]} _]
   check))

(rf/reg-sub
 :analytics/registered-users
 (fn [db]
   (get-in db [:analytics :users :registered])))
