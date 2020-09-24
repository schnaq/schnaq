(ns schnaq.interface.utils.markdown-parser
  (:require [clojure.string :as string]
            [markdown.core :refer [md->html]]))

(defn escape-images [text state]
  [(string/replace text #"(!\[.*?\]\()(.+?)(\))" "") state])

(defn escape-html
  "Change special characters into HTML character entities."
  [text state]
  [(if-not (or (:code state) (:codeblock state))
     (string/escape
       text
       {\& "&amp;"
        \< "&lt;"
        \> "&gt;"
        \" "&quot;"
        \' "&#39;"})
     text) state])


(defn markdown-to-html [markdown]
  [:div
   {:dangerouslySetInnerHTML {:__html (md->html markdown)}}])