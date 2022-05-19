(ns schnaq.interface.components.lexical.plugins.toolbar
  (:require ["@lexical/list" :refer [$isListNode INSERT_ORDERED_LIST_COMMAND
                                     INSERT_UNORDERED_LIST_COMMAND ListNode
                                     REMOVE_LIST_COMMAND]]
            ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["@lexical/rich-text" :refer [$createQuoteNode $isHeadingNode]]
            ["@lexical/selection" :refer [$wrapLeafNodesInElements]]
            ["@lexical/utils" :refer [$getNearestNodeOfType mergeRegister]]
            ["lexical" :refer [$getSelection $isRangeSelection CAN_REDO_COMMAND
                               CAN_UNDO_COMMAND FORMAT_TEXT_COMMAND REDO_COMMAND
                               SELECTION_CHANGE_COMMAND UNDO_COMMAND]]
            ["react" :refer [useCallback useEffect useState]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.inputs :as inputs]
            [schnaq.interface.components.lexical.plugins.images :refer [INSERT_IMAGE_COMMAND]]
            [schnaq.interface.components.lexical.plugins.video :refer [INSERT_VIDEO_COMMAND]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.tooltip :as tooltip]))

(def low-priority 1)

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
  [^LexicalEditor editor debug?]
  (when debug?
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

(defn image-upload-button
  "Show a button and a modal to upload own images."
  [^js/LexicalEditor _editor _file-storage]
  (let [tooltip-visible? (r/atom false)]
    (fn [editor file-storage]
      [tooltip/html
       [:<>
        [:form {:on-submit
                (fn [e]
                  (.preventDefault e)
                  (rf/dispatch [:editor.upload/image editor file-storage])
                  (reset! tooltip-visible? false))}
         [inputs/image [:span.fs-5 "Image Upload"] "editor-upload-image" [:editor :temporary :image] {:required true}]
         [:div.d-flex.mt-2
          [:input.btn.btn-primary.me-auto
           {:type :submit
            :value "Bild einfügen"}]
          [:button.btn.btn-sm.btn-link.text-dark.ps-auto
           {:on-click #(reset! tooltip-visible? false)}
           "Schließen"]]]]
       [:button.toolbar-item.spaced
        {:on-click #(swap! tooltip-visible? not)}
        [icon :image-file]]
       {:visible @tooltip-visible?
        :appendTo js/document.body}
       [:trigger]])))

(rf/reg-event-fx
 :editor.upload/image
 (fn [{:keys [db]} [_ editor file-storage]]
   (when (= :schnaq/by-share-hash file-storage)
     (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
           image (get-in db [:editor :temporary :image])]
       {:fx [[:dispatch [:image/upload share-hash image :schnaq/media [:editor.upload.image/success editor]]]]}))))

(rf/reg-event-fx
 :editor.upload.image/success
 (fn [_ [_ editor {:keys [url]}]]
   {:fx [[:editor/dispatch-command! [editor INSERT_IMAGE_COMMAND #js {:src url}]]]}))

(rf/reg-fx
 :editor/dispatch-command!
 (fn [[^LexicalEditor editor command payload]]
   (.dispatchCommand editor command payload)))

(defn ToolbarPlugin
  "Build a toolbar for the editor."
  [{:keys [file-storage debug?]}]
  (let [[editor] (useLexicalComposerContext)
        [active-editor active-editor!] (useState editor)
        [block-type block-type!] (useState "paragraph")
        [bold? bold!] (useState false)
        [code? code!] (useState false)
        [italic? italic!] (useState false)
        [underline? underline!] (useState false)
        [strike-through? strike-through!] (useState false)
        [can-undo? can-undo!] (useState false)
        [can-redo? can-redo!] (useState false)
        update-toolbar
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
                 (strike-through! (.hasFormat selection "strikethrough"))))))
         #js [active-editor])]
    (useEffect
     #(mergeRegister
       (.registerUpdateListener active-editor
                                (fn [editor]
                                  (.read (.-editorState editor) (fn [] (update-toolbar)))))
       (.registerCommand editor ;; here it is the initial editor to swap it when changed.
                         SELECTION_CHANGE_COMMAND
                         (fn [_payload new-editor] (update-toolbar) (active-editor! new-editor) false)
                         low-priority)
       (.registerCommand active-editor
                         CAN_UNDO_COMMAND
                         (fn [payload] (can-undo! payload) false)
                         low-priority)
       (.registerCommand active-editor
                         CAN_REDO_COMMAND
                         (fn [payload] (can-redo! payload) false)
                         low-priority))
     #js [editor update-toolbar])
    [:div.toolbar
     [tooltip/text
      (labels :editor.toolbar/bold)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand active-editor FORMAT_TEXT_COMMAND "bold")
        :class (when bold? "active")}
       [icon :bold]]]
     [tooltip/text
      (labels :editor.toolbar/italic)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand active-editor FORMAT_TEXT_COMMAND "italic")
        :class (when italic? "active")}
       [icon :italic]]]
     [tooltip/text
      (labels :editor.toolbar/underline)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand active-editor FORMAT_TEXT_COMMAND "underline")
        :class (when underline? "active")}
       [icon :underline]]]
     [tooltip/text
      (labels :editor.toolbar/strike-through)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand active-editor FORMAT_TEXT_COMMAND "strikethrough")
        :class (when strike-through? "active")}
       [icon :strike-through]]]
     [tooltip/text
      (labels :editor.toolbar/code)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand active-editor FORMAT_TEXT_COMMAND "code")
        :class (when code? "active")}
       [icon :code]]]
     [tooltip/text
      (labels :editor.toolbar/quote)
      [:button.toolbar-item.spaced
       {:on-click #(format-quote active-editor block-type)}
       [icon :quote-right]]]
     [image-upload-button active-editor file-storage]
     [tooltip/text
      (labels :editor.toolbar/video-upload)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand active-editor INSERT_VIDEO_COMMAND #js {:url "https://s3.schnaq.com/startpage/videos/above_the_fold.webm"})}
       [icon :video-file]]]
     [tooltip/text
      (labels :editor.toolbar/list-ul)
      [:button.toolbar-item.spaced
       (let [unordered-list? (= block-type "ul")]
         {:on-click #(if unordered-list?
                       (.dispatchCommand active-editor REMOVE_LIST_COMMAND)
                       (.dispatchCommand active-editor INSERT_UNORDERED_LIST_COMMAND))
          :class (when unordered-list? "active")})
       [icon :list]]]
     [tooltip/text
      (labels :editor.toolbar/list-ol)
      [:button.toolbar-item.spaced
       (let [ordered-list? (= block-type "ol")]
         {:on-click #(if ordered-list?
                       (.dispatchCommand active-editor REMOVE_LIST_COMMAND)
                       (.dispatchCommand active-editor INSERT_ORDERED_LIST_COMMAND))
          :class (when ordered-list? "active")})
       [icon :list-ol]]]
     [tooltip/text
      (labels :editor.toolbar/undo)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand active-editor UNDO_COMMAND)
        :disabled (not can-undo?)}
       [icon :undo]]]
     [tooltip/text
      (labels :editor.toolbar/redo)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand active-editor REDO_COMMAND)
        :disabled (not can-redo?)}
       [icon :redo]]]
     [development-buttons active-editor debug?]]))
