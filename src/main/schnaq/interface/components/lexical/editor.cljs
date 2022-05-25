(ns schnaq.interface.components.lexical.editor
  "Creating our own editor, based on lexical. "
  (:require ["@lexical/markdown" :refer [$convertFromMarkdownString
                                         $convertToMarkdownString]]
            ["@lexical/react/LexicalAutoFocusPlugin" :as AutoFocusPlugin]
            ["@lexical/react/LexicalComposer" :as LexicalComposer]
            ["@lexical/react/LexicalContentEditable" :as ContentEditable]
            ["@lexical/react/LexicalHistoryPlugin" :refer [HistoryPlugin]]
            ["@lexical/react/LexicalLinkPlugin" :as LinkPlugin]
            ["@lexical/react/LexicalListPlugin" :as ListPlugin]
            ["@lexical/react/LexicalOnChangePlugin" :as OnChangePlugin]
            ["@lexical/react/LexicalRichTextPlugin" :as RichTextPlugin]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.interface.components.lexical.config :refer [initial-config]]
            [schnaq.interface.components.lexical.plugins.autolink :refer [autolink-plugin]]
            [schnaq.interface.components.lexical.plugins.images :refer [ImagesPlugin]]
            [schnaq.interface.components.lexical.plugins.links :refer [LinksPlugin]]
            [schnaq.interface.components.lexical.plugins.markdown :refer [markdown-shortcut-plugin schnaq-transformers]]
            [schnaq.interface.components.lexical.plugins.text-change :refer [TextChangePlugin]]
            [schnaq.interface.components.lexical.plugins.toolbar :refer [ToolbarPlugin]]
            [schnaq.interface.components.lexical.plugins.tree-view :refer [TreeViewPlugin]]
            [schnaq.interface.components.lexical.plugins.video :refer [VideoPlugin]]))

(def ^:private sample-markdown-input
  "**Bold** *Italic* ~~Strikethrough~~ `Code`
> Quote
- list item 1
- list item 2
1. sorted list item 1
2. sorted list item 2
![Elephant in a forest](https://cdn.pixabay.com/photo/2016/11/14/04/45/elephant-1822636_1280.jpg)
![](https://s3.schnaq.com/schnaq-common/logos/schnaq.webp)
<a href=\"javascript:alert('XSS')\">Click Me</a>
[Click Me](javascript:alert('Uh oh...'))
[Some Link](https://schnaq.com)
<a href=\"https://schnaq.com\">Click Me</a>
")

(defn editor
  "Create an editor instance. Takes as a first argument the editor's options and
  as a second argument attributes for the wrapping div.
   
  **Options**
  * `id`: Required, to store the editor's content.
  * `focus?`: Adds autofocus for the editor.
  * `debug?`: Shows debug information and adds additional buttons.
  * `toolbar?` Shows the toolbar.
  * `initial-content`: Add initial content to the editor, which gets parsed into
   the editor's node-structure. Takes markdown or normal strings.
  * `on-text-change`: If the current text-block is modified, call the provided
  function.
   "
  [{:keys [id focus? debug? toolbar? initial-content on-text-change] :as options} attributes]
  [:article.lexical-editor
   [:> LexicalComposer {:initialConfig initial-config}
    [:div.editor-container attributes
     (when toolbar? [:f> ToolbarPlugin options])
     [:div.editor-inner
      [:> RichTextPlugin
       (cond-> {:contentEditable (r/as-element [:> ContentEditable {:className "editor-input"}])}
         initial-content (assoc :initialEditorState #($convertFromMarkdownString initial-content schnaq-transformers)))]
      [:> HistoryPlugin {}]
      [autolink-plugin]
      [:f> ImagesPlugin]
      [:f> VideoPlugin]
      [:> LinkPlugin]
      [:f> LinksPlugin]
      (when on-text-change [:f> TextChangePlugin {:on-text-change on-text-change}])
      [:> ListPlugin]
      [markdown-shortcut-plugin]
      (when focus? [:> AutoFocusPlugin])
      (when debug? [:f> TreeViewPlugin])
      (when id [:> OnChangePlugin
                {:onChange (fn [editorState]
                             (.read editorState
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
                :toolbar? true}]]
      [:div.col-6
       [:div.card
        [:div.card-body
         [:div.card-title "Markdown content"]
         [:div.card-text.overflow-scroll
          [:pre [:code @(rf/subscribe [:editor/content editor-id])]]]]]]]
     [:section
      [:p "Pre-filled editor"]
      [editor {:id :playground-naked-editor-with-toolbar
               :toolbar? true
               :initial-content sample-markdown-input}
       {:class "pb-3"}]]
     [:section.pt-3
      [:p "Editor without toolbar"]
      [editor {:id :playground-naked-editor} {:class "pb-3"}]]
     [:section
      [:p "Editor with toolbar"]
      [editor {:id :playground-naked-editor-with-toolbar
               :toolbar? true}
       {:class "pb-3"}]]]))

;; -----------------------------------------------------------------------------

(defn playground []
  [build-playground])

;; -----------------------------------------------------------------------------

(rf/reg-event-db
 :editor/content
 (fn [db [_ editor-id content]]
   (assoc-in db [:editors editor-id :content] content)))

(rf/reg-sub
 :editor/content
 (fn [db [_ editor-id]]
   (get-in db [:editors editor-id :content])))
