(ns schnaq.interface.components.plugins.toolbar
  (:require ["@lexical/list" :refer [$isListNode ListNode]]
            ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["@lexical/rich-text" :refer [$isHeadingNode]]
            ["@lexical/utils" :refer [$getNearestNodeOfType mergeRegister]]
            ["lexical" :refer [$getSelection $isRangeSelection
                               FORMAT_TEXT_COMMAND SELECTION_CHANGE_COMMAND]]
            ["react" :refer [useCallback useEffect useRef useState]]
            [schnaq.interface.components.icons :refer [icon]]))

(def LowPriority 1)

(def blockTypeToBlockName
  #js
   {:code "Code Block",
    :h1 "Large Heading",
    :h2 "Small Heading",
    :h3 "Heading",
    :h4 "Heading",
    :h5 "Heading",
    :ol "Numbered List",
    :paragraph "Normal",
    :quote "Quote",
    :ul "Bulleted List"})

(defn toolbar-plugin []
  (let [[editor] (useLexicalComposerContext)
        toolbarRef (useRef nil)
        [blockType setBlockType] (useState "paragraph")
        [isBold setIsBold] (useState false)
        [isItalic setIsItalic] (useState false)
        [isUnderline setIsUnderline] (useState false)
        [IsStrikethrough setIsStrikethrough] (useState false)
        updateToolbar (useCallback (fn []
                                     (let [selection ($getSelection)]
                                       (when ($isRangeSelection selection)
                                         (let [anchorNode (.getNode (.-anchor selection))
                                               element (if (= "root" anchorNode) anchorNode (.getTopLevelElementOrThrow anchorNode))
                                               element-key (.getKey element)
                                               element-dom (.getElementByKey editor element-key)]
                                           (when element-dom
                                             (if ($isListNode element)
                                               (let [parentList ($getNearestNodeOfType anchorNode ListNode)
                                                     block-type (if parentList (.getTag parentList) (.getTag element))]
                                                 (setBlockType block-type))
                                               (let [block-type (if ($isHeadingNode element) (.getTag element) (.getType element))]
                                                 (setBlockType block-type))))
                                           (setIsBold (.hasFormat selection "bold"))
                                           (setIsItalic (.hasFormat selection "italic"))
                                           (setIsUnderline (.hasFormat selection "underline"))
                                           (setIsStrikethrough (.hasFormat selection "strikethrough"))
                                           (.log js/console "huhu")))))
                                   #js [editor])]
    (useEffect
     #(mergeRegister
       (.registerUpdateListener editor
                                (fn [editor]
                                  (.read (.-editorState editor) (fn [] (updateToolbar)))))
       (.registerCommand editor
                         SELECTION_CHANGE_COMMAND
                         (fn [_payload _newEditor] (updateToolbar) false)
                         LowPriority))
     #js [editor updateToolbar])
    [:div.toolbar
     [:button.toolbar-item.spaced
      {:on-click #(.dispatchCommand editor FORMAT_TEXT_COMMAND "bold")
       :class (when isBold "active")}
      [icon :bold]]
     [:button.toolbar-item.spaced
      {:on-click #(.dispatchCommand editor FORMAT_TEXT_COMMAND "italic")
       :class (when isItalic "active")}
      [icon :italic]]
     [:button.toolbar-item.spaced
      {:on-click #(.dispatchCommand editor FORMAT_TEXT_COMMAND "underline")
       :class (when isUnderline "active")}
      [icon :underline]]
     [:button.toolbar-item.spaced
      {:on-click #(.dispatchCommand editor FORMAT_TEXT_COMMAND "strikethrough")
       :class (when IsStrikethrough "active")}
      [icon :strikethrough]]]))
