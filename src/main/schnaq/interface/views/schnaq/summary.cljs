(ns schnaq.interface.views.schnaq.summary
  "All views and events important to extractive summaries can be found here."
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.time :as time]
            [schnaq.interface.views.pages :as pages]))

(defn- summary-request-button
  "Requests a summary or a refresh."
  [share-hash]
  (let [request-status @(rf/subscribe [:schnaq.summary/status share-hash])
        summary @(rf/subscribe [:schnaq/summary share-hash])
        locale @(rf/subscribe [:current-locale])
        button-text (case request-status
                      :request-succeeded (labels :summary.user.request-succeeded/label)
                      :requested (labels :summary.user.requested/label)
                      (labels :summary.user.not-requested/label))]
    [:section.d-block.text-center
     [:button.btn.btn-secondary
      (if request-status
        {:disabled true}
        {:on-click #(rf/dispatch [:schnaq.summary/request share-hash])})
      button-text]
     [:p.small.text-muted.mt-2
      (if summary
        [:span (labels :summary.user.status/label) (time/timestamp-with-tooltip (:summary/requested-at summary) locale)]
        (labels :summary.user/cta))]]))

(defn- summary-body
  "Contains the summary an possibly some meta information."
  [schnaq]
  (let [{:summary/keys [created-at text]} @(rf/subscribe [:schnaq/summary (:discussion/share-hash schnaq)])
        locale @(rf/subscribe [:current-locale])
        [updated-at text] (if text
                            [(time/timestamp-with-tooltip created-at locale) text]
                            ["-" "-"])]
    [:<>
     [:h2.text-center (labels :summary.user/label) (:discussion/title schnaq)]
     [:small.text-muted (labels :summary.user/last-updated) updated-at]
     [:p (str text)]]))

(defn- user-summary-view
  []
  (let [current-schnaq @(rf/subscribe [:schnaq/selected])]
    [pages/with-discussion-nav
     {:page/heading (labels :summary.user/heading)
      :page/subheading (labels :summary.user/subheading)
      :condition/needs-beta-tester? true}
     current-schnaq
     [:div.container.panel-white.mt-3
      [summary-request-button (:discussion/share-hash current-schnaq)]
      [summary-body current-schnaq]]]))

(defn public-user-view []
  [user-summary-view])

(rf/reg-event-fx
  :schnaq.summary/request
  (fn [{:keys [db]} [_ share-hash]]
    {:db (assoc-in db [:schnaq :summary :status share-hash] :requested)
     :fx [(http/xhrio-request db :post "/schnaq/summary/request" [:schnaq.summary.request/success share-hash]
                              {:share-hash share-hash})]}))

(rf/reg-event-db
  :schnaq.summary.request/success
  (fn [db [_ share-hash result]]
    (-> db
        (assoc-in [:schnaq :summary :status share-hash] :request-succeeded)
        (assoc-in [:schnaq :summary :result share-hash] (:summary result)))))

(rf/reg-sub
  :schnaq.summary/status
  (fn [db [_ share-hash]]
    (get-in db [:schnaq :summary :status share-hash])))

(rf/reg-sub
  :schnaq/summary
  (fn [db [_ share-hash]]
    (get-in db [:schnaq :summary :result share-hash])))

(rf/reg-event-fx
  :schnaq.summary/load
  (fn [{:keys [db]} [_ share-hash]]
    {:fx [(http/xhrio-request db :get "/schnaq/summary" [:schnaq.summary.load/success share-hash]
                              {:share-hash share-hash})]}))

(rf/reg-event-db
  :schnaq.summary.load/success
  (fn [db [_ share-hash result]]
    (let [{:summary/keys [created-at requested-at text] :as summary} (:summary result)]
      (cond-> (assoc-in db [:schnaq :summary :result share-hash] summary)
              (or (and requested-at (not text))             ;; Requested, but not finished
                  (and created-at requested-at (> requested-at created-at))) ; Requested update
              (assoc-in [:schnaq :summary :status share-hash] :request-succeeded)))))