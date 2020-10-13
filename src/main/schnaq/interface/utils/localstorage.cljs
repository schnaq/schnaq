(ns schnaq.interface.utils.localstorage
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
  "Stringifies a symbol or keyword."
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

;; #### Hashmap Storage Helper ####

(def ^:private tuple-separator ",")
(def ^:private tuple-data #"\[(.*?)\]")
(def ^:private hash-separator " ")

(defn parse-hash-map-string
  "Read previously visited meetings from localstorage. E.g (ls/get-item :meetings/admin-access)
  The string must obey the following convention '[share-1 edit-1],[share-2 edit-2]'"
  [hash-map-string]
  (let [hashes (remove empty? (string/split hash-map-string (re-pattern tuple-separator)))
        hashes-unbox (map (fn [tuple] (second (re-find tuple-data tuple))) hashes)
        hashes-vector (map (fn [tuple] (string/split tuple (re-pattern hash-separator))) hashes-unbox)
        hashes-map (into {} hashes-vector)]
    hashes-map))

(defn- add-key-value-to-local-hashmap [hash-map-string key value]
  (let [hash-map (parse-hash-map-string hash-map-string)
        new-hash-map (conj hash-map {(str key) (str value)})]
    ;; check if key is already in hash map
    (if-not (some #{key} hash-map)
      new-hash-map
      hash-map)))

(conj {:a 1 :b 2} {:b 3})

(defn build-hash-map-from-localstorage
  "Build and insert hashmap into an existing local storage hashmap."
  ([hash-map local-storage-key]
   (let [local-hashes-as-string (get-item local-storage-key)
         local-hash-map (parse-hash-map-string local-hashes-as-string)
         new-hash-map (conj local-hash-map hash-map)
         hashes-tuple (map (fn [[val key]] (str "[" val " " key "]")) (seq new-hash-map))
         hashes-as-string (string/join "," hashes-tuple)]
     hashes-as-string))

  ([key value local-storage-key]
   "build and insert key value pair into an existing local storage hashmap.
   Does not override the key if it is present"
   (let [local-hashes (get-item local-storage-key)
         new-hashes (add-key-value-to-local-hashmap local-hashes key value)
         hashes-tuple (map (fn [[val key]] (str "[" val " " key "]")) (seq new-hashes))
         hashes-as-string (string/join "," hashes-tuple)]
     hashes-as-string)))
