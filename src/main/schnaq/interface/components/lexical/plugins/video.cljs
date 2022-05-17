(ns schnaq.interface.components.lexical.plugins.video
  (:require ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["lexical" :refer [$getSelection $isRangeSelection $isRootNode createCommand LexicalCommand COMMAND_PRIORITY_EDITOR]]
            ["react" :refer [useEffect]]
            [schnaq.interface.components.lexical.nodes.video :refer [create-video-node VideoNode]]))

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
          (let [selection ($getSelection)]
            (when ($isRangeSelection selection)
              (when ($isRootNode (.getNode (.-anchor selection)))
                (.insertParagraph selection))
              (let [imageNode (create-video-node (.-url payload))]
                (.insertNodes selection #js [imageNode])
                (.insertParagraph selection)))
            true))
        COMMAND_PRIORITY_EDITOR))
     #js [editor]))
  nil)
