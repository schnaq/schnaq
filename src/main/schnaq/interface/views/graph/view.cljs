(ns schnaq.interface.views.graph.view
  (:require ["d3" :as d3]
            ["d3-textwrap" :as d3-textwrap]
            ["/graph" :as schnaqd3]
            ["vis-network" :as vis]
            [ajax.core :as ajax]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.views.base :as base]))

(defn- graph-view
  "Visualization of Discussion Graph."
  [graph]
  (let [d3-instance (reagent/atom {})
        width (.-innerWidth js/window)
        height (* 0.75 (.-innerHeight js/window))
        node-size 50]
    (reagent/create-class
      {:display-name "D3-Visualization of Discussion Graph"
       :reagent-render (fn [_graph] [:svg])
       :component-did-mount
       (fn [this]
         (println (str "Width: " width))
         (reset! d3-instance
                 (schnaqd3/SchnaqD3.
                   d3
                   (rdom/dom-node this)
                   (clj->js graph)
                   width height node-size
                   d3-textwrap/textwrap)))
       :component-did-update
       (fn [this _argv]
         (let [[_ graph] (reagent/argv this)]
           (.replaceData @d3-instance (clj->js graph) width height node-size)))})))

(defn graph-agenda-header [agenda share-hash]
  ;; meeting header
  [base/discussion-header
   (:agenda/title agenda)
   nil
   (fn [] (rf/dispatch [:navigation/navigate :routes.discussion/start
                        {:share-hash share-hash
                         :id (:db/id (:agenda/discussion agenda))}]))])

(defn view
  "The core Graph visualization wrapper."
  []
  (let [current-agenda @(rf/subscribe [:chosen-agenda])
        {:keys [meeting/share-hash]} @(rf/subscribe [:meeting/selected])]
    [:div
     [graph-agenda-header current-agenda share-hash]
     (when-let [graph (:graph @(rf/subscribe [:graph/current]))]
       [graph-view graph])]))

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
                          :on-success [:graph/set-current]
                          :on-failure [:ajax-failure]}]]})))

(rf/reg-event-db
  :graph/set-current
  (fn [db [_ graph-data]]
    (assoc-in db [:graph :current] graph-data)))

(rf/reg-sub
  :graph/current
  (fn [db _]
    (get-in db [:graph :current])))