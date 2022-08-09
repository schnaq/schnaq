(ns schnaq.interface.components.lexical.plugins.excalidraw
  (:require ["lexical" :refer [COMMAND_PRIORITY_EDITOR createCommand]]
            ["react" :refer [useEffect]]
            [goog.string :refer [format]]
            [oops.core :refer [ocall oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.lexical.nodes.excalidraw :refer [$create-excalidraw-node $excalidraw-node? ExcalidrawNode]]
            [schnaq.interface.components.lexical.nodes.excalidraw-utils :refer [convert-elements-to-svg]]
            [schnaq.interface.components.lexical.utils :refer [$insert-node-wrapped-in-paragraphs]]
            [taoensso.timbre :as log]))

(def INSERT_EXCALIDRAW_COMMAND (createCommand))

(rf/reg-fx
 :editor.plugins.register/excalidraw
 (fn [^LexicalEditor editor]
   (if-not (ocall editor "hasNodes" #js [ExcalidrawNode])
     (log/error "ExcalidrawPlugin: ExcalidrawNode not registered on editor")
     (ocall editor "registerCommand" INSERT_EXCALIDRAW_COMMAND
            #($insert-node-wrapped-in-paragraphs ($create-excalidraw-node))
            COMMAND_PRIORITY_EDITOR))))

;; -----------------------------------------------------------------------------

(def excalidraw-transformer
  "Export / import excalidraw nodes as markdown."
  #js {:export (fn [^ExcalidrawNode node, _export-children, _export-format]
                 (when ($excalidraw-node? node)
                   (format "![%s](%s)" "Excalidraw drawing" (.getUrl node))))
       :type "text-match"})

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
               :type "image/svg+xml"
               :content (format "data:image/svg+xml;base64,%s" svg-b64)}]
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
