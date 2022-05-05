(ns schnaq.interface.components.tiptap
  (:require ["@tiptap/extension-highlight" :as Highlight]
            ["@tiptap/extension-image" :as Image]
            ["@tiptap/extension-link" :as Link]
            ["@tiptap/extension-typography" :as Typography]
            ["@tiptap/react" :refer [EditorContent useEditor]]
            ["@tiptap/starter-kit" :as StarterKit]
            [schnaq.interface.components.icons :refer [icon]]))

(defn MenuBar [{:keys [editor]}]
  (when editor
    [:section.tiptap-menubar
     [:button.menubar-item
      {:on-click #(-> editor .chain .focus .toggleBold .run)}
      [icon :bold]]
     [:button.menubar-item
      {:on-click #(-> editor .chain .focus .toggleItalic .run)}
      [icon :italic]]
     [:button.menubar-item
      {:on-click #(-> editor .chain .focus .toggleStrike .run)}
      [icon :strikethrough]]
     [:button.menubar-item
      {:on-click #(-> editor .chain .focus .toggleBulletList .run)}
      [icon :list]]
     [:button.menubar-item
      {:on-click #(-> editor .chain .focus .toggleOrderedList .run)}
      [icon :list-ol]]
     [:button.menubar-item
      {:on-click #(-> editor .chain .focus .toggleCodeBlock .run)}
      [icon :code]]
     [:button.menubar-item
      {:on-click #(-> editor .chain .focus .toggleBlockquote .run)}
      [icon :quote-right]]
     [:button.menubar-item
      {:on-click #(-> editor .chain .focus .undo .run)}
      [icon :undo]]
     [:button.menubar-item
      {:on-click #(-> editor .chain .focus .redo .run)}
      [icon :redo]]]))

(defn TipTap []
  (let
   [editor (useEditor
            #js {:extensions #js [StarterKit ;; basic formatting, e.g. bold, italic, lists, ...
                                  Highlight ;; Markdown Shortcuts during typing
                                  Typography ;; visual effects, e.g. converting (c) to a symbol
                                  Link ;; automatically make links
                                  Image ;; render images
                                  ]
                 :content "<img src=\"https://source.unsplash.com/8xznAGy4HcY/800x400\"/><p>Huhu</p><ul><li><p>Hello World!</p></li><li><p>asd</p></li></ul><p>was ist hier los?</p>"})]
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
