(ns meetly.interface.utils.localstorage
  (:require [ghostwheel.core :refer [>defn >defn- ?]]
            [cljs.spec.alpha :as s]
            [clojure.string :as string]))

(>defn- keyword->string
  "Takes (namespaced) keywords and creates a string. Optionally is prefixed with
  the keyword's namespace."
  [k]
  [keyword? :ret (? string?)]
  (if-let [keyword-namespace (namespace k)]
    (string/join "/" [keyword-namespace (name k)])
    (str (name k))))

(>defn- stringify
  "Stringifies a symbol or keyword. Tosses the namespace."
  [val]
  [(s/or keyword? symbol? string?) :ret string?]
  (if (keyword? val)
    (keyword->string val)
    (str val)))

(>defn set-item!
  "Set `key` in browser's localStorage to `val`."
  [key val]
  [keyword? any? :ret nil?]
  (.setItem (.-localStorage js/window) (stringify key) val))

(>defn get-item
  "Returns value of `key' from browser's localStorage."
  [key]
  [keyword? :ret any?]
  (.getItem (.-localStorage js/window) (stringify key)))

(>defn remove-item!
  "Remove the browser's localStorage value for the given `key`"
  [key]
  [keyword? :ret nil?]
  (.removeItem (.-localStorage js/window) (stringify key)))