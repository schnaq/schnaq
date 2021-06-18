(ns schnaq.interface.views.graph.view
  (:require ["vis-network/standalone/esm/vis-network" :refer [DataSet Network]]
            [clojure.set :as set]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.config :as conf]
            [schnaq.interface.text.display-data :refer [colors fa]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.graph.settings :as graph-settings]
            [schnaq.interface.views.loading :as loading]
            [schnaq.interface.views.schnaq.admin :as admin]))

(def ^:private graph-id "graph")

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
                   :statement.type/starting (colors :blue/light)
                   :statement.type/support (colors :blue/default)
                   :statement.type/attack (colors :orange/default)
                   :statement.type/neutral (colors :gray/medium)
                   :agenda (colors :white)
                   (colors :blue/default))]
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
       (if (< (- 100 conf/graph-controversy-upper-bound)
              controversy-score
              conf/graph-controversy-upper-bound)
         (-> %
             (assoc-in [:color :border] "#fab907")
             (assoc-in [:color :highlight :border] "#fab907")
             (assoc-in [:color :hover :border] "#fab907"))
         %))
    nodes))

(defn- node-content-color
  [nodes]
  (map
    #(let [text-color (case (:type %)
                        :agenda (colors :gray/dark)
                        (colors :white))]
       (merge % {:shape "box"
                 :shapeProperties {:borderRadius 12}
                 :widthConstraint {:minimum 50
                                   :maximum 200}
                 :font {:align "left" :color text-color}
                 :margin 10}))
    nodes))

(>defn- convert-nodes-for-vis
  "Converts the nodes received from backend specifically for viz."
  [nodes controversy-values]
  [sequential? map? :ret sequential?]
  (->> nodes
       node-types->colors
       (mark-controversy controversy-values)
       node-content-color))

(defn- graph-canvas
  "Visualization of Discussion Graph."
  [{:keys [nodes edges controversy-values]}]
  (let [nodes-vis (DataSet.)
        edges-vis (DataSet.)
        nodes-store (reagent/atom nodes)
        edges-store (reagent/atom edges)
        width (.-innerWidth js/window)
        height (* 0.75 (.-innerHeight js/window))
        route-params (get-in @(rf/subscribe [:navigation/current-route]) [:parameters :path])
        share-hash (:discussion/share-hash @(rf/subscribe [:schnaq/selected]))
        gravity @(rf/subscribe [:graph.settings/gravity])
        options {:width (str width)
                 :height (str height)
                 :layout {:randomSeed :constant}
                 :physics {:barnesHut {:avoidOverlap gravity}}}]
    (reagent/create-class
      {:display-name "Visualization of Discussion Graph"
       :reagent-render
       (fn [_graph]
         (let [^js graph-object @(rf/subscribe [:graph/get-object])
               gravity @(rf/subscribe [:graph.settings/gravity])]
           (when graph-object
             (.setOptions graph-object
                          (clj->js (assoc-in options [:physics :barnesHut :avoidOverlap]
                                             gravity))))
           [:div {:id graph-id}]))
       :component-did-mount
       (fn [this]
         (.add nodes-vis (clj->js (convert-nodes-for-vis nodes controversy-values)))
         (.add edges-vis (clj->js edges))
         (let [root-node (rdom/dom-node this)
               data #js {:nodes nodes-vis
                         :edges edges-vis}
               graph (Network. root-node data (clj->js options))]
           (rf/dispatch [:graph/store-object graph])
           (.on graph "doubleClick"
                (fn [properties]
                  (let [node-id (first (get (js->clj properties) "nodes"))]
                    (if (= node-id share-hash)              ;; if true, the user clicked on the discussion title
                      (rf/dispatch [:navigation/navigate :routes.schnaq/start {:share-hash share-hash}])
                      (rf/dispatch [:navigation/navigate :routes.schnaq.select/statement
                                    (assoc route-params :statement-id node-id)])))))))
       :component-did-update
       (fn [this _argv]
         (let [[_ {:keys [nodes edges controversy-values]}] (reagent/argv this)
               new-nodes (set/difference (set nodes) (set @nodes-store))
               new-edges (set/difference (set edges) (set @edges-store))]
           (.add nodes-vis (clj->js (convert-nodes-for-vis new-nodes controversy-values)))
           (.add edges-vis (clj->js new-edges))
           (reset! nodes-store nodes)
           (reset! edges-store edges)))
       :component-will-unmount
       (fn [_this] (rf/dispatch [:graph/set-current nil]))})))

(defn graph-agenda-header
  "Header when displaying the graph."
  [title share-hash]
  (let [go-back-fn (fn [] (rf/dispatch [:navigation/navigate :routes.schnaq/start
                                        {:share-hash share-hash}]))]
    (common/set-website-title! title)
    [:section.container-fluid.bg-white.p-4.shadow-sm
     [:div.row
      [:div.col-1.back-arrow
       [:span {:on-click go-back-fn}                        ;; the icon itself is not clickable
        [:i.arrow-icon {:class (str "m-auto fas " (fa :arrow-left))}]]]
      [:div.col-7 [:h2 title]]
      [:div.col-4.pull-right
       [graph-settings/open-settings]
       [admin/graph-download-as-png (gstring/format "#%s" graph-id)]
       [admin/txt-export share-hash title]]]]))

(defn- graph-view
  "The core Graph visualization wrapper."
  []
  (let [{:discussion/keys [share-hash title]} @(rf/subscribe [:schnaq/selected])]
    [:<>
     [graph-agenda-header title share-hash]
     (when-let [graph (:graph @(rf/subscribe [:graph/current]))]
       [graph-canvas graph])
     [loading/spinner]]))

(defn graph-view-entrypoint []
  [graph-view])


;; -----------------------------------------------------------------------------

(rf/reg-event-fx
  :graph/load-data-for-discussion
  (fn [{:keys [db]} _]
    (let [share-hash (get-in db [:current-route :parameters :path :share-hash])]
      {:fx [(http/xhrio-request db :post "/graph/discussion" [:graph/set-current] {:share-hash share-hash})]})))

(rf/reg-event-db
  :graph/set-current
  (fn [db [_ graph-data]]
    (rf/dispatch [:spinner/active! false])
    (assoc-in db [:graph :current] graph-data)))

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
