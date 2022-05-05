(ns schnaq.interface.components.lexical.plugins.toolbar
  (:require ["@lexical/selection" :refer [$wrapLeafNodesInElements]]
            ["@lexical/list" :refer [$isListNode ListNode
                                     INSERT_ORDERED_LIST_COMMAND
                                     INSERT_UNORDERED_LIST_COMMAND
                                     REMOVE_LIST_COMMAND]]
            ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["@lexical/rich-text" :refer [$isHeadingNode $createQuoteNode]]
            ["@lexical/utils" :refer [$getNearestNodeOfType mergeRegister]]
            ["lexical" :refer [$getSelection $isRangeSelection
                               FORMAT_TEXT_COMMAND SELECTION_CHANGE_COMMAND
                               CAN_UNDO_COMMAND CAN_REDO_COMMAND
                               REDO_COMMAND UNDO_COMMAND]]
            ["react" :refer [useCallback useEffect useRef useState]]
            [schnaq.interface.components.icons :refer [icon]]))

(def low-priority 1)

(defn format-quote
  "Format a selection to be a quote block."
  [editor block-type]
  (when (not= block-type "quote")
    (.update editor
             #(let [selection ($getSelection)]
                (when ($isRangeSelection selection)
                  ($wrapLeafNodesInElements selection (fn [] ($createQuoteNode))))))))

(defn toolbar-plugin
  "Build a toolbar for the editor."
  []
  (let [[editor] (useLexicalComposerContext)
        [blockType setBlockType] (useState "paragraph")
        [bold? setIsBold] (useState false)
        [code? setIsCode] (useState false)
        [italic? setIsItalic] (useState false)
        [underline? setIsUnderline] (useState false)
        [strike-through? setIsStrikethrough] (useState false)
        [canUndo setCanUndo] (useState false)
        [canRedo setCanRedo] (useState false)
        updateToolbar
        (useCallback
         (fn []
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
                 (setIsCode (.hasFormat selection "code"))
                 (setIsItalic (.hasFormat selection "italic"))
                 (setIsUnderline (.hasFormat selection "underline"))
                 (setIsStrikethrough (.hasFormat selection "strikethrough"))))))
         #js [editor])]
    (useEffect
     #(mergeRegister
       (.registerUpdateListener editor
                                (fn [editor]
                                  (.read (.-editorState editor) (fn [] (updateToolbar)))))
       (.registerCommand editor
                         SELECTION_CHANGE_COMMAND
                         (fn [_payload _newEditor] (updateToolbar) false)
                         low-priority)
       (.registerCommand editor
                         CAN_UNDO_COMMAND
                         (fn [payload] (setCanUndo payload) false)
                         low-priority)
       (.registerCommand editor
                         CAN_REDO_COMMAND
                         (fn [payload] (setCanRedo payload) false)
                         low-priority))
     #js [editor updateToolbar])
    [:div.toolbar
     [:button.toolbar-item.spaced
      {:on-click #(.dispatchCommand editor FORMAT_TEXT_COMMAND "bold")
       :class (when bold? "active")}
      [icon :bold]]
     [:button.toolbar-item.spaced
      {:on-click #(.dispatchCommand editor FORMAT_TEXT_COMMAND "italic")
       :class (when italic? "active")}
      [icon :italic]]
     [:button.toolbar-item.spaced
      {:on-click #(.dispatchCommand editor FORMAT_TEXT_COMMAND "underline")
       :class (when underline? "active")}
      [icon :underline]]
     [:button.toolbar-item.spaced
      {:on-click #(.dispatchCommand editor FORMAT_TEXT_COMMAND "strikethrough")
       :class (when strike-through? "active")}
      [icon :strikethrough]]
     [:button.toolbar-item.spaced
      {:on-click #(.dispatchCommand editor FORMAT_TEXT_COMMAND "code")
       :class (when code? "active")}
      [icon :code]]
     [:button.toolbar-item.spaced
      {:on-click #(format-quote editor blockType)}
      [icon :quote-right]]
     [:button.toolbar-item.spaced
      (let [unordered-list? (= blockType "ul")]
        {:on-click #(if unordered-list?
                      (.dispatchCommand editor REMOVE_LIST_COMMAND)
                      (.dispatchCommand editor INSERT_UNORDERED_LIST_COMMAND))
         :class (when unordered-list? "active")})
      [icon :list]]
     [:button.toolbar-item.spaced
      (let [ordered-list? (= blockType "ol")]
        {:on-click #(if ordered-list?
                      (.dispatchCommand editor REMOVE_LIST_COMMAND)
                      (.dispatchCommand editor INSERT_ORDERED_LIST_COMMAND))
         :class (when ordered-list? "active")})
      [icon :list-ol]]
     [:button.toolbar-item.spaced
      {:on-click #(.dispatchCommand editor UNDO_COMMAND)
       :disabled (not canUndo)}
      [icon :undo]]
     [:button.toolbar-item.spaced
      {:on-click #(.dispatchCommand editor REDO_COMMAND)
       :disabled (not canRedo)}
      [icon :redo]]]))
