(ns schnaq.interface.components.lexical.plugins.toolbar
  (:require ["@lexical/list" :refer [$isListNode INSERT_ORDERED_LIST_COMMAND
                                     INSERT_UNORDERED_LIST_COMMAND ListNode
                                     REMOVE_LIST_COMMAND]]
            ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["@lexical/rich-text" :refer [$createQuoteNode $isHeadingNode]]
            ["@lexical/selection" :refer [$setBlocksType]]
            ["@lexical/utils" :refer [$getNearestNodeOfType mergeRegister]]
            ["lexical" :refer [$getSelection $isRangeSelection
                               CAN_REDO_COMMAND CAN_UNDO_COMMAND FORMAT_TEXT_COMMAND
                               REDO_COMMAND SELECTION_CHANGE_COMMAND UNDO_COMMAND]]
            ["react" :refer [useCallback useEffect useState]]
            [goog.string :as gstring]
            [oops.core :refer [ocall oget]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.inputs :as inputs]
            [schnaq.interface.components.lexical.plugins.excalidraw :refer [INSERT_EXCALIDRAW_COMMAND]]
            [schnaq.interface.components.lexical.plugins.images :refer [INSERT_IMAGE_COMMAND]]
            [schnaq.interface.components.lexical.plugins.links :refer [INSERT_LINK_COMMAND]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.utils.tooltip :as tooltip]))

(def low-priority 1)

(defn format-quote
  "Format a selection to be a quote block."
  [editor block-type]
  (when (not= block-type "quote")
    (ocall editor "update"
           #(let [selection ($getSelection)]
              (when ($isRangeSelection selection)
                ($setBlocksType selection (fn [] ($createQuoteNode))))))))

