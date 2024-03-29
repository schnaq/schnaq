(ns schnaq.interface.components.lexical.nodes.video
  (:require ["lexical" :refer [DecoratorNode EditorConfig LexicalEditor]]
            [oops.core :refer [oget oset!]]
            [reagent.core :as r]
            [shadow.cljs.modern :refer [defclass]]))

(declare $create-video-node)

(defn VideoPlayer
  "Create a video element."
  [url]
  (r/as-element
   [:video.w-75 {:src url
                 :controls true}]))

(defclass VideoNode
  (field ^string __url)
  (extends DecoratorNode)
  (constructor [this url ?key]
               (super ?key)
               (oset! this :!__url url))
  Object
  (createDOM [this ^EditorConfig config]
             (let [div (.createElement js/document "div")]
               (oset! div ["style" "display"] "contents")
               div))
  (updateDOM [_this] false)
  (getURL [this]
          (oget this "__url"))
  (setURL [this url]
          (let [writable (.getWritable this)]
            (oset! writable "__url" url)))
  (exportJSON [this] {:url (oget this :__url)
                      :type "video"
                      :version 1})
  (decorate [this ^LexicalEditor editor]
            (VideoPlayer (oget this "__url"))))
(oset! VideoNode "getType" (fn [] "video"))
(oset! VideoNode "clone"
       (fn [^VideoNode node]
         (VideoNode. (oget node "__url") (oget node "__key"))))
(oset! VideoNode "importJSON"
       (fn [^SerializedVideoNode node]
         (let [url (oget node :url)]
           ($create-video-node url))))

(defn $create-video-node [url]
  (VideoNode. url nil))

(defn $video-node? [^LexicalNode node]
  (when node
    (instance? VideoNode node)))
