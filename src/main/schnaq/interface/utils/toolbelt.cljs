(ns schnaq.interface.utils.toolbelt
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn]]
            [oops.core :refer [oset! oget]]
            [schnaq.interface.utils.tooltip :as tooltip]))

(defn height-to-scrollheight!
  "Get current scroll height and set the height of the element accordingly.
  Used for textareas to grow with input."
  [element]
  (oset! element [:style :height] "0.5rem")
  (oset! element [:style :height] (str (inc (oget element [:scrollHeight])) "px")))

(defn- reset-form-height!
  "Reset all formfields with dynamicHeights.
  Textareas with the attribute ':data-dynamic-height' will reset their height to one line.
  E.g. after submitting of a form all dynamic height fields will be reset to one line."
  [fields]
  (doseq [field fields]
    ;; ? : nil if not present
    (when (oget field [:dataset :?dynamicHeight])
      (height-to-scrollheight! field))))

(>defn reset-form-fields!
  "Takes a collection of form input fields and resets their DOM representation
  to a specific value. If no default is provided, will always set to the blank
  string.

  Example usage in a form submit event:
  `(let [element (oget e [:target :elements :contact-name])]
     (reset-form-fields! [element]))`"
  ([fields]
   [(s/coll-of any?) :ret nil?]
   (reset-form-fields! fields ""))
  ([fields default]
   [(s/coll-of any?) string? :ret nil?]
   (run! #(oset! % [:value] default) fields)
   (reset-form-height! fields)))

(defn desktop-mobile-switch
  "Displays desktop view on medium+ devices and displays mobile view on small- devices"
  [desktop-view mobile-view]
  [:<>
   [:div.d-none.d-md-block desktop-view]
   [:div.d-md-none mobile-view]])

(>defn truncate-to-n-words
  "Truncate string to n words."
  [text n-words]
  [string? nat-int? :ret string?]
  (let [s (string/split text #" ")]
    (if (< n-words (count s))
      (string/join " " (conj (vec (take n-words s)) "…"))
      text)))

(>defn truncate-to-n-chars
  "Truncate a string to the first x chars and return it in a tooltiped span."
  [text char-count]
  [string? nat-int? :ret string?]
  (if (< char-count (count text))
    [tooltip/text
     text
     (apply str (concat (take char-count text) "…"))]
    text))

(defn obfuscate-mail
  "Hide real mail address."
  [mail]
  [:span.obfuscate
   (apply str (reverse mail))])

(defn update-statement-in-list
  "Updates the content of a statement in a collection."
  [coll new-statement]
  (map #(if (= (:db/id new-statement) (:db/id %)) new-statement %) coll))