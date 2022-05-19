(ns schnaq.interface.components.lexical.editor
  (:require ["@lexical/code" :refer [CodeHighlightNode CodeNode]]
            ["@lexical/link" :refer [AutoLinkNode LinkNode]]
            ["@lexical/list" :refer [ListItemNode ListNode]]
            ["@lexical/markdown" :refer [$convertToMarkdownString]]
            ["@lexical/react/LexicalAutoFocusPlugin" :as AutoFocusPlugin]
            ["@lexical/react/LexicalComposer" :as LexicalComposer]
            ["@lexical/react/LexicalContentEditable" :as ContentEditable]
            ["@lexical/react/LexicalHistoryPlugin" :refer [HistoryPlugin]]
            ["@lexical/react/LexicalLinkPlugin" :as LinkPlugin]
            ["@lexical/react/LexicalListPlugin" :as ListPlugin]
            ["@lexical/react/LexicalOnChangePlugin" :as OnChangePlugin]
            ["@lexical/react/LexicalRichTextPlugin" :as RichTextPlugin]
            ["@lexical/rich-text" :refer [HeadingNode QuoteNode]]
            ["@lexical/table" :refer [TableCellNode TableNode TableRowNode]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.interface.components.lexical.nodes.image :refer [ImageNode]]
            [schnaq.interface.components.lexical.nodes.video :refer [VideoNode]]
            [schnaq.interface.components.lexical.plugins.autolink :refer [autolink-plugin]]
            [schnaq.interface.components.lexical.plugins.images :refer [ImagesPlugin]]
            [schnaq.interface.components.lexical.plugins.markdown :refer [markdown-shortcut-plugin schnaq-transformers]]
            [schnaq.interface.components.lexical.plugins.toolbar :refer [ToolbarPlugin]]
            [schnaq.interface.components.lexical.plugins.tree-view :refer [TreeViewPlugin]]
            [schnaq.interface.components.lexical.plugins.video :refer [VideoPlugin]]
            [taoensso.timbre :as log]))

(def theme
  #js
   {:ltr "ltr"
    :rtl "rtl"
    :placeholder "editor-placeholder"
    :paragraph "editor-paragraph"
    :quote "editor-quote"
    :heading
    #js
     {:h1 "editor-heading-h1"
      :h2 "editor-heading-h2"
      :h3 "editor-heading-h3"
      :h4 "editor-heading-h4"
      :h5 "editor-heading-h5"}
    :list
    #js
     {:nested #js {:listitem "editor-nested-listitem"}
      :ol "editor-list-ol"
      :ul "editor-list-ul"
      :listitem "editor-listitem"}
    :image "editor-image"
    :link "editor-link"
    :text
    #js
     {:bold "editor-text-bold"
      :italic "editor-text-italic"
      :overflowed "editor-text-overflowed"
      :hashtag "editor-text-hashtag"
      :underline "editor-text-underline"
      :strikethrough "editor-text-strikethrough"
      :underline-strikethrough "editor-text-underline-strikethrough"
      :code "editor-text-code"}
    :code "editor-code"
    :codeHighlight
    #js
     {:atrule "editor-token-attribute"
      :attr "editor-token-attribute"
      :boolean "editor-token-property"
      :builtin "editor-token-selector"
      :cdata "editor-token-comment"
      :char "editor-token-selector"
      :class "editor-token-function"
      :class-name "editor-token-function"
      :comment "editor-token-comment"
      :constant "editor-token-property"
      :deleted "editor-token-property"
      :doctype "editor-token-comment"
      :entity "editor-token-operator"
      :function "editor-token-function"
      :important "editor-token-variable"
      :inserted "editor-token-selector"
      :keyword "editor-token-attribute"
      :namespace "editor-token-variable"
      :number "editor-token-property"
      :operator "editor-token-operator"
      :prolog "editor-token-comment"
      :property "editor-token-property"
      :punctuation "editor-token-punctuation"
      :regex "editor-token-variable"
      :selector "editor-token-selector"
      :string "editor-token-selector"
      :symbol "editor-token-property"
      :tag "editor-token-property"
      :url "editor-token-operator"
      :variable "editor-token-variable"}})

(defn- on-error [error]
  (log/error error))

(def ^:private initial-config
  #js {:theme theme :onError on-error
       :nodes #js [AutoLinkNode
                   CodeNode
                   CodeHighlightNode
                   HeadingNode
                   ImageNode
                   VideoNode
                   LinkNode
                   ListNode
                   ListItemNode
                   QuoteNode
                   TableCellNode
                   TableNode
                   TableRowNode]})

(defn- editor
  "Create an editor instance. Takes as a first argument the editor's options and
  as a second argument attributes for the wrapping div."
  [{:keys [id focus? debug? toolbar?] :as options} ?attributes]
  [:article.lexical-editor
   [:> LexicalComposer {:initialConfig initial-config}
    [:div.editor-container (merge {} ?attributes)
     (when toolbar? [:f> ToolbarPlugin options])
     [:div.editor-inner
      [:> RichTextPlugin
       {:contentEditable (r/as-element [:> ContentEditable {:className "editor-input"}])}]
      [:> HistoryPlugin {}]
      [autolink-plugin]
      [:f> ImagesPlugin]
      [:f> VideoPlugin]
      [:> LinkPlugin]
      [:> ListPlugin]
      [markdown-shortcut-plugin]
      (when focus? [:> AutoFocusPlugin])
      (when debug? [:f> TreeViewPlugin])
      [:> OnChangePlugin
       {:onChange (fn [editorState]
                    (.read editorState
                           #(rf/dispatch [:editor/content id ($convertToMarkdownString schnaq-transformers)])))}]]]]])

;; -----------------------------------------------------------------------------

(defn- build-page []
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
     [:section.pt-3
      [:p "Editor without toolbar"]
      [editor {:id :playground-naked-editor} {:class "pb-3"}]]
     [:section
      [:p "Editor with toolbar"]
      [editor {:id :playground-naked-editor-with-toolbar
               :toolbar? true}
       {:class "pb-3"}]]
     [:section
      [:p "Editor with toolbar"]
      [editor {:id :playground-naked-editor-with-toolbar
               :toolbar? true}
       {:class "pb-3"}]]]))

(defn playground []
  [build-page])

(rf/reg-event-db
 :editor/content
 (fn [db [_ editor-id content]]
   (assoc-in db [:editor :content editor-id] content)))

(rf/reg-sub
 :editor/content
 (fn [db [_ editor-id]]
   (get-in db [:editor :content editor-id])))
