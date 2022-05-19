(ns schnaq.interface.components.lexical.config
  (:require ["@lexical/code" :refer [CodeHighlightNode CodeNode]]
            ["@lexical/link" :refer [AutoLinkNode LinkNode]]
            ["@lexical/list" :refer [ListItemNode ListNode]]
            ["@lexical/rich-text" :refer [HeadingNode QuoteNode]]
            ["@lexical/table" :refer [TableCellNode TableNode TableRowNode]]
            [schnaq.interface.components.lexical.nodes.image :refer [ImageNode]]
            [schnaq.interface.components.lexical.nodes.video :refer [VideoNode]]
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

(def initial-config
  "Initial configuration for all editor instances."
  #js {:theme theme :onError #(log/error %)
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
