(ns schnaq.interface.components.lexical.plugins.autolink
  "Find patterns in the given text and convert them to url / email links."
  (:require ["@lexical/react/LexicalAutoLinkPlugin" :as AutoLinkPlugin]))

(def ^:private url-regex
  (re-pattern "^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"))

(def ^:private url-starting-with-www
  #"^[\w\.]+\.\w+\.\w{2,10}$")

(def ^:private url-without-www
  #"\w+\.\w{2,10}$")

(def ^:private email-regex
  #"^[a-zA-Z0-9\._%-]+@[a-zA-Z0-9\.-]+\.[a-zA-Z]{2,10}$")

(def ^:private match-functions
  "Each function is executed on every character the user types, checked against
  the regular expressions and then converted, if there is a match."
  [(fn [text]
     (when-let [match (or
                       (.exec url-regex text)
                       (.exec url-starting-with-www text)
                       (.exec url-without-www text))]
       #js {:index (.-index match)
            :length (.-length (nth match 0))
            :text (nth match 0)
            :url (nth match 0)}))
   (fn [text]
     (when-let [match (.exec email-regex text)]
       #js {:index (.-index match)
            :length (.-length (nth match 0))
            :text (nth match 0)
            :url (str "mailto:" (nth match 0))}))])

;; -----------------------------------------------------------------------------

(defn autolink-plugin []
  [:> AutoLinkPlugin {:matchers match-functions}])
