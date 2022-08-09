(ns schnaq.interface.components.lexical.utils
  (:require ["lexical" :refer [$getRoot $getSelection $isRangeSelection
                               $isRootNode]]))

(defn $insert-node-wrapped-in-paragraphs
  "When inserting a node, you sometimes want to wrap it in paragraphs for better
  usability (e.g. adding a drawing / image and being able to continue writing in
  the next paragraph.)"
  [node]
  (let [selection (or ($getSelection) (.selectEnd ($getRoot)))]
    (when ($isRangeSelection selection)
      (when ($isRootNode (.getNode (.-anchor selection)))
        (.insertParagraph selection))
      (.insertNodes selection #js [node])
      (.insertParagraph selection))
    true))
