#_(ns schnaq.interface.components.slate
    #_(:require ["slate" :refer [createEditor]]
                ["slate-react" :refer [Slate Editable withReact]]))
(ns schnaq.interface.components.slate
  (:require ["react" :refer [useState]]
            ["slate" :refer [createEditor]]
            ["slate-react" :refer [Slate Editable withReact]]))

(def initialValue #js [])

(defn slate
  []
  (let [[editor] (useState (fn [] (withReact (createEditor))))]
    [:f> Slate #js {:editor editor, :value initialValue}]))

(defn- build-page []
  [:<>
   [:h2 "Slate"]
   [:f> slate]])

(defn page []
  [build-page])
