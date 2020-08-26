(ns meetly.interface.views.graph.view
  (:require ["d3" :as d3]
            ["/graph" :as schnaqd3]
            [ajax.core :as ajax]
            [meetly.interface.config :refer [config]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]))

(defn viz [id data]
  (reagent/create-class
    {:reagent-render (fn [] [:svg {:id id}])
     :component-did-mount (fn []
                            (let [width 1200 height 600]
                              (schnaqd3/SchnaqD3. d3 (str "#" id) (clj->js (:graph data)) width height)))}))

(defn view []
  (let [graph-data @(rf/subscribe [:graph/current])]
    [:div.container
     [:h1 "Barchart"]
     (when-not (nil? graph-data)
       [viz "viz" graph-data])]))

(rf/reg-event-fx
  :graph/load-data-for-discussion
  (fn [_ [_ id share-hash]]
    {:fx [[:http-xhrio {:method :post
                        :uri (str (:rest-backend config) "/graph/discussion")
                        :params {:share-hash share-hash
                                 :discussion-id id}
                        :format (ajax/transit-request-format)
                        :response-format (ajax/transit-response-format)
                        :on-success [:graph/set-current-graph]
                        :on-failure [:ajax-failure]}]]}))

(rf/reg-event-db
  :graph/set-current-graph
  (fn [db [_ graph-data]]
    (assoc-in db [:graph :current] graph-data)))

(rf/reg-sub
  :graph/current
  (fn [db _]
    (get-in db [:graph :current])))