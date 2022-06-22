(ns schnaq.interface.components.lexical.editor
  "Creating our own editor, based on lexical. "
  (:require ["@lexical/markdown" :refer [$convertToMarkdownString]]
            ["@lexical/react/LexicalAutoFocusPlugin" :refer [AutoFocusPlugin]]
            ["@lexical/react/LexicalClearEditorPlugin" :refer [ClearEditorPlugin]]
            ["@lexical/react/LexicalComposer" :refer [LexicalComposer]]
            ["@lexical/react/LexicalContentEditable" :refer [ContentEditable]]
            ["@lexical/react/LexicalHistoryPlugin" :refer [HistoryPlugin]]
            ["@lexical/react/LexicalLinkPlugin" :refer [LinkPlugin]]
            ["@lexical/react/LexicalListPlugin" :refer [ListPlugin]]
            ["@lexical/react/LexicalOnChangePlugin" :refer [OnChangePlugin]]
            ["@lexical/react/LexicalRichTextPlugin" :refer [RichTextPlugin]]
            ["lexical" :refer [CLEAR_EDITOR_COMMAND CLEAR_HISTORY_COMMAND]]
            [oops.core :refer [ocall]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.interface.components.lexical.config :refer [initial-config sample-markdown-input]]
            [schnaq.interface.components.lexical.plugins.autolink :refer [autolink-plugin]]
            [schnaq.interface.components.lexical.plugins.markdown :refer [markdown-shortcut-plugin schnaq-transformers]]
            [schnaq.interface.components.lexical.plugins.text-change :refer [TextChangePlugin]]
            [schnaq.interface.components.lexical.plugins.toolbar :refer [ToolbarPlugin]]
            [schnaq.interface.components.lexical.plugins.tree-view :refer [TreeViewPlugin]]))

(defn editor
  "Create a lexical editor instance.
   
  **Options**
  * `id`: Required, to store the editor's content.
  * `focus?`: Adds autofocus for the editor.
  * `debug?`: Shows debug information and adds additional buttons.
  * `toolbar?` Shows the toolbar.
  * `initial-content`: Add initial content to the editor, which gets parsed into
   the editor's node-structure. Takes markdown or normal strings.
  * `on-text-change`: If the current text-block is modified, call the provided
  function.
  * `placeholder`: Define a placeholder for the editor."
  [{:keys [id focus? debug? toolbar? initial-content on-text-change placeholder] :as options} attributes]
  [:> LexicalComposer {:initialConfig (initial-config id initial-content)}
   [:section.lexical-editor attributes
    [:div.editor-container
     (when toolbar? [:f> ToolbarPlugin options])
     [:div.editor-inner
      [:> RichTextPlugin
       (cond-> {:contentEditable (r/as-element [:> ContentEditable {:className "editor-input"}])}
         placeholder (assoc :placeholder (r/as-element [:div.editor-placeholder placeholder])))]
      [:> HistoryPlugin {}]
      [autolink-plugin]
      [:> ClearEditorPlugin]
      [:> LinkPlugin]
      (when on-text-change [:f> TextChangePlugin {:on-text-change on-text-change}])
      [:> ListPlugin]
      [markdown-shortcut-plugin]
      (when focus? [:> AutoFocusPlugin])
      (when debug? [:f> TreeViewPlugin])
      (when id [:> OnChangePlugin
                {:onChange (fn [editor-state _editor]
                             (ocall editor-state "read"
                                    #(rf/dispatch [:editor/content id ($convertToMarkdownString schnaq-transformers)])))}])]]]])

;; -----------------------------------------------------------------------------

(defn- build-playground []
  (let [editor-id :playground-editor]
    [:div.container.pt-5
     [:h1 "Lexical"]
     [:p "Configured share-hash: " @(rf/subscribe [:schnaq/share-hash])]
     [:div.row
      [:div.col-6
       [editor {:id editor-id
                :file-storage :schnaq/by-share-hash
                :focus? true
                :debug? true
                :toolbar? true
                :placeholder "Write your code here..."}]]
      [:div.col-6
       [:div.card
        [:div.card-body
         [:div.card-title "Markdown content"]
         [:div.card-text.overflow-scroll
          [:pre [:code @(rf/subscribe [:editor/content editor-id])]]]]]]]
     [:section
      [:p "Pre-filled editor"]
      [editor {:id :playground-pre-filled
               :toolbar? true
               :initial-content sample-markdown-input}
       {:class "mb-3"}]]
     [:section.pt-3
      [:p "Editor without toolbar"]
      [editor {:id :playground-naked-editor} {:class "mb-3"}]]
     [:section
      [:p "Editor with toolbar"]
      [editor {:id :playground-naked-editor-with-toolbar
               :toolbar? true}
       {:class "mb-3"}]]]))

;; -----------------------------------------------------------------------------

(defn playground []
  [build-playground])

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 ;; Register editor in app-db and configure all plugins.
 :editor/register
 (fn [{:keys [db]} [_ editor-id ^LexicalEditor editor]]
   {:db (assoc-in db [:editors editor-id :editor] editor)
    :fx [[:editor.plugins.register/images editor]
         [:editor.plugins.register/videos editor]
         [:editor.plugins.register/links editor]]}))

(rf/reg-event-db
 :editor/content
 (fn [db [_ editor-id content]]
   (assoc-in db [:editors editor-id :content] content)))

(rf/reg-event-fx
 :editor/clear
 (fn [{:keys [db]} [_ editor-id]]
   (let [editor (get-in db [:editors editor-id :editor])]
     {:db (update-in db [:editors editor-id] dissoc :content)
      :fx [[:editor/command! [editor CLEAR_EDITOR_COMMAND nil]]
           [:editor/command! [editor CLEAR_HISTORY_COMMAND nil]]]})))

(rf/reg-event-fx
 :editor/command
 (fn [_ [_ editor command payload]]
   {:fx [[:editor/command! [editor command payload]]]}))

(rf/reg-fx
 :editor/command!
 (fn [[^LexicalEditor editor command payload]]
   (ocall editor "dispatchCommand" command payload)))

(rf/reg-sub
 :editor/content
 (fn [db [_ editor-id]]
   (get-in db [:editors editor-id :content])))
