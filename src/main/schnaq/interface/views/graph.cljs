(ns schnaq.interface.views.graph
  (:require ["remove-markdown" :as remove-markdown]
            ["vis-network/standalone/esm/vis-network" :refer [DataSet Network]]
            [clojure.set :as set]
            [clojure.string :as str]
            [com.fulcrologic.guardrails.core :refer [=> >defn-]]
            [oops.core :refer [oget oset!]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom.server :as rserver]
            [schnaq.interface.components.colors :refer [colors]]
            [schnaq.interface.config :as config :refer [graph-label-length]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.markdown :refer [as-markdown]]
            [schnaq.interface.utils.toolbelt :refer [truncate-to-n-chars-string]]
            [schnaq.interface.views.loading :as loading]
            [schnaq.interface.views.pages :as pages]))

(>defn- node-types->colors
  "Add colors depending on node type."
  [nodes]
  [:graph/nodes :ret :graph/nodes]
  (map
   #(let [color (case (:type %)
                  :statement.type/starting (colors :neutral/dark)
                  :statement.type/support (colors :positive/default)
                  :statement.type/attack (colors :negative/default)
                  :statement.type/neutral (colors :neutral/medium)
                  :agenda (colors :white)
                  (colors :positive/default))
          border-color (case (:type %)
                         :agenda (colors :neutral/medium)
                         color)]
      (assoc % :color {:background color
                       :highlight {:background color}
                       :hover {:background color}
                       :border border-color}))
   nodes))

(>defn- mark-controversy
  "Marks controversy in nodes."
  [controversy-map nodes]
  [:graph/controversy-values :graph/nodes => sequential?]
  (map
   #(let [controversy-score (get controversy-map (:id %))]
      (if (< (- 100 config/graph-controversy-upper-bound)
             controversy-score
             config/graph-controversy-upper-bound)
        (-> %
            (assoc-in [:color :border] "#fab907")
            (assoc-in [:color :highlight :border] "#fab907")
            (assoc-in [:color :hover :border] "#fab907"))
        %))
   nodes))

(>defn- node-content-color
  [nodes]
  [:graph/nodes => :graph/nodes]
  (map
   #(let [text-color (case (:type %)
                       :agenda (colors :neutral/dark)
                       (colors :white))]
      (merge % {:shape "box"
                :shapeProperties {:borderRadius 12}
                :widthConstraint {:minimum 50
                                  :maximum 200}
                :font {:align "left" :color text-color}
                :margin 10}))
   nodes))

(>defn- add-html-title-from-labels
  "Take the original content of a node and render it to the title field."
  [nodes]
  [:graph/nodes => :graph/nodes]
  (map (fn [node]
         (let [dom-element (.createElement js/document "div")
               label-html (rserver/render-to-string (as-markdown (:label node)))]
           (oset! dom-element :innerHTML label-html)
           (.add (oget dom-element :classList) "graph-node")
           (assoc node :title dom-element)))
       nodes))

