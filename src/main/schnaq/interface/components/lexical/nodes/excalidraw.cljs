(ns schnaq.interface.components.lexical.nodes.excalidraw
  (:require ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["@lexical/react/useLexicalNodeSelection" :refer [useLexicalNodeSelection]]
            ["@lexical/utils" :refer [mergeRegister]]
            ["lexical" :refer [$getNodeByKey $getSelection $isNodeSelection
                               CLICK_COMMAND COMMAND_PRIORITY_LOW DecoratorNode
                               KEY_BACKSPACE_COMMAND KEY_DELETE_COMMAND NodeKey]]
            ["react" :refer [useCallback useEffect useMemo useRef useState]]
            [oops.core :refer [ocall oget oset!]]
            [reagent.core :as r]
            [schnaq.interface.components.lexical.nodes.excalidraw-image :refer [ExcalidrawImage]]
            [schnaq.interface.components.lexical.nodes.excalidraw-modal :refer [ExcalidrawModal]]
            [shadow.cljs.modern :refer [defclass]]))

(def ^:private data-excalidraw-attribute "data-lexical-excalidraw-json")

(declare $excalidraw-node? $create-excalidraw-node)

(defn- ExcalidrawComponent
  "The excalidraw component. Here all the magic happens and the component is
  configured."
  [properties]
  (let [data (oget properties :data)
        ^NodeKey nodeKey (oget properties :nodeKey)
        [editor] (useLexicalComposerContext)
        [modal-open? modal-open!] (useState (and (= data "[]") (not (.isReadOnly editor))))
        image-container-ref (useRef nil)
        button-ref (useRef nil)
        [selected? selected! clear-selection!] (useLexicalNodeSelection nodeKey)
        [resizing? resizing!] (useState false)

        on-delete (useCallback
                   (fn [event]
                     (when (and selected? ($isNodeSelection ($getSelection)))
                       (.preventDefault event)
                       (.update editor
                                #(let [node ($getNodeByKey nodeKey)]
                                   (when ($excalidraw-node? node)
                                     (.remove node))
                                   (selected! false))))
                     false)
                   #js [editor selected? nodeKey selected!])

        delete-node (useCallback
                     (fn []
                       (modal-open! false)
                       (.update editor #(let [node ($getNodeByKey nodeKey)]
                                          (when ($excalidraw-node? node)
                                            (ocall node "remove"))))))
        set-data (fn [new-data]
                   (when-not (.isReadOnly editor)
                     (.update editor #(let [node ($getNodeByKey nodeKey)]
                                        (when ($excalidraw-node? node)
                                          (if (pos? (oget new-data :length))
                                            (ocall node "setData" (.stringify js/JSON new-data))
                                            (ocall node "remove")))))))
        on-resize-start #(resizing! true)
        on-resize-end (fn [] (js/setTimeout #(resizing! false) 200))
        elements (useMemo #(.parse js/JSON data) #js [data])]

    (useEffect
     #(ocall editor "setReadOnly" modal-open?)
     #js [modal-open? editor])

    (useEffect
     #(mergeRegister
       (ocall editor "registerCommand" CLICK_COMMAND
              (fn [event]
                (let [button-element (oget button-ref :current)
                      event-target (oget event :target)]
                  (if resizing?
                    true
                    (when (and (not= button-element nil) (.contains button-element event-target))
                      (when-not (ocall event "shiftKey")
                        (clear-selection!))
                      (selected! (not selected?))
                      (when (pos? (oget event :detail))
                        (modal-open! true))
                      true)))
                false)
              COMMAND_PRIORITY_LOW)
       (ocall editor "registerCommand" KEY_DELETE_COMMAND on-delete COMMAND_PRIORITY_LOW)
       (ocall editor "registerCommand" KEY_BACKSPACE_COMMAND on-delete COMMAND_PRIORITY_LOW))
     #js [clear-selection! editor selected? nodeKey on-delete selected!])

    (r/as-element
     [:<>
      [:> ExcalidrawModal {:initialElements elements
                           :shown? modal-open?
                           :onDelete delete-node
                           :onSave (fn [new-data]
                                     (.setReadOnly editor false)
                                     (set-data new-data)
                                     (modal-open! false))
                           :closeOnClickOutside? true}]
      (when (pos? (count elements))
        [ExcalidrawImage {:elements elements :imageContainerRef image-container-ref :rootClassName "image"}])])))

;; -----------------------------------------------------------------------------
;; Extend the DecoratorNode to create a node.

(defn- convert-excalidraw-element [domNode]
  (when-let [excalidraw-data (.getAttribute domNode data-excalidraw-attribute)]
    (let [node ($create-excalidraw-node)]
      (oset! node :__data excalidraw-data)
      {:node node})))

(defclass ExcalidrawNode
  (field ^string __data)
  (field ^string __url)
  (extends DecoratorNode)
  (constructor [this ?data ?key]
               (super ?key)
               (oset! this :!__data (or ?data "[]"))
               (oset! this :!__url ""))
  Object
  (createDOM [this config]
             (let [span (.createElement js/document "span")
                   class-name (oget config [:theme :image])]
               (when class-name
                 (oset! span :className class-name))
               span))
  (updateDOM [_this] false)
  (exportDOM [this ^LexicalEditor editor]
             (let [element (.createElement js/document "span")
                   content (.getElementByKey editor (.getKey this))]
               (when content
                 (when-let [svg (.querySelector content "svg")]
                   (oset! element :innerHTML (oget svg :outerHTML))))
               (.setAttribute element data-excalidraw-attribute (oget this :__data))
               element))
  (setUrl [this url]
          (let [self (.getWritable this)]
            (prn ".setUrl: Setting url")
            (oset! self :__url url)))
  (getUrl [this]
          (oget this :__url))
  (hasUrl [this]
          (seq (oget this :__url)))
  (setData [this data]
           (let [self (.getWritable this)]
             (oset! self :__data data)))
  (exportJSON [this]
              {:data (oget this :__data)
               :type "excalidraw"
               :version 1})
  (decorate [this _editor]
            (r/create-element ExcalidrawComponent
                              #js {:data (oget this :__data) :nodeKey (.getKey this)})))

;; Configure static methods on our new class, because it is not possible to do
;; this inline in the `defclass` macro.
(oset! ExcalidrawNode "!importDOM"
       (fn []
         {:span (fn [^HTMLSpanElement domNode]
                  (when (.hasAttribute domNode data-excalidraw-attribute)
                    {:conversion convert-excalidraw-element
                     :priority 1}))}))
(oset! ExcalidrawNode "getType" (fn [] "excalidraw"))
(oset! ExcalidrawNode "clone"
       (fn [^ExcalidrawNode node]
         (ExcalidrawNode. (oget node :__data) (oget node :__key))))
(oset! ExcalidrawNode "importJSON"
       (fn [node]
         (ExcalidrawNode. (oget node :data) nil)))

(defn $create-excalidraw-node
  "Create an image node."
  []
  (ExcalidrawNode. nil nil))

(defn $excalidraw-node?
  "Check that `node` is an instance of `ExcalidrawNode`."
  [^LexicalNode node]
  (when node
    (instance? ExcalidrawNode node)))
