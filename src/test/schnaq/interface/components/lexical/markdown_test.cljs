(ns schnaq.interface.components.lexical.markdown-test
  (:require ["@lexical/markdown" :refer [$convertToMarkdownString]]
            ["lexical" :refer [$createParagraphNode $createTextNode $getRoot
                               createEditor]]
            [clojure.string :as string]
            [clojure.test :refer [deftest is testing]]
            [schnaq.interface.components.lexical.nodes.excalidraw :refer [$create-excalidraw-node ExcalidrawNode]]
            [schnaq.interface.components.lexical.nodes.image :refer [ImageNode]]
            [schnaq.interface.components.lexical.nodes.video :refer [VideoNode]]
            [schnaq.interface.components.lexical.plugins.markdown :refer [schnaq-transformers]]))

(defn- ->markdown
  "Build an editor, populate its root via `populate!` and export it to markdown."
  [populate!]
  (let [editor (createEditor #js {:nodes #js [ExcalidrawNode ImageNode VideoNode]
                                  :onError #(throw %)})]
    (.update editor #(populate! ($getRoot)) #js {:discrete true})
    (.read (.getEditorState editor)
           #($convertToMarkdownString schnaq-transformers))))

(defn- append-drawing!
  "Append a drawing, wrapped in paragraphs like the excalidraw plugin does."
  [root url]
  (let [node ($create-excalidraw-node)]
    (.setData node "[{\"type\":\"rectangle\"}]")
    (when url (.setUrl node url))
    (.append root ($createParagraphNode))
    (.append root node)
    (.append root ($createParagraphNode))))

(deftest excalidraw-export-test
  (testing "A drawing exports as a markdown image pointing at its uploaded png."
    (let [markdown (->markdown #(append-drawing! % "https://example.com/drawing.png"))]
      (is (string/includes? markdown "![Excalidraw drawing](https://example.com/drawing.png)"))))
  (testing "A drawing whose png is not uploaded yet exports nothing, so that a
  statement consisting only of that drawing cannot be submitted."
    (let [markdown (->markdown #(append-drawing! % nil))]
      (is (string/blank? markdown))))
  (testing "Text next to a drawing is preserved."
    (let [markdown (->markdown
                    (fn [root]
                      (.append root (doto ($createParagraphNode)
                                      (.append ($createTextNode "look at this"))))
                      (append-drawing! root "https://example.com/drawing.png")))]
      (is (string/includes? markdown "look at this"))
      (is (string/includes? markdown "![Excalidraw drawing](https://example.com/drawing.png)")))))
