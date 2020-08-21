(ns meetly.interface.views.graph.view
  (:require ["d3"]
            ["vega" :as vega]
            ["/js/schnaqd3/graph" :as schnaqd3]
            [oops.core :refer [oget]]
            [reagent.core :as reagent]))

(def graph-spec
  {:description
   "A node-link diagram with force-directed layout, depicting character co-occurrence in the novel Les Misérables.",
   :autosize "none",
   :width 700,
   :scales
   [{:name "color",
     :type "ordinal",
     :domain {:data "node-data", :field "group"},
     :range {:scheme "category20c"}}],
   :padding 0,
   :marks
   [{:name "nodes",
     :type "symbol",
     :zindex 1,
     :from {:data "node-data"},
     :on
     [{:trigger "fix",
       :modify "node",
       :values
       "fix === true ? {fx: node.x, fy: node.y} : {fx: fix[0], fy: fix[1]}"}
      {:trigger "!fix",
       :modify "node",
       :values "{fx: null, fy: null}"}],
     :encode
     {:enter
      {:fill {:scale "color", :field "group"},
       :stroke {:value "white"}},
      :update
      {:size
       {:signal "2 * nodeRadius * nodeRadius"},
       :cursor {:value "pointer"}}},
     :transform
     [{:type "force",
       :iterations 300,
       :restart {:signal "restart"},
       :static {:signal "static"},
       :signal "force",
       :forces
       [{:force "center",
         :x {:signal "cx"},
         :y {:signal "cy"}}
        {:force "collide",
         :radius {:signal "nodeRadius"}}
        {:force "nbody",
         :strength {:signal "nodeCharge"}}
        {:force "link",
         :links "link-data",
         :distance {:signal "linkDistance"}}]}]}
    {:type "path",
     :from {:data "link-data"},
     :field "name"
     :interactive false,
     :encode
     {:update
      {:stroke {:value "#000"},
       :strokeWidth {:value 0.5}}},
     :transform
     [{:type "linkpath",
       :require {:signal "force"},
       :shape "line",
       :sourceX "datum.source.x",
       :sourceY "datum.source.y",
       :targetX "datum.target.x",
       :targetY "datum.target.y"}]}],
   :$schema
   "https://vega.github.io/schema/vega/v5.json",
   :signals
   [{:name "cx", :update "width / 2"}
    {:name "cy", :update "height / 2"}
    {:name "nodeRadius",
     :value 8,
     :bind
     {:input "range", :min 1, :max 50, :step 1}}
    {:name "nodeCharge",
     :value -30,
     :bind
     {:input "range",
      :min -100,
      :max 10,
      :step 1}}
    {:name "linkDistance",
     :value 30,
     :bind
     {:input "range", :min 5, :max 100, :step 1}}
    {:name "static",
     :value true,
     :bind {:input "checkbox"}}
    {:description
     "State variable for active node fix status.",
     :name "fix",
     :value false,
     :on
     [{:events
       "symbol:mouseout[!event.buttons], window:mouseup",
       :update "false"}
      {:events "symbol:mouseover",
       :update "fix || true"}
      {:events
       "[symbol:mousedown, window:mouseup] > window:mousemove!",
       :update "xy()",
       :force true}]}
    {:description
     "Graph node most recently interacted with.",
     :name "node",
     :value nil,
     :on
     [{:events "symbol:mouseover",
       :update "fix === true ? item() : node"}]}
    {:description
     "Flag to restart Force simulation upon data changes.",
     :name "restart",
     :value false,
     :on
     [{:events {:signal "fix"},
       :update "fix && fix.length"}]}],
   :height 500,
   :data
   [{:name "node-data",
     :url "https://raw.githubusercontent.com/d3/d3-plugins/master/graph/data/miserables.json",
     :format {:type "json", :property "nodes"}}
    {:name "link-data",
     :url "https://raw.githubusercontent.com/d3/d3-plugins/master/graph/data/miserables.json",
     :format {:type "json", :property "links"}}]})

(defn render
  "Takes a json describing a vega specification and renders it."
  [spec-json id]
  (let [spec (vega/parse spec-json)
        options (clj->js {:renderer "svg"
                          :container (str "#" id)
                          :hover true})]
    (.runAsync
      (vega/View. spec options))))

(comment
  (vega/View. (vega/parse (clj->js graph-spec)))
  )

(defn vega-did-mount
  [id]
  (render (clj->js graph-spec) id))

(comment
  (defn viz [id]
    (reagent/create-class
      {:reagent-render (fn [] [:div {:id id}])
       :component-did-mount #(vega-did-mount id)})))

(defn viz [id]
  (reagent/create-class
    {:reagent-render (fn [] [:svg {:id id} "Graph, lel"])
     :component-did-mount #(schnaqd3/drawGraph "#viz")}))

(defn view []
  [:div.container
   [:h1 "Barchart"]
   [viz "viz"]])