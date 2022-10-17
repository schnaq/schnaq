(ns schnaq.interface.utils.markdown
  (:require ["react-markdown$default" :as ReactMarkdown]
            ["remark-gfm" :as gfm]
            [goog.string :as gstring]
            [reagent.core :as r]
            [schnaq.interface.components.motion :refer [zoom-image]]
            [schnaq.interface.utils.toolbelt :as tools]))

(defn Image [props]
  [:a {:href (:src props) :target :_blank}
   [:img (merge {:class "markdown-image"} props)]])

(defn Anchor
  "Custom anchor when it is rendered from markdown. Decodes url and truncates
  the resulting string."
  [props]
  [:a (-> props
          (update :href gstring/urlDecode)
          (update-in [:children 0] #(-> % gstring/urlDecode (tools/truncate-in-the-middle 16))))])

(defn as-markdown
  "Renders any string as markdown."
  [content]
  [:> ReactMarkdown
   {:remarkPlugins [gfm]
    :components {:a (fn [props]
                      (r/as-element [Anchor (dissoc (js->clj props :keywordize-keys true) :node)]))
                 :img (fn [props]
                        (r/as-element
                         [Image (dissoc (js->clj props :keywordize-keys true) :node)]))}}
   content])
