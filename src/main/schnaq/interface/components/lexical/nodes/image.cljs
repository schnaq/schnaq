(ns schnaq.interface.components.lexical.nodes.image
  (:require ["lexical" :refer [DecoratorNode EditorConfig LexicalEditor]]
            [oops.core :refer [oget oset!]]
            [reagent.core :as r]
            [shadow.cljs.modern :refer [defclass]]))

(defn ImageComponent [src altText]
  (r/create-element "img"
                    #js{:className "w-100"
                        :src src
                        :alt altText}))

;; -----------------------------------------------------------------------------
;; Extend the DecoratorNode to create an image node.

(defclass ImageNode
  (field ^string __src)
  (field ^string __altText)
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
  (decorate [this ^LexicalEditor editor]
            (ImageComponent (oget this "__src") (oget this "__altText"))))
;; Configure static methods on our new class, because it is not possible to do
;; this inline in the `defclass` macro.
(set! (.-getType ImageNode) (fn [] "image"))
(set! (.-clone ImageNode)
      (fn [^ImageNode node]
        (ImageNode. (.-src node) (.-altText node) nil)))

;; -----------------------------------------------------------------------------

(defn $createImageNode [src altText]
  (ImageNode. src altText nil))

(defn $isImageNode [^LexicalNode node]
  (when node
    (instance? node ImageNode)))
