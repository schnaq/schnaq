(ns schnaq.interface.components.editor
  (:require ["@lexical/react/LexicalComposer" :as LexicalComposer]
            ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            ["@lexical/react/LexicalContentEditable" :as LexicalContentEditable]
            ["@lexical/react/LexicalPlainTextPlugin" :as LexicalPlainTextPlugin]
            ["@lexical/react/LexicalHistoryPlugin" :refer [HistoryPlugin]]
            ["@lexical/react/LexicalOnChangePlugin" :as LexicalOnChangePlugin]
            ["@lexical/react/LexicalTreeView" :as LexicalTreeView]
            ["lexical" :refer [$getRoot $getSelection createEditor]]
            ["react" :refer [useEffect]]
            [goog.dom :as gdom]
            [reagent.core :as r]
            [taoensso.timbre :as log]))

(def theme
  #js {:ltr "ltr"
       :rtl "rtl"
       :placeholder "editor-placeholder",
       :paragraph "editor-paragraph"})

(defn- on-change [editor-state]
  (let [root ($getRoot)
        selection ($getSelection)]
    (log/info root selection)))

(defn- on-error [error]
  (log/error error))

(defn auto-focus-plugin
  []
  (let [[editor] (useLexicalComposerContext)]
    (useEffect (fn [] (.focus editor)) #js [editor])
    nil))

(defn TreeViewPlugin
  []
  (let [[editor] (useLexicalComposerContext)]
    [:> LexicalTreeView
     #js {:viewClassName "tree-view-output",
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
        {:contentEditable (r/as-element [:> LexicalContentEditable]) ;;#js {:className "editor-input"}
         :placeholder (r/as-element [:div "Enter some text..."])}]
       #_[:> LexicalOnChangePlugin {:onChange on-change}]
       [:> HistoryPlugin {}]
       #_[:> auto-focus-plugin]]]]))

(defn- view []
  [:<>
   [:div {:style {:background-color "green"}}
    [:h1 "huhu"]
    [:div#editor]
    [Editor]]])

(defn page []
  [view])

(comment

  (let [editor (createEditor #js {:theme theme})]
    (.setRootElement editor (gdom/getElement "editor")))

  nil)
