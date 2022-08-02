(ns schnaq.interface.components.lexical.plugins.excalidraw
  (:require ["lexical" :refer [$getSelection $isRangeSelection
                               COMMAND_PRIORITY_EDITOR createCommand]]
            [oops.core :refer [ocall]]
            [re-frame.core :as rf]
            [schnaq.interface.components.lexical.nodes.excalidraw :refer [$create-excalidraw-node ExcalidrawNode]]
            [taoensso.timbre :as log]))

(def INSERT_EXCALIDRAW_COMMAND (createCommand))

(rf/reg-fx
 :editor.plugins.register/excalidraw
 (fn [^LexicalEditor editor]
   (if-not (ocall editor "hasNodes" #js [ExcalidrawNode])
     (log/error "ExcalidrawPlugin: ExcalidrawNode not registered on editor")
     (ocall editor "registerCommand" INSERT_EXCALIDRAW_COMMAND
            (fn []
              (let [selection ($getSelection)]
                (when ($isRangeSelection selection)
                  (let [node ($create-excalidraw-node)]
                    (.insertNodes selection #js [node])))
                true))
            COMMAND_PRIORITY_EDITOR))))