(>defn- remove-markdown-from-nodes
  "Removes markdown from the label of a node."
  [nodes]
  [:graph/nodes => :graph/nodes]
  (map (fn [node] (update node :label #(-> % remove-markdown str/trim))) nodes))

(>defn- remove-empty-nodes
  "Remove empty nodes, if the labels were reduced to an empty strings due to
  processing steps."
  [nodes]
  [:graph/nodes => :graph/nodes]
  (remove #(empty? (:label %)) nodes))

(>defn- shorten-labels
  "Shorten long statements."
  [nodes]
  [:graph/nodes => :graph/nodes]
  (map (fn [node] (update node :label #(truncate-to-n-chars-string % graph-label-length)))
       nodes))

(>defn- convert-nodes-for-vis
  "Converts the nodes received from backend specifically for viz."
  [nodes controversy-values]
  [:graph/nodes :graph/controversy-values :ret :graph/nodes]
  (->> nodes
       node-types->colors
       add-html-title-from-labels
       remove-markdown-from-nodes
       (mark-controversy controversy-values)
       node-content-color
       shorten-labels
       remove-empty-nodes))

;; -----------------------------------------------------------------------------

(defn- graph-canvas
  "Visualization of Discussion Graph."
  [{:keys [nodes edges controversy-values]}]
  (let [nodes-vis (DataSet.)
        edges-vis (DataSet.)
        dom-node (atom nil)
        nodes-store (reagent/atom nodes)
        edges-store (reagent/atom edges)
        height (* 0.75 (.-innerHeight js/window))
        route-params (get-in @(rf/subscribe [:navigation/current-route]) [:parameters :path])
        share-hash (:discussion/share-hash @(rf/subscribe [:schnaq/selected]))
        gravity @(rf/subscribe [:graph.settings/gravity])
        options {:height (str height)
                 :layout {:randomSeed :constant}
                 :physics {:barnesHut {:avoidOverlap gravity}}}]
    (reagent/create-class
     {:display-name "Visualization of Discussion Graph"
      :reagent-render
      (fn [_graph]
        (let [graph-object @(rf/subscribe [:graph/get-object])
              gravity @(rf/subscribe [:graph.settings/gravity])]
          (when graph-object
            (.setOptions graph-object
                         (clj->js (assoc-in options [:physics :barnesHut :avoidOverlap]
                                            gravity)))
            ;; Disable gravitation / physics after graph is stabilized
            (.on graph-object "stabilizationIterationsDone"
                 #(.setOptions graph-object (clj->js {:physics false}))))
          [:div {:id config/graph-id
                 :ref #(reset! dom-node %)}]))
      :component-did-mount
      (fn [_this]
        (.add nodes-vis (clj->js (convert-nodes-for-vis nodes controversy-values)))
        (.add edges-vis (clj->js edges))
        (let [data #js {:nodes nodes-vis
                        :edges edges-vis}
              graph (Network. @dom-node data (clj->js options))]
          (rf/dispatch [:graph/store-object graph])
          (rf/dispatch [:tour/start :mindmap])
          (.on graph "doubleClick"
               (fn [properties]
                 (when-let [clicked-node-id (first (get (js->clj properties) "nodes"))] ;; If `clicked-node-id` is nil, the user clicked in an empty space instead of a node
                   (if (= clicked-node-id share-hash) ;; if true, the user clicked on the discussion title
                     (rf/dispatch [:navigation/navigate :routes.schnaq/start {:share-hash share-hash}])
                     (rf/dispatch [:navigation/navigate :routes.schnaq.select/statement
                                   (assoc route-params :statement-id clicked-node-id)])))))))
      :component-did-update
      (fn [this _argv]
        (let [[_ {:keys [nodes edges controversy-values]}] (reagent/argv this)
              new-nodes (set/difference (set nodes) (set @nodes-store))
              new-edges (set/difference (set edges) (set @edges-store))]
          (.add nodes-vis (clj->js (convert-nodes-for-vis new-nodes controversy-values)))
          (.add edges-vis (clj->js new-edges))
          (reset! nodes-store nodes)
          (reset! edges-store edges)))
      :component-will-unmount #(rf/dispatch [:graph/reset])})))

(defn- graph-view
  "The core Graph visualization wrapper."
  []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])]
    [pages/with-discussion-header
     {:page/heading (:discussion/title current-discussion)}
     [:<>
      (if-let [graph (:graph @(rf/subscribe [:graph/current]))]
        [graph-canvas graph]
        [:div.spinner-position
         [loading/spinner-icon]])]]))

(defn graph-view-entrypoint []
  [graph-view])

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :graph/load-data-for-discussion
 (fn [{:keys [db]} _]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:fx [(http/xhrio-request db :get "/discussion/graph" [:graph/set-current] {:share-hash share-hash})]})))

(rf/reg-event-db
 :graph/set-current
 (fn [db [_ graph-data]]
   (when graph-data
     (assoc-in db [:graph :current] graph-data))))

(rf/reg-event-db
 :graph/reset
 (fn [db]
   (dissoc db :graph)))

(rf/reg-sub
 :graph/current
 (fn [db _]
   (get-in db [:graph :current])))

(rf/reg-event-db
 :graph/store-object
 (fn [db [_ graph-js]]
   (assoc-in db [:graph :object] graph-js)))

(rf/reg-sub
 :graph/get-object
 (fn [db _]
   (get-in db [:graph :object])))

(rf/reg-event-db
 :graph.settings/gravity!
 (fn [db [_ value]]
   (assoc-in db [:graph :settings :gravity] value)))

(rf/reg-sub
 :graph.settings/gravity
 (fn [db _]
   (let [default-gravity 0.1]
     (get-in db [:graph :settings :gravity] default-gravity)))) ;
