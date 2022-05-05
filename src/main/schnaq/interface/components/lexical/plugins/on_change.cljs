(ns schnaq.interface.components.lexical.plugins.on-change
  (:require ["@lexical/react/LexicalOnChangePlugin" :as OnChangePlugin]
            ["lexical" :refer [$getRoot $getSelection]]))

(defn- on-change [editorState]
  (.read editorState
         (fn []
           (let [root ($getRoot)
                 selection ($getSelection)]
             (.log js/console root selection)))))

(defn on-change-plugin
  "Plugin to trigger a function on changes in the editor."
  []
  [:> OnChangePlugin {:onChange on-change}])
