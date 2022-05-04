(ns schnaq.interface.components.tiptap
  (:require ["@tiptap/extension-highlight" :as Highlight]
            ["@tiptap/extension-link" :as Link]
            ["@tiptap/extension-typography" :as Typography]
            ["@tiptap/react" :refer [EditorContent useEditor]]
            ["@tiptap/starter-kit" :as StarterKit]
            [reagent.core :as r]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.utils.tooltip :as tooltip]))

;; <select class= "form-select" aria-label= "Default select example" >
  ;; <option selected>Open this select menu</option>
  ;; <option value= "1" >One</option>
  ;; <option value= "2" >Two</option>
  ;; <option value= "3" >Three</option>
;; </select>

(defn- select-heading []
  (let [selection (r/atom nil)]
    (fn []
      [tooltip/html
       [:<> [:button.btn.btn-sm.btn-outline-dark
             {:on-click #(reset! selection 1)}
             "fuu"] "baa"]
       [:button.toolbar-item
        [icon :rocket]]
       nil
       #_[:select.form-select {:aria-label "Select a heading"}
          [:option {:value 1} ""]]])))

(defn MenuBar [{:keys [editor]}]
  (when editor
    [:section.toolbar
     [select-heading]
     [:button.toolbar-item
      {:on-click #(-> editor .chain .focus .toggleBold .run)}
      [icon :bold]]
     [:button.toolbar-item
      {:on-click #(-> editor .chain .focus .toggleItalic .run)}
      [icon :italic]]
     [:button.toolbar-item
      {:on-click #(-> editor .chain .focus .toggleStrike .run)}
      [icon :strikethrough]]
     [:button.toolbar-item
      {:on-click #(-> editor .chain .focus .toggleBulletList .run)}
      [icon :list]]
     [:button.toolbar-item
      {:on-click #(-> editor .chain .focus .toggleOrderedList .run)}
      [icon :list-ol]]
     [:button.toolbar-item
      {:on-click #(-> editor .chain .focus .toggleCodeBlock .run)}
      [icon :code]]
     [:button.toolbar-item
      {:on-click #(-> editor .chain .focus .toggleBlockquote .run)}
      [icon :quote-right]]
     [:button.toolbar-item
      {:on-click #(-> editor .chain .focus .undo .run)}
      [icon :undo]]
     [:button.toolbar-item
      {:on-click #(-> editor .chain .focus .redo .run)}
      [icon :redo]]]))

(defn TipTap []
  (let
   [editor (useEditor
            #js {:extensions #js [StarterKit ;; basic formatting, e.g. bold, italic, lists, ...
                                  Highlight ;; Markdown Shortcuts during typing
                                  Typography ;; visual effects, e.g. converting (c) to a symbol
                                  Link ;; automatically make links
                                  ]
                 :content "<p>Huhu</p><ul><li><p>Hello World!</p></li><li><p>asd</p></li></ul><p>was ist hier los?</p>"})]
    [:<>
     [MenuBar {:editor editor}]
     [:> EditorContent {:editor editor}]
     [:button {:on-click #(.log js/console (.getHTML editor))}
      "to HTML"]]))

;; -----------------------------------------------------------------------------

(defn- build-page []
  [:<>
   [:h1 "TipTap"]
   [:f> TipTap]])

(defn page []
  [build-page])
