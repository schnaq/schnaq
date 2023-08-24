(ns schnaq.interface.components.lexical.config
  (:require ["@lexical/code" :refer [CodeHighlightNode CodeNode]]
            ["@lexical/link" :refer [AutoLinkNode LinkNode]]
            ["@lexical/list" :refer [ListItemNode ListNode]]
            ["@lexical/markdown" :refer [$convertFromMarkdownString]]
            ["@lexical/react/LexicalHorizontalRuleNode" :refer [HorizontalRuleNode]]
            ["@lexical/rich-text" :refer [HeadingNode QuoteNode]]
            ["@lexical/table" :refer [TableCellNode TableNode TableRowNode]]
            ["lexical" :refer [$createParagraphNode]]
            [re-frame.core :as rf]
            [schnaq.interface.components.lexical.nodes.excalidraw :refer [ExcalidrawNode]]
            [schnaq.interface.components.lexical.nodes.image :refer [ImageNode]]
            [schnaq.interface.components.lexical.nodes.video :refer [VideoNode]]
            [schnaq.interface.components.lexical.plugins.markdown :refer [schnaq-transformers]]
            [taoensso.timbre :as log]))

(def ^:private theme
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

(defn- initialize-editor-state
  "Initial editor state. Called only once when the editor is loaded.
  Convert initial-content from markdown to lexical nodes or create an empty
  paragraph node."
  [id initial-content]
  (fn [editor]
    (rf/dispatch [:editor/register id editor])
    (rf/dispatch [:editor/content id initial-content])
    (if initial-content
      ($convertFromMarkdownString initial-content schnaq-transformers)
      ($createParagraphNode))))

(defn initial-config
  "Initial configuration for all editor instances."
  [id initial-content]
  #js {:theme theme :onError #(log/error %)
       :nodes #js [AutoLinkNode
                   CodeNode
                   CodeHighlightNode
                   ExcalidrawNode
                   HeadingNode
                   HorizontalRuleNode,
                   ImageNode
                   VideoNode
                   LinkNode
                   ListNode
                   ListItemNode
                   QuoteNode
                   TableCellNode
                   TableNode
                   TableRowNode]
       :editorState (initialize-editor-state id initial-content)})

(def sample-markdown-input
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
<a href=\"https://schnaq.com\">Click Me</a>")
