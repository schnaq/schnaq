(ns schnaq.interface.utils.markdown
  (:require ["react-markdown$default" :as ReactMarkdown]
            ["remark-gfm" :as gfm]
            [reagent.core :as r]))

(defn Image [props]
  [:img (merge {:class "markdown-image"}
               props)])

(defn as-markdown
  "Renders any string as markdown."
  [content]
  [:> ReactMarkdown
   {:remarkPlugins [gfm]
    :components {:img (fn [props]
                        (r/as-element
                         [Image (dissoc (js->clj props) "node")]))}}
   content])
