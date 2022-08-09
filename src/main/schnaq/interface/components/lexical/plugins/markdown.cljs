(ns schnaq.interface.components.lexical.plugins.markdown
  (:require ["@lexical/markdown" :refer [TRANSFORMERS]]
            ["@lexical/react/LexicalMarkdownShortcutPlugin" :refer [MarkdownShortcutPlugin]]
            [goog.string :refer [format]]
            [oops.core :refer [ocall]]
            [schnaq.interface.components.lexical.nodes.image :refer [$create-image-node $image-node?]]
            [schnaq.interface.components.lexical.nodes.video :refer [$create-video-node $video-node?]]
            [schnaq.interface.components.lexical.plugins.excalidraw :refer [excalidraw-transformer]]))

(def ^:private markdown-image-import-regex
  #"!\[[^\]]*\]\((.*?)(?=\"|\))(\".*\")?\)")

(def ^:private image-transformer
  "Export / import image nodes."
  #js {:export (fn [^ImageNode node, _export-children, _export-format]
                 (when ($image-node? node)
                   (format "![%s](%s)" (or (.getAltText node) "") (or (.getSrc node) ""))))
       :importRegExp markdown-image-import-regex
       :regExp markdown-image-import-regex
       :replace (fn [text-node match]
                  (let [[_ src altText] match
                        image-node ($create-image-node src altText)]
                    (ocall text-node "replace" image-node)))
       :trigger ")"
       :type "text-match"})

(def ^:private video-transformer
  "Export / Import video nodes."
  #js {:export (fn [^VideoNode node, _export-children, _export-format]
                 (when ($video-node? node)
                   (format "![Video](%s)" (.getURL node))))
       :importRegExp markdown-image-import-regex
       :regExp markdown-image-import-regex
       :replace (fn [text-node match]
                  (let [[_ src] match
                        video-node ($create-video-node src)]
                    (ocall text-node "replace" video-node)))
       :trigger ")"
       :type "text-match"})

;; -----------------------------------------------------------------------------

(def schnaq-transformers
  (.concat #js [image-transformer video-transformer excalidraw-transformer] TRANSFORMERS))

(defn markdown-shortcut-plugin
  "Plugin to enable markdown support"
  []
  [:> MarkdownShortcutPlugin #js {:transformers schnaq-transformers}])
