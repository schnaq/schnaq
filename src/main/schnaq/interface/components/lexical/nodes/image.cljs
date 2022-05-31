(ns schnaq.interface.components.lexical.nodes.image
  (:require ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["@lexical/react/useLexicalNodeSelection" :as useLexicalNodeSelection]
            ["@lexical/utils" :refer [mergeRegister]]
            ["lexical" :refer [$getNodeByKey $getSelection $isNodeSelection
                               CLICK_COMMAND COMMAND_PRIORITY_LOW DecoratorNode
                               EditorConfig KEY_BACKSPACE_COMMAND KEY_DELETE_COMMAND NodeKey]]
            ["react" :refer [useCallback useEffect useRef]]
            [oops.core :refer [ocall oget oset!]]
            [reagent.core :as r]
            [shadow.cljs.modern :refer [defclass]]))

(declare $image-node?)

(defn- ImageComponent
  "The real image component. Here all the magic happens and the component is
  configured."
  [properties]
  (let [src (oget properties :src)
        altText (oget properties :alt)
        ^NodeKey nodeKey (oget properties :nodeKey)
        ref (useRef nil)
        [selected? set-selected clear-selection] (useLexicalNodeSelection nodeKey)
        [editor] (useLexicalComposerContext)
        on-delete (useCallback
                   (fn [event]
                     (when (and selected? ($isNodeSelection ($getSelection)))
                       (.preventDefault event)
                       (.update editor
                                #(let [node ($getNodeByKey nodeKey)]
                                   (when ($image-node? node)
                                     (.remove node))
                                   (set-selected false))))
                     false)
                   #js [editor selected? nodeKey set-selected])]
    (useEffect
     (fn []
       (mergeRegister
        (ocall editor "registerCommand" CLICK_COMMAND
               (fn [event]
                 (when (= (oget event :target) (oget ref :current))
                   (when-not (oget event :shiftKey) (clear-selection))
                   (set-selected (not selected?))
                   true)
                 false)
               COMMAND_PRIORITY_LOW)
        (ocall editor "registerCommand" KEY_DELETE_COMMAND on-delete COMMAND_PRIORITY_LOW)
        (ocall editor "registerCommand" KEY_BACKSPACE_COMMAND on-delete COMMAND_PRIORITY_LOW)))
     #js [clear-selection editor selected? nodeKey on-delete set-selected])
    (r/as-element
     [:div.editor-image
      [:img
       {:class (when selected? "focused")
        :src src
        :alt altText
        :ref ref}]])))

;; -----------------------------------------------------------------------------
;; Extend the DecoratorNode to create an image node.

(defclass ImageNode
  (field ^string __src)
  (field ^string __altText)
  (field ^NodeKey __key)
  (extends DecoratorNode)
  (constructor [_this src altText ?key]
               (super ?key)
               (set! __src src)
               (set! __altText altText))
  Object
  (createDOM [this ^EditorConfig config]
             (let [div (.createElement js/document "div")]
               (oset! div ["style" "display"] "contents")
               div))
  (updateDOM [_this] false)
  (getSrc [this] (oget this "__src"))
  (getAltText [this] (or (oget this "__altText") ""))
  (decorate [this ^LexicalEditor _editor]
            (r/create-element ImageComponent #js {:src (oget this "__src") :alt (oget this "__altText") :nodeKey (.getKey this)})))

;; Configure static methods on our new class, because it is not possible to do
;; this inline in the `defclass` macro.
(oset! ImageNode "getType" (fn [] "image"))
(oset! ImageNode "clone"
       (fn [^ImageNode node]
         (ImageNode. (oget node "__src") (oget node "__altText") (oget node "__key"))))

(defn $create-image-node
  "Create an image node."
  [src altText]
  (ImageNode. src (or altText "") nil))

(defn $image-node?
  "Check that `node` is an instance of `ImageNode`."
  [^LexicalNode node]
  (when node
    (instance? ImageNode node)))
