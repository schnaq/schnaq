(ns schnaq.interface.components.lexical.plugins.toolbar
  (:require ["@lexical/list" :refer [$isListNode INSERT_ORDERED_LIST_COMMAND
                                     INSERT_UNORDERED_LIST_COMMAND ListNode
                                     REMOVE_LIST_COMMAND]]
            ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["@lexical/rich-text" :refer [$createQuoteNode $isHeadingNode]]
            ["@lexical/selection" :refer [$wrapLeafNodesInElements]]
            ["@lexical/utils" :refer [$getNearestNodeOfType mergeRegister]]
            ["lexical" :refer [$getSelection $isRangeSelection
                               CAN_REDO_COMMAND CAN_UNDO_COMMAND CLEAR_EDITOR_COMMAND
                               CLEAR_HISTORY_COMMAND FORMAT_TEXT_COMMAND REDO_COMMAND SELECTION_CHANGE_COMMAND
                               UNDO_COMMAND]]
            ["react" :refer [useCallback useEffect useState]]
            [oops.core :refer [ocall]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.inputs :as inputs]
            [schnaq.interface.components.lexical.plugins.images :refer [INSERT_IMAGE_COMMAND]]
            [schnaq.interface.components.lexical.plugins.links :refer [INSERT_LINK_COMMAND]]
            [schnaq.interface.components.lexical.plugins.videos :refer [INSERT_VIDEO_COMMAND]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as tools]
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
       {:on-click #(.dispatchCommand editor INSERT_IMAGE_COMMAND #js {:src "https://cdn.pixabay.com/photo/2016/11/14/04/45/elephant-1822636_1280.jpg" :altText "Elephant in a forest"})}
       [icon :image-file]]]
     [tooltip/text
      "[Dev] Insert Video"
      [:button.toolbar-item.spaced.text-secondary
       {:on-click #(.dispatchCommand editor INSERT_VIDEO_COMMAND #js {:url "https://s3.schnaq.com/startpage/videos/above_the_fold.webm"})}
       [icon :video-file]]]]))

(defn- file-upload-button
  "Show a button and a modal to upload own images."
  [_input-label _input-component _icon-component _on-click-event]
  (let [tooltip-visible? (r/atom false)]
    (fn [input-label input-component icon-component on-click-event]
      [tooltip/text
       input-label
       [:span ;; Wrap into a span to make tippy nestable.
        [tooltip/html
         [:<>
          ;; Use here no form, because forms can't be nested.
          input-component
          [:div.d-flex.mt-2
           [:button.btn.btn-primary.me-auto
            {:type :button
             :on-click (fn []
                         (rf/dispatch on-click-event)
                         (reset! tooltip-visible? false))}
            (labels :editor.toolbar.file-upload/submit)]
           [:button.btn.btn-sm.btn-link.text-dark.ps-auto
            {:type :button
             :on-click #(reset! tooltip-visible? false)}
            (labels :editor.toolbar.file-upload/close)]]]
         [:button.toolbar-item.spaced
          {:on-click #(swap! tooltip-visible? not)
           :type :button}
          icon-component]
         {:visible @tooltip-visible?
          :appendTo js/document.body}
         [:trigger]]]])))

(rf/reg-event-fx
 :editor.upload/image
 (fn [{:keys [db]} [_ id editor file-storage]]
   (when (= :schnaq/by-share-hash file-storage)
     (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
           image (get-in db [:editors id :image])]
       {:fx [[:dispatch [:file/upload share-hash image :schnaq/media [:editor.upload.image/success editor] [:file.store/error]]]]}))))

