(ns schnaq.interface.components.lexical.plugins.excalidraw
  (:require ["lexical" :refer [$getSelection $isRangeSelection
                               COMMAND_PRIORITY_EDITOR createCommand]]
            ["react" :refer [useEffect]]
            [goog.string :refer [format]]
            [oops.core :refer [ocall oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.lexical.nodes.excalidraw :refer [$create-excalidraw-node ExcalidrawNode]]
            [schnaq.interface.components.lexical.nodes.excalidraw-utils :refer [convert-elements-to-svg]]
            [taoensso.timbre :as log]))

(def INSERT_EXCALIDRAW_COMMAND (createCommand))

(rf/reg-fx
 :editor.plugins.register/excalidraw
 (fn [^LexicalEditor editor]
   (if-not (ocall editor "hasNodes" #js [ExcalidrawNode])
     (log/error "ExcalidrawPlugin: ExcalidrawNode not registered on editor")
     (ocall editor "registerCommand" INSERT_EXCALIDRAW_COMMAND
            #(let [selection ($getSelection)]
               (when ($isRangeSelection selection)
                 (let [node ($create-excalidraw-node)]
                   (.insertNodes selection #js [node])))
               true)
            COMMAND_PRIORITY_EDITOR))))

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :excalidraw.elements.store/success
 (fn [_ [_ editor node {:keys [url]}]]
   {:fx [[:editor/update! [editor #(.setUrl node url)]]]}))

(rf/reg-event-fx
 :excalidraw.elements/store
 (fn [{:keys [db]} [_ editor node svg]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         svg-b64 (.btoa js/window (oget svg :outerHTML))
         file {:name "drawing.svg"
               :type "text/plain"
               :content (format "data:application/octet-stream;base64,%s" svg-b64)}]
     (rf/dispatch [:file/upload share-hash file :schnaq/media [:excalidraw.elements.store/success editor node] [:ajax.error/as-notification]]))))

(defn excalidraw-changed
  "Handle a changed excalidraw node."
  [^LexicalEditor editor]
  (useEffect
   (fn []
     (ocall editor "registerNodeTransform"
            ExcalidrawNode
            (fn [^ExcalidrawNode node]
              (let [data (oget node :__data)]
                (when-not (or (= data "[]") (.hasUrl node))
                  (let [elements (.parse js/JSON data)]
                    (convert-elements-to-svg
                     elements
                     #(rf/dispatch [:excalidraw.elements/store editor node %]))))))))
   #js [editor]))
