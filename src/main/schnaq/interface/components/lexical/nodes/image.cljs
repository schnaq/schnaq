(ns schnaq.interface.components.lexical.nodes.image
  (:require ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["@lexical/react/useLexicalNodeSelection" :as useLexicalNodeSelection]
            ["@lexical/utils" :refer [mergeRegister]]
            ["lexical" :refer [$getNodeByKey $getSelection $isNodeSelection
                               CLICK_COMMAND COMMAND_PRIORITY_LOW DecoratorNode
                               EditorConfig KEY_BACKSPACE_COMMAND KEY_DELETE_COMMAND NodeKey]]
            ["react" :refer [useCallback useEffect useRef]]
            [oops.core :refer [oget oset!]]
            [reagent.core :as r]
            [shadow.cljs.modern :refer [defclass]]))

(declare image-node?)

(defn- ImageComponent
  "The real image component. Here all the magic happens and the component is
  configured."
  [properties]
  (let [src (.-src properties)
        altText (.-alt properties)
        nodeKey ^NodeKey (.-nodeKey properties)
        ref (useRef nil)
        [selected? set-selected clear-selection] (useLexicalNodeSelection nodeKey)
        [editor] (useLexicalComposerContext)
        on-delete (useCallback
                   (fn [event]
                     (when (and selected? ($isNodeSelection ($getSelection)))
                       (.preventDefault event)
                       (.update editor
                                #(let [node ($getNodeByKey nodeKey)]
                                   (when (image-node? node)
                                     (.remove node))
                                   (set-selected false))))
                     false)
                   #js [editor selected? nodeKey set-selected])]
    (useEffect
     (fn []
       (mergeRegister
        (.registerCommand
         editor CLICK_COMMAND
         (fn [event]
           (when (= (.-target event) (.-current ref))
             (when-not (.-shiftKey event) (clear-selection))
             (set-selected (not selected?))
             true)
           false)
         COMMAND_PRIORITY_LOW)
        (.registerCommand editor KEY_DELETE_COMMAND on-delete COMMAND_PRIORITY_LOW)
        (.registerCommand editor KEY_BACKSPACE_COMMAND on-delete COMMAND_PRIORITY_LOW)))
     #js [clear-selection editor selected? nodeKey on-delete set-selected])
    (r/as-element
     [:img.w-75 {:class (when selected? "focused")
                 :src src
                 :alt altText
                 :ref ref}])))

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
  (getAltText [this] (oget this "__altText"))
  (decorate [this ^LexicalEditor _editor]
            (r/create-element ImageComponent #js {:src (oget this "__src") :alt (oget this "__altText") :nodeKey (.getKey this)})))

;; Configure static methods on our new class, because it is not possible to do
;; this inline in the `defclass` macro.
(set! (.-getType ImageNode) (fn [] "image"))
(set! (.-clone ImageNode)
      (fn [^ImageNode node]
        (ImageNode. (oget node "__src") (oget node "__altText") (oget node "__key"))))

(defn create-image-node [src altText]
  (ImageNode. src altText nil))

(defn image-node? [^LexicalNode node]
  (when node
    (instance? ImageNode node)))
