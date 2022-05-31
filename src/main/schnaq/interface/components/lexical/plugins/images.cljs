(ns schnaq.interface.components.lexical.plugins.images
  (:require ["lexical" :refer [$getRoot $getSelection $isRangeSelection
                               $isRootNode COMMAND_PRIORITY_EDITOR createCommand
                               LexicalCommand]]
            [oops.core :refer [ocall oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.lexical.nodes.image :refer [$create-image-node ImageNode]]
            [taoensso.timbre :as log]))

(def INSERT_IMAGE_COMMAND (createCommand))

(rf/reg-fx
 :editor.plugins.register/images
 (fn [^LexicalEditor editor]
   (if-not (ocall editor "hasNodes" #js [ImageNode])
     (log/error "ImagesPlugin: ImageNode not registered on editor")
     (ocall editor "registerCommand" INSERT_IMAGE_COMMAND
            (fn [^LexicalCommand payload]
              (let [selection (or ($getSelection) (.selectEnd ($getRoot)))]
                (when ($isRangeSelection selection)
                  (when ($isRootNode (.getNode (.-anchor selection)))
                    (.insertParagraph selection))
                  (let [image-node ($create-image-node (oget payload :src) (oget payload :?altText))]
                    (.insertNodes selection #js [image-node])
                    (.insertParagraph selection)))
                true))
            COMMAND_PRIORITY_EDITOR))))
