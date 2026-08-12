(ns schnaq.interface.utils.markdown
  (:require ["react-markdown$default" :as ReactMarkdown]
            ["remark-gfm" :as gfm]
            [goog.string :as gstring]
            [reagent.core :as r]
            [schnaq.interface.utils.toolbelt :as tools]))

(defn Image [props]
  [:a {:href (:src props) :target :_blank}
   [:img (merge {:class "markdown-image"} props)]])

(defn- decode-and-truncate
  "Decode and shorten a link's label. Only plain strings can be decoded, a link
  may just as well wrap another element, e.g. an image."
  [label]
  (if (string? label)
    (-> label gstring/urlDecode (tools/truncate-in-the-middle 16))
    label))

(defn Anchor
  "Custom anchor when it is rendered from markdown. Decodes url and truncates
  the resulting string."
  [props]
  ;; `goog.string/urlDecode` calls `.replace` on its argument, so anything but a
  ;; string blows up. A link without a label, e.g. `[](https://schnaq.com)`, is
  ;; handed to us entirely without children.
  [:a (cond-> props
        (string? (:href props)) (update :href gstring/urlDecode)
        (seq (:children props)) (update-in [:children 0] decode-and-truncate))])

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
