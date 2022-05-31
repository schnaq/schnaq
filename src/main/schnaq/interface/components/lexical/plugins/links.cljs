(ns schnaq.interface.components.lexical.plugins.links
  "This LinksPlugin adds functionality to directly insert a link node."
  (:require ["@lexical/link" :refer [$createLinkNode LinkNode]]
            ["lexical" :refer [$createParagraphNode $createTextNode $getRoot
                               $getSelection $isRangeSelection $isRootNode
                               COMMAND_PRIORITY_EDITOR createCommand LexicalCommand]]
            [oops.core :refer [ocall oget]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]))

(def INSERT_LINK_COMMAND (createCommand))

(rf/reg-fx
 :editor.plugins.register/links
 (fn [^LexicalEditor editor]
   (if-not (ocall editor "hasNodes" #js [LinkNode])
     (log/error "LinksPlugin: LinkNode not registered on editor")
     (ocall editor "registerCommand" INSERT_LINK_COMMAND
            (fn [^LexicalCommand payload]
              (let [url (oget payload :url)
                    text (oget payload :text)
                    text-node ($createTextNode text)
                    link-node (.append ($createLinkNode url) text-node)
                    paragraph-node (.append ($createParagraphNode) link-node)
                    selection (or ($getSelection) (.selectEnd ($getRoot)))]
                (when ($isRangeSelection selection)
                  (when ($isRootNode (.getNode (.-anchor selection)))
                    (.insertParagraph selection))
                  (.insertNodes selection #js [paragraph-node]))
                true))
            COMMAND_PRIORITY_EDITOR))))
