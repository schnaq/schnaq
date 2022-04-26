(ns schnaq.interface.components.plugins.tree-view
  (:require ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["@lexical/react/LexicalTreeView" :as TreeView]))

(defn tree-view-plugin
  "Show the structure of the editor's content in a tree view."
  []
  (let [[editor] (useLexicalComposerContext)]
    [:> TreeView
     {:viewClassName "tree-view-output",
      :timeTravelPanelClassName "debug-timetravel-panel",
      :timeTravelButtonClassName "debug-timetravel-button",
      :timeTravelPanelSliderClassName "debug-timetravel-panel-slider",
      :timeTravelPanelButtonClassName "debug-timetravel-panel-button",
      :editor editor}]))
