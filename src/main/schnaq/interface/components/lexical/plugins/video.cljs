(ns schnaq.interface.components.lexical.plugins.video
  (:require ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["lexical" :refer [$getSelection createCommand LexicalCommand COMMAND_PRIORITY_EDITOR]]
            ["react" :refer [useEffect]]
            [schnaq.interface.components.lexical.nodes.video :refer [$createVideoNode VideoNode]]))

(def INSERT_VIDEO_COMMAND (createCommand))

(defn VideoPlugin
  "Plugin to include videos into the editor."
  []
  (let [[editor] (useLexicalComposerContext)]
    (useEffect
     (fn []
       (when (not (.hasNodes editor #js [VideoNode]))
         (throw (new js/Error "VideoPlugin: VideoNode not registered on editor")))
       (.registerCommand
        editor
        INSERT_VIDEO_COMMAND
        (fn [^LexicalCommand payload]
          (.update editor
                   (fn []
                     (when-let [selection ($getSelection)]
                       (.insertNodes selection #js [($createVideoNode payload)])))
                   true))
        COMMAND_PRIORITY_EDITOR))
     #js [editor]))
  nil)
