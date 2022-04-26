(ns schnaq.interface.components.editor
  (:require ["@lexical/react/LexicalComposer" :as LexicalComposer]
            ["@lexical/react/LexicalContentEditable" :as ContentEditable]
            ["@lexical/react/LexicalHistoryPlugin" :refer [HistoryPlugin]]
            ["@lexical/react/LexicalPlainTextPlugin" :as PlainTextPlugin]
            ["@lexical/react/LexicalRichTextPlugin" :as RichTextPlugin]
            [reagent.core :as r]
            [schnaq.interface.components.plugins.on-change :refer [on-change-plugin]]
            [schnaq.interface.components.plugins.toolbar :refer [toolbar-plugin]]
            [schnaq.interface.components.plugins.tree-view :refer [tree-view-plugin]]
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

(defn- Editor
  []
  (let [initial-config #js {:theme theme :onError on-error}]
    [:<>
     [:h2 "Editor"]
     [:> LexicalComposer {:initialConfig initial-config}
      [:div.editor-container
       [:div.editor-inner
        [:> PlainTextPlugin
         {:contentEditable (r/as-element [:> ContentEditable {:className "editor-input"}])}]
        [:> HistoryPlugin {}]
        [:f> tree-view-plugin]
        [on-change-plugin]]]]]))

;; -----------------------------------------------------------------------------

(defn- RichTextEditor
  []
  (let [initial-config #js {:theme theme :onError on-error}]
    [:<>
     [:h2 "RichTextEditor"]
     [:> LexicalComposer {:initialConfig initial-config}
      [:div.editor-container
       [:f> toolbar-plugin]
       [:div.editor-inner
        [:> RichTextPlugin
         {:contentEditable (r/as-element [:> ContentEditable {:className "editor-input"}])}]
        [:> HistoryPlugin {}]
        [:f> tree-view-plugin]]]]]))

(defn- view []
  [:<>
   [Editor]
   [RichTextEditor]])

(defn page []
  [view])
