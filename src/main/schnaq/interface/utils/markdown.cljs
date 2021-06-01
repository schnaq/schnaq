(ns schnaq.interface.utils.markdown
  (:require ["react-markdown" :as ReactMarkdown]
            ["remark-gfm" :as gfm]))

(defn as-markdown
  "Renders any string as markdown."
  [content]
  [:> ReactMarkdown {:children content
                     :remarkPlugins [gfm]}])