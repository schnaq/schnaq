(ns schnaq.interface.components.lexical.plugins.node-changed
  (:require ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["lexical" :refer [TextNode]]
            ["react" :refer [useEffect]]
            [oops.core :refer [ocall]]
            [schnaq.interface.components.lexical.plugins.excalidraw :refer [excalidraw-changed]]))

(defn- text-changed
  "Calls the on-text-change function and fills it with the node's content."
  [^LexicalEditor editor on-text-change]
  (useEffect
   (fn []
     (ocall editor "registerNodeTransform"
            TextNode
            (fn [^TextNode node]
              (on-text-change (ocall node "getTextContent")))))
   #js [editor]))

(defn NodeChangedPlugin
  "Trigger functions if a TextNode changes in the editor."
  [{:keys [on-text-change]}]
  (let [[editor] (useLexicalComposerContext)]
    (when on-text-change (text-changed editor on-text-change))
    (excalidraw-changed editor)
    nil))
