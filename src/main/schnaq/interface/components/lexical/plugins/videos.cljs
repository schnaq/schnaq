(ns schnaq.interface.components.lexical.plugins.videos
  (:require ["lexical" :refer [COMMAND_PRIORITY_EDITOR createCommand
                               LexicalCommand]]
            [oops.core :refer [ocall oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.lexical.nodes.video :refer [$create-video-node VideoNode]]
            [schnaq.interface.components.lexical.utils :refer [$insert-node-wrapped-in-paragraphs]]
            [taoensso.timbre :as log]))

(def INSERT_VIDEO_COMMAND (createCommand))

(rf/reg-fx
 :editor.plugins.register/videos
 (fn [^LexicalEditor editor]
   (if-not (ocall editor "hasNodes" #js [VideoNode])
     (log/error "ImagesPlugin: VideoNode not registered on editor")
     (ocall editor "registerCommand" INSERT_VIDEO_COMMAND
            (fn [^LexicalCommand payload]
              (let [node ($create-video-node (oget payload :url))]
                ($insert-node-wrapped-in-paragraphs node)))
            COMMAND_PRIORITY_EDITOR))))
