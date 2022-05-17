(ns schnaq.interface.components.lexical.editor
  (:require ["@lexical/code" :refer [CodeHighlightNode CodeNode]]
            ["@lexical/link" :refer [AutoLinkNode LinkNode]]
            ["@lexical/list" :refer [ListItemNode ListNode]]
            ["@lexical/markdown" :refer [$convertToMarkdownString]]
            ["@lexical/react/LexicalComposer" :as LexicalComposer]
            ["@lexical/react/LexicalContentEditable" :as ContentEditable]
            ["@lexical/react/LexicalHistoryPlugin" :refer [HistoryPlugin]]
            ["@lexical/react/LexicalLinkPlugin" :as LinkPlugin]
            ["@lexical/react/LexicalListPlugin" :as ListPlugin]
            ["@lexical/react/LexicalOnChangePlugin" :as OnChangePlugin]
            ["@lexical/react/LexicalRichTextPlugin" :as RichTextPlugin]
            ["@lexical/rich-text" :refer [HeadingNode QuoteNode]]
            ["@lexical/table" :refer [TableCellNode TableNode TableRowNode]]
            [reagent.core :as r]
            [schnaq.interface.components.lexical.nodes.image :refer [ImageNode]]
            [schnaq.interface.components.lexical.nodes.video :refer [VideoNode]]
            [schnaq.interface.components.lexical.plugins.autolink :refer [autolink-plugin]]
            [schnaq.interface.components.lexical.plugins.images :refer [ImagesPlugin]]
            [schnaq.interface.components.lexical.plugins.markdown :refer [markdown-shortcut-plugin schnaq-transformers]]
            [schnaq.interface.components.lexical.plugins.toolbar :refer [ToolbarPlugin]]
            [schnaq.interface.components.lexical.plugins.tree-view :refer [tree-view-plugin]]
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

(defn- Editor
  []
  (let [content (r/atom nil)]
    (fn []
      [:<>
       [:button.btn.btn-primary {:on-click #(.log js/console @content)}
        "Mach was"]
       [:section.lexical-editor
        [:> LexicalComposer {:initialConfig initial-config}
         [:div.editor-container
          [:f> ToolbarPlugin]
          [:div.editor-inner
           [:> RichTextPlugin
            {:contentEditable (r/as-element [:> ContentEditable {:className "editor-input"}])}]
           [:> HistoryPlugin {}]
           [:f> tree-view-plugin]
           [autolink-plugin]
           [:f> ImagesPlugin]
           [:f> VideoPlugin]
           [:> LinkPlugin]
           [:> ListPlugin]
           [markdown-shortcut-plugin]
           [:> OnChangePlugin
            {:onChange (fn [editorState]
                         (.read editorState
                                #(reset! content ($convertToMarkdownString schnaq-transformers))))}]]]]]])))

;; -----------------------------------------------------------------------------

(defn- build-page []
  [:<>
   [:h2 "Lexical"]
   [Editor]])

(defn page []
  [build-page])
