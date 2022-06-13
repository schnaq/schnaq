(ns schnaq.interface.utils.markdown
  (:require ["react-markdown$default" :as ReactMarkdown]
            ["remark-gfm" :as gfm]))

(defn as-markdown
  "Renders any string as markdown."
  [content]
  [:> ReactMarkdown {:remarkPlugins [gfm]}
   content])
