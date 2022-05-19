(ns schnaq.interface.components.lexical.plugins.video
  (:require ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["lexical" :refer [$getSelection $isRootNode $getRoot $isRangeSelection
                               COMMAND_PRIORITY_EDITOR createCommand
                               LexicalCommand]]
            ["react" :refer [useEffect]]
            [schnaq.interface.components.lexical.nodes.video :refer [create-video-node
                                                                     VideoNode]]
            [taoensso.timbre :as log]))

(def INSERT_VIDEO_COMMAND (createCommand))

(defn VideoPlugin
  "Plugin to include videos into the editor."
  []
  (let [[editor] (useLexicalComposerContext)]
    (useEffect
     (fn []
       (if-not (.hasNodes editor #js [VideoNode])
         (log/error "VideoPlugin: VideoNode not registered on editor")
         (.registerCommand
          editor
          INSERT_VIDEO_COMMAND
          (fn [^LexicalCommand payload]
            (let [selection (or ($getSelection) (.selectEnd ($getRoot)))]
              (when ($isRangeSelection selection)
                (when ($isRootNode (.getNode (.-anchor selection)))
                  (.insertParagraph selection))
                (let [imageNode (create-video-node (.-url payload))]
                  (.insertNodes selection #js [imageNode])
                  (.insertParagraph selection)))
              true))
          COMMAND_PRIORITY_EDITOR)))
     #js [editor]))
  nil)
