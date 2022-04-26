(ns schnaq.interface.components.editor
  (:require ["@lexical/react/LexicalComposer" :as LexicalComposer]
            ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["@lexical/react/LexicalContentEditable" :as LexicalContentEditable]
            ["@lexical/react/LexicalPlainTextPlugin" :as LexicalPlainTextPlugin]
            ["@lexical/react/LexicalRichTextPlugin" :as LexicalRichTextPlugin]
            ["@lexical/react/LexicalHistoryPlugin" :refer [HistoryPlugin]]
            ["@lexical/react/LexicalOnChangePlugin" :as LexicalOnChangePlugin]
            ["@lexical/react/LexicalTreeView" :as LexicalTreeView]
            ["lexical" :refer [$getRoot $getSelection createEditor]]
            ["react" :refer [useEffect] :as react]
            ["/editor/plugins/ToolbarPlugin" :as ToolbarPlugin]
            [reagent.core :as r]
            [taoensso.timbre :as log]))

#_(def theme
    #js {:ltr "ltr"
         :rtl "rtl"
         :placeholder "editor-placeholder",
         :paragraph "editor-paragraph"})

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
        [:> LexicalPlainTextPlugin
         {:contentEditable (r/as-element [:> LexicalContentEditable {:className "editor-input"}])}]
        [:> HistoryPlugin {}]]]]]))

;; -----------------------------------------------------------------------------

(defn- RichTextEditor
  []
  (let [initial-config #js {:theme theme :onError on-error}]
    [:<>
     [:h2 "RichTextEditor"]
     [:> LexicalComposer {:initialConfig initial-config}
      [:div.editor-container
       ;; toolbar plugin
       [:div.editor-inner
        [:> LexicalRichTextPlugin
         {:contentEditable (r/as-element [:> LexicalContentEditable {:className "editor-input"}])}]
        [:> HistoryPlugin {}]]]]]))

(defn- view []
  [:<>
   [Editor]
   [RichTextEditor]])

(defn page []
  [view])
