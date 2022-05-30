(ns schnaq.interface.components.lexical.plugins.links
  "This LinksPlugin adds functionality to directly insert a link node."
  (:require ["@lexical/link" :refer [LinkNode $createLinkNode]]
            ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["lexical" :refer [$getSelection $isRootNode $getRoot $isRangeSelection $createParagraphNode
                               COMMAND_PRIORITY_EDITOR createCommand
                               LexicalCommand $createTextNode]]
            ["react" :refer [useEffect]]
            [taoensso.timbre :as log]))

(def INSERT_LINK_COMMAND (createCommand))

(defn LinksPlugin
  "Adds a link with the command `INSERT_LINK_COMMAND`.
  Note: The `LinkPlugin` of lexical does not support inserting links directly."
  []
  (let [[editor] (useLexicalComposerContext)]
    (useEffect
     #(if-not (.hasNodes editor #js [LinkNode])
        (log/error "LinksPlugin: LinkNode not registered on editor")
        (.registerCommand
         editor
         INSERT_LINK_COMMAND
         (fn [^LexicalCommand payload]
           (let [url (.-url payload)
                 text (.-text payload)
                 text-node ($createTextNode text)
                 link-node (.append ($createLinkNode url) text-node)
                 paragraph-node (.append ($createParagraphNode) link-node)
                 selection (or ($getSelection) (.selectEnd ($getRoot)))]
             (when ($isRangeSelection selection)
               (when ($isRootNode (.getNode (.-anchor selection)))
                 (.insertParagraph selection))
               (.insertNodes selection #js [paragraph-node]))
             true))
         COMMAND_PRIORITY_EDITOR))
     #js [editor]))
  nil)
