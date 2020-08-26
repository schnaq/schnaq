(ns meetly.interface.views.graph.view
  (:require ["d3" :as d3]
            ["/graph" :as schnaqd3]
            [ajax.core :as ajax]
            [meetly.interface.config :refer [config]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]))

(defn viz [graph]
  (let [d3-instance (reagent/atom {})
        width 1200 height 600 node-size 5]
    (reagent/create-class
      {:display-name "D3-Visualization of Discussion Graph"
       :reagent-render (fn [_graph] [:svg])
       :component-did-mount (fn [this]
                              (reset! d3-instance
                                      (schnaqd3/SchnaqD3. d3 (rdom/dom-node this) (clj->js graph) width height)))
       :component-did-update (fn [this _argv]
                               (let [[_ graph] (reagent/argv this)]
                                 (.replaceData @d3-instance (clj->js graph) width height node-size)))})))

(defn view []
  [:div.container
   [:h1 "Überblick"]
   (when-let [graph (:graph @(rf/subscribe [:graph/current]))]
     [viz graph])])

(rf/reg-event-fx
  :graph/load-data-for-discussion
  (fn [{:keys [db]} _]
    (let [{:keys [id share-hash]} (get-in db [:current-route :parameters :path])]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/graph/discussion")
                          :params {:share-hash share-hash
                                   :discussion-id id}
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:graph/set-current-graph]
                          :on-failure [:ajax-failure]}]]})))

(rf/reg-event-db
  :graph/set-current-graph
  (fn [db [_ graph-data]]
    (assoc-in db [:graph :current] graph-data)))

(rf/reg-sub
  :graph/current
  (fn [db _]
    (get-in db [:graph :current])))