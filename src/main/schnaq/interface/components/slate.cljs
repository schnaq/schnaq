(ns schnaq.interface.components.slate
  (:require ["react" :refer [createElement useState]]
            ["slate" :refer [createEditor]]
            ["slate-react" :refer [Editable Slate withReact]]
            [reagent.core :as r]))

(def initialValue
  #js [#js {:type "paragraph"
            :children #js [#js {:text "This is text"}]}])

(defn slate
  []
  (let [[editor] (useState #(withReact (createEditor)))]
    (r/create-element Slate #js {:editor editor :value initialValue}
                      (r/create-element Editable))))

(defn- build-page []
  [:<>
   [:h2 "Slate"]
   [:f> slate]])

(defn page []
  [build-page])
