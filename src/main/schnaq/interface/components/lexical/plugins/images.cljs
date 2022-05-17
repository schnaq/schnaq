(ns schnaq.interface.components.lexical.plugins.images
  (:require ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["lexical" :refer [$getSelection $isRangeSelection $isParagraphNode COMMAND_PRIORITY_EDITOR createCommand LexicalCommand]]
            ["react" :refer [useEffect]]
            [schnaq.interface.components.lexical.nodes.image :refer [create-image-node ImageNode]]))

(def INSERT_IMAGE_COMMAND (createCommand))

(defn ImagesPlugin
  "Plugin to include images into the editor."
  []
  (let [[editor] (useLexicalComposerContext)]
    (useEffect
     (fn []
       (when (not (.hasNodes editor #js [ImageNode]))
         (throw (new js/Error "ImagesPlugin: ImageNode not registered on editor")))
       (.registerCommand
        editor
        INSERT_IMAGE_COMMAND
        (fn [^LexicalCommand payload]
          (let [selection ($getSelection)]
            (when ($isRangeSelection selection)
              (when-not ($isParagraphNode (.getNode (.-anchor selection)))
                (.insertParagraph selection))
              (let [imageNode (create-image-node (.-src payload) (.-altText payload))]
                (.insertNodes selection #js [imageNode])
                (.insertParagraph selection)))
            true))
        COMMAND_PRIORITY_EDITOR))
     #js [editor]))
  nil)
