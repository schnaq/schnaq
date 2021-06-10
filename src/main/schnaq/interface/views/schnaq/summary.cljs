(ns schnaq.interface.views.schnaq.summary
  "All views and events important to extractive summaries can be found here."
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.time :as time]
            [schnaq.interface.views.pages :as pages]))

;; todo Show modal if person is not registered / not in beta
(defn- summary-request-button
  "Requests a summary or a refresh."
  [share-hash]
  (let [request-status @(rf/subscribe [:schnaq.summary/status share-hash])
        summary @(rf/subscribe [:schnaq/summary share-hash])
        locale @(rf/subscribe [:current-locale])
        button-text (case request-status
                      ;; todo labelize
                      :request-succeeded "Summary requested, please wait."
                      :requested "Requesting summary …"
                      "Request summary")]
    [:section.d-block.text-center
     [:button.btn.btn-secondary
      (if request-status
        {:disabled true}
        {:on-click #(rf/dispatch [:schnaq.summary/request share-hash])})
      button-text]
     [:p.small.text-muted.mt-2
      (if summary
        [:span "A summary is currently being generated. Last requested: "
         (time/timestamp-with-tooltip (:summary/requested-at summary) locale)]
        "Press the button to request a summary. It will take a few hours. The summary will appear here as soon as its done.")]]))

(defn- user-summary-view
  []
  ;; todo only show view when user is allowed to see it
  (let [current-schnaq @(rf/subscribe [:schnaq/selected])]
    [pages/with-discussion-nav
     ;; todo labelize
     {:page/heading "Summaries"
      :page/subheading "See the discussion in a few sentences"}
     current-schnaq
     [:div.container.panel-white.mt-3
      [summary-request-button (:discussion/share-hash current-schnaq)]]]))

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