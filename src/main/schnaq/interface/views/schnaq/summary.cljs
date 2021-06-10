(ns schnaq.interface.views.schnaq.summary
  "All views and events important to extractive summaries can be found here."
  (:require [schnaq.interface.views.pages :as pages]
            [re-frame.core :as rf]))

(defn- summary-request-button
  "Requests a summary or a refresh."
  [schnaq]
  [:section.d-block.text-center
   [:button.btn.btn-secondary
    ;; todo dispatch method
    {:on-click #(rf/dispatch [:todo schnaq])}
    ;; todo labelize
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
      [summary-request-button current-schnaq]]]))

(defn public-user-view []
  [user-summary-view])