(ns schnaq.interface.components.lexical.plugins.toolbar
  (:require ["@lexical/link" :refer [$isLinkNode TOGGLE_LINK_COMMAND]]
            ["@lexical/list" :refer [$isListNode INSERT_ORDERED_LIST_COMMAND
                                     INSERT_UNORDERED_LIST_COMMAND ListNode
                                     REMOVE_LIST_COMMAND]]
            ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["@lexical/rich-text" :refer [$createQuoteNode $isHeadingNode]]
            ["@lexical/selection" :refer [$isAtNodeEnd $wrapLeafNodesInElements]]
            ["@lexical/utils" :refer [$getNearestNodeOfType mergeRegister]]
            ["lexical" :refer [$getSelection $isRangeSelection CAN_REDO_COMMAND
                               CAN_UNDO_COMMAND FORMAT_TEXT_COMMAND REDO_COMMAND
                               SELECTION_CHANGE_COMMAND UNDO_COMMAND]]
            ["react" :refer [useCallback useEffect useState]]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.lexical.plugins.images :refer [INSERT_IMAGE_COMMAND]]
            [schnaq.interface.components.lexical.plugins.video :refer [INSERT_VIDEO_COMMAND]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.tooltip :as tooltip]))

(def low-priority 1)

(defn get-selected-node
  "Return the selected node by the user's selection."
  [^js/LexicalNode selection]
  (let [anchor (.-anchor selection)
        focus (.-focus selection)
        anchor-node (.getNode anchor)
        focus-node (.getNode focus)
        backward? (.isBackward selection)]
    (if (= anchor-node focus-node)
      anchor-node
      (if backward?
        (if ($isAtNodeEnd focus) anchor-node focus-node)
        (if ($isAtNodeEnd anchor) focus-node anchor-node)))))

(defn format-quote
  "Format a selection to be a quote block."
  [editor block-type]
  (when (not= block-type "quote")
    (.update editor
             #(let [selection ($getSelection)]
                (when ($isRangeSelection selection)
                  ($wrapLeafNodesInElements selection (fn [] ($createQuoteNode))))))))

(defn development-buttons
  "Some buttons only for development, e.g. to fast insert an image."
  [^LexicalEditor editor]
  (when-not shared-config/production?
    [:<>
     [tooltip/text
      "[Dev] Insert Image"
      [:button.toolbar-item.spaced.text-secondary
       {:on-click #(.dispatchCommand editor INSERT_IMAGE_COMMAND #js {:src "https://cdn.pixabay.com/photo/2016/11/14/04/45/elephant-1822636_1280.jpg" :altText "foo"})}
       [icon :image-file]]]
     [tooltip/text
      "[Dev] Insert Video"
      [:button.toolbar-item.spaced.text-secondary
       {:on-click #(.dispatchCommand editor INSERT_VIDEO_COMMAND #js {:url "https://s3.schnaq.com/startpage/videos/above_the_fold.webm"})}
       [icon :video-file]]]]))

(defn ToolbarPlugin
  "Build a toolbar for the editor."
  []
  (let [[editor] (useLexicalComposerContext)
        [block-type block-type!] (useState "paragraph")
        [bold? bold!] (useState false)
        [code? code!] (useState false)
        [italic? italic!] (useState false)
        [underline? underline!] (useState false)
        [strike-through? strike-through!] (useState false)
        [link? link!] (useState false)
        [can-undo? can-undo!] (useState false)
        [can-redo? can-redo!] (useState false)
        _insert-link (useCallback
                      #(if (not link?)
                         (.dispatchCommand editor TOGGLE_LINK_COMMAND "https://")
                         (.dispatchCommand editor TOGGLE_LINK_COMMAND nil))
                      #js [editor link?])
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
                       (block-type! block-type))
                     (let [block-type (if ($isHeadingNode element) (.getTag element) (.getType element))]
                       (block-type! block-type))))
                 ;; Update text format
                 (bold! (.hasFormat selection "bold"))
                 (code! (.hasFormat selection "code"))
                 (italic! (.hasFormat selection "italic"))
                 (underline! (.hasFormat selection "underline"))
                 (strike-through! (.hasFormat selection "strikethrough"))

                 ;; Update links
                 (let [node (get-selected-node selection)
                       parent (.getParent node)]
                   (link! (or ($isLinkNode parent) ($isLinkNode node))))))))
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
                         (fn [payload] (can-undo! payload) false)
                         low-priority)
       (.registerCommand editor
                         CAN_REDO_COMMAND
                         (fn [payload] (can-redo! payload) false)
                         low-priority))
     #js [editor updateToolbar])
    [:div.toolbar
     [tooltip/text
      (labels :editor.toolbar/bold)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand editor FORMAT_TEXT_COMMAND "bold")
        :class (when bold? "active")}
       [icon :bold]]]
     [tooltip/text
      (labels :editor.toolbar/italic)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand editor FORMAT_TEXT_COMMAND "italic")
        :class (when italic? "active")}
       [icon :italic]]]
     [tooltip/text
      (labels :editor.toolbar/underline)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand editor FORMAT_TEXT_COMMAND "underline")
        :class (when underline? "active")}
       [icon :underline]]]
     [tooltip/text
      (labels :editor.toolbar/strike-through)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand editor FORMAT_TEXT_COMMAND "strikethrough")
        :class (when strike-through? "active")}
       [icon :strike-through]]]
     [tooltip/text
      (labels :editor.toolbar/code)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand editor FORMAT_TEXT_COMMAND "code")
        :class (when code? "active")}
       [icon :code]]]
     [tooltip/text
      (labels :editor.toolbar/quote)
      [:button.toolbar-item.spaced
       {:on-click #(format-quote editor block-type)}
       [icon :quote-right]]]
     [tooltip/text
      (labels :editor.toolbar/image-upload)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand editor INSERT_IMAGE_COMMAND #js {:src "https://cdn.pixabay.com/photo/2016/11/14/04/45/elephant-1822636_1280.jpg" :altText "foo"})}
       [icon :image-file]]]
     [tooltip/text
      (labels :editor.toolbar/video-upload)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand editor INSERT_VIDEO_COMMAND #js {:url "https://s3.schnaq.com/startpage/videos/above_the_fold.webm"})}
       [icon :video-file]]]
     [tooltip/text
      (labels :editor.toolbar/list-ul)
      [:button.toolbar-item.spaced
       (let [unordered-list? (= block-type "ul")]
         {:on-click #(if unordered-list?
                       (.dispatchCommand editor REMOVE_LIST_COMMAND)
                       (.dispatchCommand editor INSERT_UNORDERED_LIST_COMMAND))
          :class (when unordered-list? "active")})
       [icon :list]]]
     [tooltip/text
      (labels :editor.toolbar/list-ol)
      [:button.toolbar-item.spaced
       (let [ordered-list? (= block-type "ol")]
         {:on-click #(if ordered-list?
                       (.dispatchCommand editor REMOVE_LIST_COMMAND)
                       (.dispatchCommand editor INSERT_ORDERED_LIST_COMMAND))
          :class (when ordered-list? "active")})
       [icon :list-ol]]]
     [tooltip/text
      (labels :editor.toolbar/undo)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand editor UNDO_COMMAND)
        :disabled (not can-undo?)}
       [icon :undo]]]
     [tooltip/text
      (labels :editor.toolbar/redo)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand editor REDO_COMMAND)
        :disabled (not can-redo?)}
       [icon :redo]]]
     [development-buttons editor]]))
