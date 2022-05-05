(ns schnaq.interface.components.lexical.editor
  (:require ["@lexical/code" :refer [CodeNode CodeHighlightNode]]
            ["@lexical/link" :refer [LinkNode AutoLinkNode]]
            ["@lexical/list" :refer [ListItemNode ListNode]]
            ["@lexical/rich-text" :refer [HeadingNode QuoteNode]]
            ["@lexical/table" :refer [TableCellNode TableNode TableRowNode]]
            ["@lexical/markdown" :refer [TRANSFORMERS $convertFromMarkdownString $convertToMarkdownString]]
            ["@lexical/react/LexicalComposer" :as LexicalComposer]
            ["@lexical/react/LexicalContentEditable" :as ContentEditable]
            ["@lexical/react/LexicalHistoryPlugin" :refer [HistoryPlugin]]
            ["@lexical/react/LexicalLinkPlugin" :as LinkPlugin]
            ["@lexical/react/LexicalListPlugin" :as ListPlugin]
            ["@lexical/react/LexicalMarkdownShortcutPlugin" :as MarkdownShortcut]
            ["@lexical/react/LexicalOnChangePlugin" :as OnChangePlugin]
            ["@lexical/react/LexicalRichTextPlugin" :as RichTextPlugin]
            [reagent.core :as r]
            [schnaq.interface.components.lexical.plugins.toolbar :refer [toolbar-plugin]]
            [schnaq.interface.components.lexical.plugins.tree-view :refer [tree-view-plugin]]
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
      :underlineStrikethrough "editor-text-underlineStrikethrough"
      :code "editor-text-code"}
    :code "editor-code"
    :codeHighlight
    #js
     {:atrule "editor-tokenAttr"
      :attr "editor-tokenAttr"
      :boolean "editor-tokenProperty"
      :builtin "editor-tokenSelector"
      :cdata "editor-tokenComment"
      :char "editor-tokenSelector"
      :class "editor-tokenFunction"
      :class-name "editor-tokenFunction"
      :comment "editor-tokenComment"
      :constant "editor-tokenProperty"
      :deleted "editor-tokenProperty"
      :doctype "editor-tokenComment"
      :entity "editor-tokenOperator"
      :function "editor-tokenFunction"
      :important "editor-tokenVariable"
      :inserted "editor-tokenSelector"
      :keyword "editor-tokenAttr"
      :namespace "editor-tokenVariable"
      :number "editor-tokenProperty"
      :operator "editor-tokenOperator"
      :prolog "editor-tokenComment"
      :property "editor-tokenProperty"
      :punctuation "editor-tokenPunctuation"
      :regex "editor-tokenVariable"
      :selector "editor-tokenSelector"
      :string "editor-tokenSelector"
      :symbol "editor-tokenProperty"
      :tag "editor-tokenProperty"
      :url "editor-tokenOperator"
      :variable "editor-tokenVariable"}})

(defn- on-error [error]
  (log/error error))

(def ^:private initial-config
  #js {:theme theme :onError on-error
       :nodes #js [AutoLinkNode
                   CodeNode
                   CodeHighlightNode
                   HeadingNode
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
      [:> LexicalComposer {:initialConfig initial-config}
       [:div.editor-container
        [:f> toolbar-plugin]
        [:div.editor-inner
         [:> RichTextPlugin
          {:contentEditable (r/as-element [:> ContentEditable {:className "editor-input"}])}]
         [:> HistoryPlugin {}]
         [:f> tree-view-plugin]
        ;;  [:> LinkPlugin]
         [:> ListPlugin]
         [:> MarkdownShortcut #js {:transformers TRANSFORMERS}]
         [:> OnChangePlugin {:onChange (fn [editorState]
                                         (.read editorState
                                                #(reset! content ($convertToMarkdownString TRANSFORMERS))))}]]]])))

;; -----------------------------------------------------------------------------

(defn- build-page []
  [:<>
   [:h2 "Lexical"]
   [Editor]])

(defn page []
  [build-page])
