(ns schnaq.interface.components.lexical.plugins.images
  (:require ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["lexical" :refer [$getSelection $isRootNode $getRoot $isRangeSelection
                               COMMAND_PRIORITY_EDITOR createCommand
                               LexicalCommand]]
            ["react" :refer [useEffect]]
            [schnaq.interface.components.lexical.nodes.image :refer [$create-image-node
                                                                     ImageNode]]
            [taoensso.timbre :as log]))

(def INSERT_IMAGE_COMMAND (createCommand))

(defn ImagesPlugin
  "Plugin to include images into the editor."
  []
  (let [[editor] (useLexicalComposerContext)]
    (useEffect
     (fn []
       (if-not (.hasNodes editor #js [ImageNode])
         (log/error "ImagesPlugin: ImageNode not registered on editor")
         (.registerCommand
          editor
          INSERT_IMAGE_COMMAND
          (fn [^LexicalCommand payload]
            (let [selection (or ($getSelection) (.selectEnd ($getRoot)))]
              (when ($isRangeSelection selection)
                (when ($isRootNode (.getNode (.-anchor selection)))
                  (.insertParagraph selection))
                (let [image-node ($create-image-node (.-src payload) (.-altText payload))]
                  (.insertNodes selection #js [image-node])
                  (.insertParagraph selection)))
              true))
          COMMAND_PRIORITY_EDITOR)))
     #js [editor]))
  nil)
