(ns schnaq.interface.components.lexical.nodes.video
  (:require ["lexical" :refer [DecoratorNode EditorConfig LexicalEditor NodeKey]]
            [oops.core :refer [oget oset!]]
            [reagent.core :as r]
            [shadow.cljs.modern :refer [defclass]]))

(defn VideoPlayer [url]
  (r/as-element
   [:video.w-75 {:src url
                 :controls true}]))

(defclass VideoNode
  (field ^string __url)
  (field ^NodeKey __key)
  (extends DecoratorNode)
  (constructor [_this url ?key]
               (super ?key)
               (set! __url url))
  Object
  (createDOM [this ^EditorConfig config]
             (let [div (.createElement js/document "div")]
               (oset! div ["style" "display"] "contents")
               div))
  (updateDOM [_this] false)
  (setURL [this url]
          (let [writable (.getWritable this)]
            (oset! writable "__url" url)))
  (decorate [this ^LexicalEditor editor]
            (VideoPlayer (oget this "__url"))))
(set! (.-getType VideoNode) (fn [] "video"))
(set! (.-clone VideoNode)
      (fn [^VideoNode node]
        (VideoNode. (oget node "__url") (oget node "__key"))))

(defn $createVideoNode [url]
  (VideoNode. url nil))

(defn $isVideoNode [^LexicalNode node]
  (when node
    (instance? node VideoNode)))
