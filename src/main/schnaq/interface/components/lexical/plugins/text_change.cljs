(ns schnaq.interface.components.lexical.plugins.text-change
  (:require ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["lexical" :refer [LexicalEditor TextNode]]
            ["react" :refer [useEffect]]
            [oops.core :refer [ocall]]))

(defn- on-text-change-fn
  "Calls the on-text-change function and fills it with the node's content."
  [^LexicalEditor editor on-text-change]
  (useEffect
   (fn []
     (ocall editor "registerNodeTransform"
            (fn [^TextNode node]
              (on-text-change (.getTextContent node)))))
   #js [editor]))

(defn TextChangePlugin
  "Trigger functions if a TextNode changes in the editor."
  [{:keys [on-text-change]}]
  (let [[editor] (useLexicalComposerContext)]
    (on-text-change-fn editor on-text-change)
    nil))
