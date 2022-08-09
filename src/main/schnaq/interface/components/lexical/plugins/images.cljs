(ns schnaq.interface.components.lexical.plugins.images
  (:require ["lexical" :refer [COMMAND_PRIORITY_EDITOR createCommand LexicalCommand]]
            [oops.core :refer [ocall oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.lexical.nodes.image :refer [$create-image-node ImageNode]]
            [schnaq.interface.components.lexical.utils :refer [$insert-node-wrapped-in-paragraphs]]
            [taoensso.timbre :as log]))

(def INSERT_IMAGE_COMMAND (createCommand))

(rf/reg-fx
 :editor.plugins.register/images
 (fn [^LexicalEditor editor]
   (if-not (ocall editor "hasNodes" #js [ImageNode])
     (log/error "ImagesPlugin: ImageNode not registered on editor")
     (ocall editor "registerCommand" INSERT_IMAGE_COMMAND
            (fn [^LexicalCommand payload]
              (let [node ($create-image-node (oget payload :src) (oget payload :?altText))]
                ($insert-node-wrapped-in-paragraphs node)))
            COMMAND_PRIORITY_EDITOR))))
