(ns schnaq.interface.utils.localstorage
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn >defn- ?]]
            [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]))

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

(defn assoc-item!
  "Sets `val` in a serialized form into local-storage under the serialized key `key`"
  [key val]
  (assoc! local-storage key val))

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

(defn add-key-value-and-build-map-from-localstorage
  "Build key value pair for inserting into local storage hashmap.
  Does not override the key if it is present"
  [local-storage-key]
  ;; PARTIALLY DEPRECATED: Remove the meeting part after 2021-08-05
  (let [local-hashes (get-item local-storage-key)
        combined-hashes (if (= :schnaqs/admin-access local-storage-key)
                          (if-let [old-admin-access (get-item :meetings/admin-access)]
                            (str local-hashes "," old-admin-access)
                            local-hashes)
                          local-hashes)]
    (when (= :schnaqs/admin-access local-storage-key)
      (remove-item! :meetings/admin-access))
    (parse-hash-map-string combined-hashes)))

;; ### Set Storage Helper ###
(defn parse-string-as-set
  "Parse a string of a set to clojure set. Must obey convention 'item1 item2'"
  [set-string]
  (let [items (remove empty? (string/split set-string (re-pattern hash-separator)))]
    (set items)))

(rf/reg-fx
  ;; Associates a value into local-storage. Can be retrieved as EDN via get or get-in.
  :localstorage/assoc
  (fn [[key value]]
    (assoc-item! key value)))

(rf/reg-fx
  :localstorage/dissoc
  (fn [key]
    (dissoc! local-storage key)))