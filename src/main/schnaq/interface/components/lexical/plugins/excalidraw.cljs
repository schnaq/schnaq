(ns schnaq.interface.components.lexical.plugins.excalidraw
  (:require ["lexical" :refer [COMMAND_PRIORITY_EDITOR createCommand]]
            ["react" :refer [useEffect]]
            [goog.string :refer [format]]
            [oops.core :refer [ocall oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.lexical.nodes.excalidraw :refer [$create-excalidraw-node $excalidraw-node? ExcalidrawNode]]
            [schnaq.interface.components.lexical.nodes.excalidraw-utils :refer [elements->base64]]
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

(def ^:private never-matching-regexp
  "Element transformers must provide a regexp for markdown imports and shortcuts.
  A drawing cannot be reconstructed from its markdown representation, therefore
  this transformer only exports and its regexp never matches."
  #"(?!)")

(def excalidraw-transformer
  "Export excalidraw nodes as markdown.

  Must be an `element` transformer: `ExcalidrawNode` is a block-level
  `DecoratorNode` and therefore a direct child of the root. Lexical's markdown
  export only offers root children to `element` transformers, `text-match`
  transformers are exclusively applied to children of element nodes. A
  `text-match` transformer would never be called and the drawing would silently
  export as an empty string.

  Exports nothing until the PNG has been uploaded and the node knows its url.
  Otherwise a drawing submitted right after saving would result in a markdown
  image without a target."
  #js {:dependencies [ExcalidrawNode]
       :export (fn [^ExcalidrawNode node, _traverse-children, _selection]
                 (when (and ($excalidraw-node? node) (.hasUrl node))
                   (format "![%s](%s)" "Excalidraw drawing" (.getUrl node))))
       :regExp never-matching-regexp
       :replace (constantly false)
       :type "element"})

;; -----------------------------------------------------------------------------

(defn set-node-url
  "Helper function to provide type for infer"
  [node url]
  ^void (.setUrl node url))

(rf/reg-event-fx
 :excalidraw.elements.store/success
 (fn [_ [_ editor node {:keys [url]}]]
   {:fx [[:editor/update! [editor #(set-node-url node url)]]]}))

(rf/reg-event-fx
 :excalidraw.elements/store-png
 (fn [{:keys [db]} [_ editor node b64]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         file {:content b64
               :name "drawing.png"
               :type "image/png"}]
     (if b64
       (rf/dispatch [:file/upload share-hash file :schnaq/media [:excalidraw.elements.store/success editor node] [:ajax.error/as-notification]])
       (log/error "Could not store excalidraw image. Conversion failed.")))))

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
                    (elements->base64
                     elements
                     #(rf/dispatch [:excalidraw.elements/store-png editor node %]))))))))
   #js [editor]))