(defn development-buttons
  "Some buttons only for development, e.g. to fast insert an image."
  [^LexicalEditor editor debug?]
  (when debug?
    [:<>
     [tooltip/text
      "[Dev] Insert Image"
      [:button.toolbar-item.spaced.text-secondary
       {:type :button
        :on-click #(rf/dispatch [:editor/command editor INSERT_IMAGE_COMMAND #js {:src "https://cdn.pixabay.com/photo/2016/11/14/04/45/elephant-1822636_1280.jpg" :altText "Elephant in a forest"}])}
       [icon :image-file]]]]))

(defn- toolbar-button
  "Icon-only toolbar button named by `label`. Toggles pass `active?`."
  [label icon-key {:keys [on-click active? disabled?]}]
  [tooltip/text label
   [:button.toolbar-item.spaced
    (cond-> {:type :button
             :on-click on-click
             :aria-label label
             :class (when active? "active")
             :disabled disabled?}
      (some? active?) (assoc :aria-pressed (boolean active?)))
    [icon icon-key]]])

(defn- file-upload-button
  "Show a button and a modal to upload own images."
  [_input-label _input-component _icon-component _on-click-event]
  (let [tooltip-visible? (r/atom false)
        close-on-escape #(when (= "Escape" (.-key %)) (reset! tooltip-visible? false))]
    (fn [input-label input-component icon-component on-click-event]
      [tooltip/text
       input-label
       [:span ;; Wrap into a span to make tippy nestable.
        [tooltip/html
         [:div {:on-key-down close-on-escape}
          ;; Use here no form, because forms can't be nested.
          input-component
          [:div.d-flex.mt-2
           [:button.btn.btn-primary.me-auto
            {:type :button
             :on-click (fn []
                         (rf/dispatch on-click-event)
                         (reset! tooltip-visible? false))}
            (labels :editor.toolbar.file-upload/submit)]
           [:button.btn.btn-sm.btn-link.text-dark
            {:type :button
             :on-click #(reset! tooltip-visible? false)}
            (labels :editor.toolbar.file-upload/close)]]]
         [:button.toolbar-item.spaced
          {:on-click #(swap! tooltip-visible? not)
           :on-key-down close-on-escape
           :type :button
           :aria-label input-label}
          icon-component]
         {:visible @tooltip-visible?
          :onHide tooltip/return-focus}
         [:trigger]]]
       {:disabled @tooltip-visible?}])))

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
   {:fx [[:editor/command! [editor INSERT_IMAGE_COMMAND #js {:src url}]]]}))

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
   {:fx [[:editor/command! [editor INSERT_LINK_COMMAND #js {:url (gstring/urlEncode url)
                                                            :text (tools/filename-from-url url)}]]]}))

;; -----------------------------------------------------------------------------

(defn ToolbarPlugin
  "Build a toolbar for the editor."
  [{:keys [file-storage debug? id]}]
  (let [[editor] (useLexicalComposerContext)
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
         #(let [selection ($getSelection)]
            (when ($isRangeSelection selection)
              (let [anchorNode (.getNode (.-anchor selection))
                    element (if (= "root" (.getKey anchorNode)) anchorNode (.getTopLevelElementOrThrow anchorNode))
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
                (strike-through! (.hasFormat selection "strikethrough")))))
         #js [active-editor])]
    (useEffect
     #(mergeRegister
       (ocall active-editor "registerUpdateListener"
              (fn [editor]
                (ocall (oget editor :editorState) "read" update-toolbar)))
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
    [:div.toolbar {:role "group" :aria-label (labels :editor.toolbar/label)}
     [toolbar-button (labels :editor.toolbar/bold) :bold
      {:on-click #(rf/dispatch [:editor/command active-editor FORMAT_TEXT_COMMAND "bold"])
       :active? bold?}]
     [toolbar-button (labels :editor.toolbar/italic) :italic
      {:on-click #(rf/dispatch [:editor/command active-editor FORMAT_TEXT_COMMAND "italic"])
       :active? italic?}]
     [toolbar-button (labels :editor.toolbar/strike-through) :strike-through
      {:on-click #(rf/dispatch [:editor/command active-editor FORMAT_TEXT_COMMAND "strikethrough"])
       :active? strike-through?}]
     [toolbar-button (labels :editor.toolbar/code) :code
      {:on-click #(rf/dispatch [:editor/command active-editor FORMAT_TEXT_COMMAND "code"])
       :active? code?}]
     [toolbar-button (labels :editor.toolbar/quote) :quote-right
      {:on-click #(format-quote active-editor block-type)}]
     [:span.divider {:aria-hidden true}]
     (when file-storage
       [:<>
        [toolbar-button (labels :editor.toolbar/drawing) :pencil-ruler
         {:on-click #(rf/dispatch [:editor/command active-editor INSERT_EXCALIDRAW_COMMAND])}]
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
         [inputs/file [:span.fs-5 (labels :editor.toolbar/file-upload)] "editor-upload-file" [:editors id :file]
          {:required true
           :form "form-upload-a-file"}]
         [icon :file-alt]
         [:editor.upload/file id active-editor file-storage]]
        [:span.divider {:aria-hidden true}]])
     (let [unordered-list? (= block-type "ul")]
       [toolbar-button (labels :editor.toolbar/list-ul) :list
        {:on-click #(rf/dispatch [:editor/command active-editor
                                  (if unordered-list? REMOVE_LIST_COMMAND INSERT_UNORDERED_LIST_COMMAND)])
         :active? unordered-list?}])
     (let [ordered-list? (= block-type "ol")]
       [toolbar-button (labels :editor.toolbar/list-ol) :list-ol
        {:on-click #(rf/dispatch [:editor/command active-editor
                                  (if ordered-list? REMOVE_LIST_COMMAND INSERT_ORDERED_LIST_COMMAND)])
         :active? ordered-list?}])
     [:span.divider {:aria-hidden true}]
     [toolbar-button (labels :editor.toolbar/undo) :undo
      {:on-click #(rf/dispatch [:editor/command active-editor UNDO_COMMAND])
       :disabled? (not can-undo?)}]
     [toolbar-button (labels :editor.toolbar/redo) :redo
      {:on-click #(rf/dispatch [:editor/command active-editor REDO_COMMAND])
       :disabled? (not can-redo?)}]
     [development-buttons active-editor debug?]]))
