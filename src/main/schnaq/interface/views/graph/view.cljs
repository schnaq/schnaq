(ns schnaq.interface.views.graph.view
  (:require ["vis-network" :as vis]
            [ajax.core :as ajax]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.views.base :as base]))

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
  (map
    #(let [color (case (:type %)
                   :argument.type/starting "#4cacf4"
                   :argument.type/support "#1292ee"
                   :argument.type/attack "#ff772d"
                   :argument.type/undercut "#ff772d"
                   :agenda "#4cacf4"
                   "#1292ee")]
       (assoc % :color {:background color
                        :highlight {:background color}
                        :hover {:background color}
                        :border color}))
    nodes))

(>defn- mark-controversy
  "Marks controversy in nodes."
  [controversy-map nodes]
  [map? sequential? :ret sequential?]
  (map
    #(let [controversy-score (get controversy-map (:id %))]
       (if (< 35 controversy-score 70)
         (-> %
             (assoc-in [:color :border] "#fab907")
             (assoc-in [:color :highlight :border] "#fab907"))
         %))
    nodes))

(>defn- convert-nodes-for-vis
  "Converts the nodes received from backend specifically for viz."
  [nodes controversy-values]
  [sequential? map? :ret sequential?]
  (->> nodes
       node-types->colors
       (mark-controversy controversy-values)
       (map #(merge % {:shape "box"
                       :shapeProperties {:borderRadius 12}
                       :widthConstraint {:minimum 50
                                         :maximum 200}
                       :font {:align "left"}
                       :margin 10}))))

(defn- graph-canvas
  "Visualization of Discussion Graph."
  [graph]
  (let [vis-object (reagent/atom nil)
        width (.-innerWidth js/window)
        height (* 0.75 (.-innerHeight js/window))
        _node-size 200]
    (reagent/create-class
      {:display-name "D3-Visualization of Discussion Graph"
       :reagent-render (fn [_graph] [:div#graph])
       :component-did-mount
       (fn [this]
         (let [root-node (rdom/dom-node this)
               controversy-vals (:controversy-values graph)
               _ (println graph)
               data (clj->js (update graph :nodes #(convert-nodes-for-vis % controversy-vals)))
               options (clj->js {:width (str width)
                                 :height (str height)})]
           (reset! vis-object (vis/Network. root-node data options))))
       :component-did-update
       (fn [this _argv]
         (let [[_ new-graph] (reagent/argv this)
               controversy-vals (:controversy-values new-graph)
               new-data (clj->js (update new-graph :nodes #(convert-nodes-for-vis % controversy-vals)))]
           (reset! vis-object (.setData @vis-object new-data))))})))

(defn graph-agenda-header [agenda share-hash]
  ;; meeting header
  (let [go-back-fn (fn [] (rf/dispatch [:navigation/navigate :routes.discussion/start
                                        {:share-hash share-hash
                                         :id (:db/id (:agenda/discussion agenda))}]))]
    [base/discussion-header (:agenda/title agenda) "" go-back-fn go-back-fn]))

(defn- graph-view
  "The core Graph visualization wrapper."
  []
  (let [current-agenda @(rf/subscribe [:chosen-agenda])
        {:keys [meeting/share-hash]} @(rf/subscribe [:meeting/selected])]
    [:div
     [graph-agenda-header current-agenda share-hash]
     (when-let [graph (:graph @(rf/subscribe [:graph/current]))]
       [graph-canvas graph])]))

(defn graph-view-entrypoint []
  [graph-view])

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