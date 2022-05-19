(ns schnaq.interface.components.lexical.plugins.markdown
  (:require ["@lexical/markdown" :refer [TRANSFORMERS]]
            ["@lexical/react/LexicalMarkdownShortcutPlugin" :as MarkdownShortcut]
            [goog.string :refer [format]]
            [schnaq.interface.components.lexical.nodes.image :refer [$create-image-node
                                                                     $image-node?]]
            [schnaq.interface.components.lexical.nodes.video :refer [create-video-node
                                                                     video-node?]]))

(def ^:private markdown-image-import-regex
  #"!\[[^\]]*\]\((?<filename>.*?)(?=\"|\))(?<optionalpart>\".*\")?\)")

(def ^:private image-transformer
  "Export / import image nodes."
  #js {:export (fn [^ImageNode node, _export-children, _export-format]
                 (when ($image-node? node)
                   (format "![%s](%s)" (or (.getAltText node) "") (.getSrc node))))
       :importRegExp markdown-image-import-regex
       :regExp markdown-image-import-regex
       :replace (fn [text-node match]
                  (let [[_ altText src] match
                        image-node ($create-image-node src altText)]
                    (.replace text-node image-node)))
       :trigger ")"
       :type "text-match"})

(def ^:private video-transformer
  "Export / Import video nodes."
  #js {:export (fn [^ImageNode node, _export-children, _export-format]
                 (when (video-node? node)
                   (format "![Video](%s)" (.getURL node))))
       :importRegExp markdown-image-import-regex
       :regExp markdown-image-import-regex
       :replace (fn [text-node match]
                  (let [[_ src] match
                        video-node (create-video-node src)]
                    (.replace text-node video-node)))
       :trigger ")"
       :type "text-match"})

;; -----------------------------------------------------------------------------

(def schnaq-transformers
  (.concat #js [image-transformer video-transformer] TRANSFORMERS))

(defn markdown-shortcut-plugin
  "Plugin to enable markdown support"
  []
  [:> MarkdownShortcut #js {:transformers schnaq-transformers}])
