(ns schnaq.interface.components.lexical.plugins.autolink
  "Find patterns in the given text and convert them to url / email links."
  (:require ["@lexical/react/LexicalAutoLinkPlugin" :as AutoLinkPlugin]))

(def ^:private url-regex
  (re-pattern "^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"))

(def ^:private email-regex
  #"^[a-zA-Z0-9\._%-]+@[a-zA-Z0-9\.-]+\.[a-zA-Z]{2,10}$")

(def ^:private match-functions
  "Each function is executed on every character the user types, checked against
  the regular expressions and then converted, if there is a match."
  [(fn [text]
     (let [match (.exec url-regex text)]
       (and match
            #js
             {:index (.-index match),
              :length (.-length (nth match 0)),
              :text (nth match 0),
              :url (nth match 0)})))
   (fn [text]
     (let [match (.exec email-regex text)]
       (and match
            #js
             {:index (.-index match),
              :length (.-length (nth match 0)),
              :text (nth match 0),
              :url (str "mailto:" (nth match 0))})))])

;; -----------------------------------------------------------------------------

(defn autolink-plugin []
  [:> AutoLinkPlugin {:matchers match-functions}])
