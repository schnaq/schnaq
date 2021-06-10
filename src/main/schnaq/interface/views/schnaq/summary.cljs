(ns schnaq.interface.views.schnaq.summary
  "All views and events important to extractive summaries can be found here."
  (:require [re-frame.core :as rf]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.pages :as pages]))

;; todo Show modal if person is not registered / not in beta
(defn- summary-request-button
  "Requests a summary or a refresh."
  [share-hash]
  [:section.d-block.text-center
   [:button.btn.btn-secondary
    {:on-click #(rf/dispatch [:schnaq.summary/request share-hash])}
    ;; todo labelize
    ;; todo button that changes text
    "Request summary"]
   ;; todo status text that changes
   [:p.small.text-muted.mt-2
    "Press the button to request a summary. It will take a few hours. The summary will appear here as soon as its done."]])

(defn- user-summary-view
  []
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
        (assoc-in [:schnaq :summary :result share-hash] result))))