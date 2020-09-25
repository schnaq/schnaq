(ns schnaq.interface.utils.markdown-parser
  (:require [markdown.core :refer [md->html]]))

(defn markdown-to-html [markdown]
  [:div
   {:dangerouslySetInnerHTML {:__html (md->html markdown)}}])