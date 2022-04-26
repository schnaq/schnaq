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
            [goog.dom :as gdom]
            [reagent.core :as r]
            [taoensso.timbre :as log]))

(def theme
  #js {:ltr "ltr"
       :rtl "rtl"
       :placeholder "editor-placeholder",
       :paragraph "editor-paragraph"})

(defn- on-error [error]
  (log/error error))

(defn TreeViewPlugin
  []
  (let [[editor] (r/create-element (useLexicalComposerContext))]
    [:h1 "huhu"]
    #_[:> LexicalTreeView
       {:viewClassName "tree-view-output",
        :timeTravelPanelClassName "debug-timetravel-panel",
        :timeTravelButtonClassName "debug-timetravel-button",
        :timeTravelPanelSliderClassName "debug-timetravel-panel-slider",
        :timeTravelPanelButtonClassName "debug-timetravel-panel-button",
        :editor editor}]))

(defn- Editor
  []
  (let [initial-config #js {:theme theme, :onError on-error}]
    [:<>
     [:h2 "Editor"]
     [:> LexicalComposer {:initialConfig initial-config}
      [:div.editor-container
       [:> LexicalPlainTextPlugin
        {:contentEditable (r/as-element [:> LexicalContentEditable {:className "editor-input"}])}]
       [:> HistoryPlugin {}]]]]))

(defn- view []
  [:<>
   [:div {:style {:background-color "green"}}
    [Editor]]])

(defn page []
  [view])
