(ns schnaq.interface.components.lexical.plugins.video
  (:require ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["lexical" :refer [$getSelection createCommand LexicalCommand]]
            ["react" :refer [useEffect]]
            [schnaq.interface.components.lexical.nodes.video :refer [$createVideoNode]]))

(def INSERT_VIDEO_COMMAND (createCommand))

(defn VideoPlugin []
  (let [[editor] (useLexicalComposerContext)]
    (useEffect
     (fn []
       (.registerCommand
        editor
        INSERT_VIDEO_COMMAND
        (fn [^LexicalCommand payload]
          (.update editor
                   (fn []
                     (when-let [selection ($getSelection)]
                       (.insertNodes selection #js [($createVideoNode payload)])))
                   true))
        0))
     #js [editor]))
  nil)
