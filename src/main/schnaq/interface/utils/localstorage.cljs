(ns schnaq.interface.utils.localstorage
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn >defn- ?]]))

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

(>defn localstorage->map
  "Dump complete content of localstorage into a map. Removes debug data from
  re-frame-10x."
  []
  [:ret map?]
  (into {}
        (for [i (range (.-length (.-localStorage js/window)))
              :let [k (.key (.-localStorage js/window) i)
                    v (get-item k)]
              :when (not (string/starts-with? k "day8.re-frame-10x"))]
          [k v])))

(>defn clear!
  "Delete all information stored in the LocalStorage."
  []
  [:ret nil?]
  (.clear (.-localStorage js/window)))

;; #### Hashmap Storage Helper ####

(def ^:private tuple-separator ",")
(def ^:private tuple-data #"\[(.*?)\]")
(def ^:private hash-separator " ")

(defn parse-hash-map-string
  "Read previously visited meetings from localstorage. E.g (ls/get-item :schnaqs/admin-access)
  The string must obey the following convention '[share-1 edit-1],[share-2 edit-2]'"
  [hash-map-string]
  (let [hashes (remove empty? (string/split hash-map-string (re-pattern tuple-separator)))
        hashes-unbox (map (fn [tuple] (second (re-find tuple-data tuple))) hashes)
        hashes-vector (map (fn [tuple] (string/split tuple (re-pattern hash-separator))) hashes-unbox)
        cleaned-vector (remove #(nil? (second %)) hashes-vector)
        hashes-map (if (empty? cleaned-vector) {} (into {} cleaned-vector))]
    hashes-map))

(defn- add-key-value-to-local-hashmap
  [hash-map-string key value]
  (let [parsed-hash-map (parse-hash-map-string hash-map-string)
        new-hash-map (conj parsed-hash-map {(str key) (str value)})]
    ;; check if key is already in hash map
    (if-not (some #{key} parsed-hash-map)
      new-hash-map
      parsed-hash-map)))

(defn add-hash-map-and-build-map-from-localstorage
  "Build and insert hashmap into an existing local storage hashmap."
  [hash-map local-storage-key]
  (let [local-hashes-as-string (get-item local-storage-key)
        local-hash-map (parse-hash-map-string local-hashes-as-string)
        new-hash-map (merge local-hash-map hash-map)
        hashes-tuple (map (fn [[val key]] (str "[" val " " key "]")) (seq new-hash-map))
        hashes-as-string (string/join "," hashes-tuple)]
    hashes-as-string))

(defn add-key-value-and-build-map-from-localstorage
  "Build key value pair for inserting into local storage hashmap.
  Does not override the key if it is present"
  [key value local-storage-key]
  ;; PARTIALLY DEPRECATED: Remove the meeting part after 2020-08-05
  (let [local-hashes (get-item local-storage-key)
        combined-hashes (if (= :schnaqs/admin-access local-storage-key)
                          (if-let [old-admin-access (get-item :meetings/admin-access)]
                            (str local-hashes "," old-admin-access)
                            local-hashes)
                          local-hashes)
        new-hashes (add-key-value-to-local-hashmap combined-hashes key value)
        hashes-tuple (map (fn [[val key]] (str "[" val " " key "]")) (seq new-hashes))
        hashes-as-string (string/join "," hashes-tuple)]
    (when (= :schnaqs/admin-access local-storage-key)
      (remove-item! :meetings/admin-access))
    hashes-as-string))

;; ### Set Storage Helper ###

(defn parse-string-as-set
  "Parse a string of a set to clojure set. Must obey convention 'item1 item2'"
  [set-string]
  (let [items (remove empty? (string/split set-string (re-pattern hash-separator)))]
    (set items)))

(defn- parse-set-as-string
  "Parse a set to a string. Will obey convention 'item1 item2'"
  [set-data]
  (let [items (map str set-data)
        set-items (set items)
        set-as-string (string/join hash-separator set-items)]
    set-as-string))

(defn add-to-and-build-set-from-local-storage
  "Add a value to a set ready for use as local storage data.
  Get localstorage value of 'key-entry', parse it as set, add 'item' and parse it as string.
  The resulting string will obey the convention 'item1 item2 item3'"
  [key-entry item]
  (let [value-entry (get-item key-entry)
        string-as-set (parse-string-as-set value-entry)
        new-set (conj string-as-set item)
        new-set-as-string (parse-set-as-string new-set)]
    new-set-as-string))
