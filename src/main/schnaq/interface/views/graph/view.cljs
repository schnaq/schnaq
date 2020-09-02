(ns schnaq.interface.views.graph.view
  (:require ["vis-network" :as vis]
            [ajax.core :as ajax]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.views.base :as base]
            [cljs.pprint :as pp]))

(defn- wrap-line
  "Takes a set of `nodes` and changes their labels to wrap properly after `break` characters."
  [size text]
  (string/join "\n"
               (re-seq (re-pattern (str ".{1," size "}\\s|.{1," size "}"))
                       (string/replace text #"\n" " "))))

(>defn- wrap-node-labels
  "Wrap the labels of all nodes inside a sequence."
  [size nodes]
  [sequential? int? :ret sequential?]
  (map #(update % :label (fn [label] (wrap-line size label))) nodes))

(>defn- node-types->colors
  "Add colors depending on node type."
  [nodes]
  [sequential? :ret sequential?]
  (map #(assoc % :color (case (:type %)
                          :argument.type/starting "#4cacf4"
                          :argument.type/support "#1292ee"
                          :argument.type/attack "#ff772d"
                          :argument.type/undercut "#ff772d"
                          :agenda "#4cacf4"
                          "#1292ee"))
       nodes))

(>defn- convert-nodes-for-vis
  "Converts the nodes received from backend specifically for viz."
  [nodes char-per-line]
  [sequential? int? :ret sequential?]
  (->> nodes
       (wrap-node-labels char-per-line)
       node-types->colors))

(defn- graph-view
  "Visualization of Discussion Graph."
  [graph]
  (let [width (.-innerWidth js/window)
        height (* 0.75 (.-innerHeight js/window))
        node-size 30
        graph (update graph :nodes #(convert-nodes-for-vis % node-size))]
    (reagent/create-class
      {:display-name "D3-Visualization of Discussion Graph"
       :reagent-render (fn [_graph] [:div#graph])
       :component-did-mount
       (fn [this]
         (pp/pprint graph)
         (let [root-node (rdom/dom-node this)
               data (clj->js graph)
               options (clj->js {:width (str width)
                                 :height (str height)})]
           (vis/Network. root-node data options)))
       :component-did-update
       (fn [this _argv]
         (let [[_ _graph] (reagent/argv this)]
           :Todo))})))

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