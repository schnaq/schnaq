(ns schnaq.interface.views.graph.view
  (:require ["vis-network/standalone/esm/vis-network" :refer [DataSet Network]]
            [ajax.core :as ajax]
            [clojure.set :as set]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.config :refer [config] :as conf]
            [schnaq.interface.text.display-data :refer [colors fa]]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.meeting.admin-buttons :as admin-buttons]
            [schnaq.interface.views.spinner.spinner :as spinner]))

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
                   :argument.type/starting (colors :blue/light)
                   :argument.type/support (colors :blue/default)
                   :argument.type/attack (colors :orange/default)
                   :argument.type/undercut (colors :orange/default)
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
       (node-content-color)))

(defn- graph-canvas
  "Visualization of Discussion Graph."
  [{:keys [nodes edges controversy-values]}]
  (let [nodes-vis (reagent/atom (DataSet.))
        edges-vis (reagent/atom (DataSet.))
        nodes-store (reagent/atom nodes)
        edges-store (reagent/atom edges)
        width (.-innerWidth js/window)
        height (* 0.75 (.-innerHeight js/window))
        route-params (get-in @(rf/subscribe [:navigation/current-route]) [:parameters :path])]
    (reagent/create-class
      {:display-name "Visualization of Discussion Graph"
       :reagent-render (fn [_graph] [:div {:id graph-id}])
       :component-did-mount
       (fn [this]
         (.add @nodes-vis (clj->js (convert-nodes-for-vis nodes controversy-values)))
         (.add @edges-vis (clj->js edges))
         (let [root-node (rdom/dom-node this)
               data #js {:nodes @nodes-vis
                         :edges @edges-vis}
               options (clj->js {:width (str width)
                                 :height (str height)
                                 :physics {:barnesHut {:avoidOverlap 0.02}}})]
           (.on (Network. root-node data options) "doubleClick"
                (fn [properties]
                  (let [node-id (first (get (js->clj properties) "nodes"))]
                    (rf/dispatch [:navigation/navigate :routes.schnaq.select/statement
                                  (assoc route-params :statement-id node-id)]))))))
       :component-did-update
       (fn [this _argv]
         (let [[_ {:keys [nodes edges controversy-values]}] (reagent/argv this)
               new-nodes (set/difference (set nodes) (set @nodes-store))
               new-edges (set/difference (set edges) (set @edges-store))]
           (.add @nodes-vis (clj->js (convert-nodes-for-vis new-nodes controversy-values)))
           (.add @edges-vis (clj->js new-edges))
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
    [:div.container-fluid.bg-white.p-4.shadow-sm
     [:div.row
      [:div.col-1.back-arrow
       [:span {:on-click go-back-fn}                        ;; the icon itself is not clickable
        [:i.arrow-icon {:class (str "m-auto fas " (fa :arrow-left))}]]]
      [:div.col-9 [:h2 title]]
      [:div.col-2.pull-right
       [admin-buttons/graph-download-as-png (gstring/format "#%s" graph-id)]
       [admin-buttons/txt-export share-hash title]]]]))

(defn- graph-view
  "The core Graph visualization wrapper."
  []
  (let [{:keys [meeting/share-hash meeting/title]} @(rf/subscribe [:meeting/selected])]
    [:<>
     [graph-agenda-header title share-hash]
     (when-let [graph (:graph @(rf/subscribe [:graph/current]))]
       [graph-canvas graph])
     [spinner/view true]]))

(defn graph-view-entrypoint []
  [graph-view])

(rf/reg-event-fx
  :graph/load-data-for-discussion
  (fn [{:keys [db]} _]
    (let [share-hash (get-in db [:current-route :parameters :path :share-hash])]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/graph/discussion")
                          :params {:share-hash share-hash}
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:graph/set-current]
                          :on-failure [:ajax.error/to-console]}]]})))

(rf/reg-event-db
  :graph/set-current
  (fn [db [_ graph-data]]
    (spinner/set-spinner-loading! false)
    (assoc-in db [:graph :current] graph-data)))

(rf/reg-sub
  :graph/current
  (fn [db _]
    (get-in db [:graph :current])))