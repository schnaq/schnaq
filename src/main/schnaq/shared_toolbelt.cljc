(ns schnaq.shared-toolbelt
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [com.fulcrologic.guardrails.core :refer [=> >defn >defn- ?]]
            [schnaq.config.shared :as shared-config])
  #?(:clj (:import (java.lang Character))))

(>defn slugify
  "Make a slug from a string. For example:
  `(slugify \"This is sparta\") => this-is-sparta`"
  [string]
  [string? => string?]
  (let [reduced-string (-> string
                           (str/replace #"/|\." " ")
                           str/trim
                           (str/split #"\s"))
        tokens (map str/lower-case reduced-string)]
    (str/join "-" (take (count tokens) tokens))))

(>defn remove-nil-values-from-map
  "Removes all entries from a map that have a value of nil or empty string."
  [data]
  [associative? :ret associative?]
  (into {} (remove #(or (nil? (second %))
                        (when (string? (second %))
                          (str/blank? (second %))))
                   data)))

(>defn normalize
  "Normalize a collection of maps to a map with the key as its identity, e.g. the
  db/id, and the value is the original map."
  [ident coll]
  [keyword? (s/or :maps (s/coll-of map?) :nil nil?) => map?]
  (into {} (map (juxt ident identity) coll)))

(defn- alphanumeric?
  "Checks whether some char is a Letter or a Digit."
  [char-to-test]
  #?(:clj (or
           (Character/isLetter ^char char-to-test)
           (Character/isDigit ^char char-to-test))
     :cljs (.test (new js/RegExp "^[a-z0-9]+$") char-to-test)))

(defn tokenize-string
  "Tokenizes a string into single tokens for the purpose of searching."
  [content]
  (->> (str/split (str/lower-case content) #"\s")
       (remove str/blank?)
       ;; Remove punctuation when generating token
       (map #(cond
               (not (alphanumeric? (first %))) (subs % 1)
               (not (alphanumeric? (last %))) (subs % 0 (dec (count %)))
               :else %))))

(def select-values (comp vals select-keys))

(>defn namespaced-keyword->string
  "Takes a namespaced keyword and returns it containing the namespace.
  Example: `(namespaced-keyword->string :user.registered/keycloak-id)
  => \"user.registered/keycloak-id\"`"
  [namespaced-keyword]
  [(? keyword?) => (? string?)]
  (when namespaced-keyword
    (let [[kw-ns kw] ((juxt namespace name) namespaced-keyword)]
      (if kw-ns
        (str/join "/" [kw-ns kw])
        (str kw)))))

(defn deep-merge-with
  "Like merge-with, but merges maps recursively, applying the given fn
  only when there's a non-map at a particular level.
   
  Taken from https://clojuredocs.org/clojure.core/merge-with#example-5b80843ae4b00ac801ed9e74"
  [f & maps]
  (apply
   (fn m [& maps]
     (if (every? map? maps)
       (apply merge-with m maps)
       (apply f maps)))
   maps))

;; -----------------------------------------------------------------------------

(>defn- intersection?
  "Check if the two sets have an intersection."
  [set1 set2]
  [set? set? => boolean?]
  (not (nil? (seq (set/intersection set1 set2)))))

(>defn beta-tester?
  "Check if a user has one of the valid beta tester roles."
  [roles]
  [(? :user.registered/roles) => (? boolean?)]
  (when roles
    (intersection? roles shared-config/beta-tester-roles)))

(>defn enterprise-user?
  "Check if a user has one of the valid enterprise-roles."
  [roles]
  [(? :user.registered/roles) => (? boolean?)]
  (when roles
    (intersection? roles shared-config/enterprise-roles)))

(>defn pro-user?
  "Check if a user has one of the valid pro-roles."
  [roles]
  [(? :user.registered/roles) => (? boolean?)]
  (when roles
    (intersection? roles shared-config/pro-roles)))

(>defn admin?
  "Check if a user has one of the valid admin-roles."
  [roles]
  [(? :user.registered/roles) => (? boolean?)]
  (when roles
    (intersection? roles shared-config/admin-roles)))

(>defn analytics-admin?
  "Check if a user has one of the valid admin-roles."
  [roles]
  [(? :user.registered/roles) => (? boolean?)]
  (when roles
    (intersection? roles shared-config/analytics-roles)))