(rf/reg-event-fx
 :editor.upload.image/success
 (fn [_ [_ editor {:keys [url]}]]
   {:fx [[:editor/dispatch-command! [editor INSERT_IMAGE_COMMAND #js {:src url}]]]}))

(rf/reg-event-fx
 :editor.upload/file
 (fn [{:keys [db]} [_ id editor file-storage]]
   (when (= :schnaq/by-share-hash file-storage)
     (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
           file (get-in db [:editors id :file])]
       {:fx [[:dispatch [:file/upload share-hash file :schnaq/media [:editor.upload.file/success editor] [:ajax.error/as-notification]]]]}))))

(rf/reg-event-fx
 :editor.upload.file/success
 (fn [_ [_ editor {:keys [url]}]]
   {:fx [[:editor/dispatch-command! [editor INSERT_LINK_COMMAND #js {:url url
                                                                     :text (tools/filename-from-url url)}]]]}))

;; -----------------------------------------------------------------------------

(defn ToolbarPlugin
  "Build a toolbar for the editor."
  [{:keys [file-storage debug? id]}]
  (let [editor-content @(rf/subscribe [:editor/content id])
        [editor] (useLexicalComposerContext)
        [active-editor active-editor!] (useState editor)
        [block-type block-type!] (useState "paragraph")
        [bold? bold!] (useState false)
        [code? code!] (useState false)
        [italic? italic!] (useState false)
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
                     element-dom (ocall editor "getElementByKey" element-key)]
                 (when element-dom
                   (if ($isListNode element)
                     (let [parentList ($getNearestNodeOfType anchorNode ListNode)
                           block-type (if parentList (.getTag parentList) (.getTag element))]
                       (block-type! block-type))
                     (let [block-type (if ($isHeadingNode element) (ocall element "getTag") (ocall element "getType"))]
                       (block-type! block-type))))
                 ;; Update text format
                 (bold! (.hasFormat selection "bold"))
                 (code! (.hasFormat selection "code"))
                 (italic! (.hasFormat selection "italic"))
                 (strike-through! (.hasFormat selection "strikethrough"))))))
         #js [active-editor])]
    (useEffect
     #(mergeRegister
       (ocall active-editor "registerUpdateListener"
              (fn [editor]
                (.read (.-editorState editor) (fn [] (update-toolbar)))))
       (ocall editor "registerCommand" ;; here it is the initial editor to swap it when changed.
              SELECTION_CHANGE_COMMAND
              (fn [_payload new-editor] (update-toolbar) (active-editor! new-editor) false)
              low-priority)
       (ocall active-editor "registerCommand"
              CAN_UNDO_COMMAND
              (fn [payload] (can-undo! payload) false)
              low-priority)
       (ocall active-editor "registerCommand"
              CAN_REDO_COMMAND
              (fn [payload] (can-redo! payload) false)
              low-priority))
     #js [editor update-toolbar])
    [:div.toolbar
     [tooltip/text
      (labels :editor.toolbar/bold)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand active-editor FORMAT_TEXT_COMMAND "bold")
        :type :button
        :class (when bold? "active")}
       [icon :bold]]]
     [tooltip/text
      (labels :editor.toolbar/italic)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand active-editor FORMAT_TEXT_COMMAND "italic")
        :type :button
        :class (when italic? "active")}
       [icon :italic]]]
     [tooltip/text
      (labels :editor.toolbar/strike-through)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand active-editor FORMAT_TEXT_COMMAND "strikethrough")
        :type :button
        :class (when strike-through? "active")}
       [icon :strike-through]]]
     [tooltip/text
      (labels :editor.toolbar/code)
      [:button.toolbar-item.spaced
       {:on-click #(.dispatchCommand active-editor FORMAT_TEXT_COMMAND "code")
        :type :button
        :class (when code? "active")}
       [icon :code]]]
     [tooltip/text
      (labels :editor.toolbar/quote)
      [:button.toolbar-item.spaced
       {:on-click #(format-quote active-editor block-type)
        :type :button}
       [icon :quote-right]]]
     (when file-storage
       [:<>
        [file-upload-button
         (labels :editor.toolbar/image-upload)
         [inputs/image [:span.fs-5 (labels :editor.toolbar/image-upload)] "editor-upload-image" [:editors id :image]
          {:required true
           :accept (conj shared-config/allowed-mime-types-images "image/gif")
           :form "form-upload-an-image"}]
         [icon :image-file]
         [:editor.upload/image id active-editor file-storage]]
        [file-upload-button
         (labels :editor.toolbar/file-upload)
         [inputs/file [:span.fs-5 (labels :editor.toolbar/file-upload)] "editor-upload-file" [:editors id :file] {:required true}]
         [icon :file-alt]
         [:editor.upload/file id active-editor file-storage]]])

     [tooltip/text
      (labels :editor.toolbar/list-ul)
      [:button.toolbar-item.spaced
       (let [unordered-list? (= block-type "ul")]
         {:on-click #(if unordered-list?
                       (.dispatchCommand active-editor REMOVE_LIST_COMMAND)
                       (.dispatchCommand active-editor INSERT_UNORDERED_LIST_COMMAND))
          :type :button
          :class (when unordered-list? "active")})
       [icon :list]]]
     [tooltip/text
      (labels :editor.toolbar/list-ol)
      [:button.toolbar-item.spaced
       (let [ordered-list? (= block-type "ol")]
         {:on-click #(if ordered-list?
                       (.dispatchCommand active-editor REMOVE_LIST_COMMAND)
                       (.dispatchCommand active-editor INSERT_ORDERED_LIST_COMMAND))
          :type :button
          :class (when ordered-list? "active")})
       [icon :list-ol]]]
     [:span.d-none.d-md-block
      [tooltip/text
       (labels :editor.toolbar/clear)
       [:button.toolbar-item.spaced
        {:on-click (fn []
                     (.dispatchCommand active-editor CLEAR_EDITOR_COMMAND)
                     (.dispatchCommand active-editor CLEAR_HISTORY_COMMAND))
         :type :button
         :disabled (empty? editor-content)}
        [icon :trash]]]]
     [:span.d-none.d-md-block
      [tooltip/text
       (labels :editor.toolbar/undo)
       [:button.toolbar-item.spaced
        {:on-click #(.dispatchCommand active-editor UNDO_COMMAND)
         :type :button
         :disabled (not can-undo?)}
        [icon :undo]]]]
     [:span.d-none.d-md-block
      [tooltip/text
       (labels :editor.toolbar/redo)
       [:button.toolbar-item.spaced
        {:on-click #(.dispatchCommand active-editor REDO_COMMAND)
         :type :button
         :disabled (not can-redo?)}
        [icon :redo]]]]
     [development-buttons active-editor debug?]]))
