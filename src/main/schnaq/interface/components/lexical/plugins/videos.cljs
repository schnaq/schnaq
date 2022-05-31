(ns schnaq.interface.components.lexical.plugins.videos
  (:require ["lexical" :refer [$getRoot $getSelection $isRangeSelection
                               $isRootNode COMMAND_PRIORITY_EDITOR createCommand
                               LexicalCommand]]
            [oops.core :refer [ocall]]
            [re-frame.core :as rf]
            [schnaq.interface.components.lexical.nodes.video :refer [$create-video-node VideoNode]]
            [taoensso.timbre :as log]))

(def INSERT_VIDEO_COMMAND (createCommand))

(rf/reg-fx
 :editor.plugins.register/videos
 (fn [^LexicalEditor editor]
   (if-not (.hasNodes editor #js [VideoNode])
     (log/error "ImagesPlugin: ImageNode not registered on editor")
     (ocall editor "registerCommand" INSERT_VIDEO_COMMAND
            (fn [^LexicalCommand payload]
              (let [selection (or ($getSelection) (.selectEnd ($getRoot)))]
                (when ($isRangeSelection selection)
                  (when ($isRootNode (.getNode (.-anchor selection)))
                    (.insertParagraph selection))
                  (let [imageNode ($create-video-node (.-url payload))]
                    (.insertNodes selection #js [imageNode])
                    (.insertParagraph selection)))
                true))
            COMMAND_PRIORITY_EDITOR))))
