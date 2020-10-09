(ns schnaq.interface.utils.markdown-parser
  (:require [goog.string :as gstring]
            [ghostwheel.core :refer [>defn-]]
            [markdown.core :refer [md->html]]))

(>defn- parse-special-forms
  "Takes unrendered markdown and enriches special forms like timeboxes with markdown-chars."
  [markdown]
  [string? :ret string?]
  (gstring/replaceAll markdown "Dauer: " "![Uhr Symbol](/imgs/clock.png) "))

(defn markdown-to-html [markdown]
  [:div
   {:dangerouslySetInnerHTML {:__html (md->html (parse-special-forms markdown))}}])