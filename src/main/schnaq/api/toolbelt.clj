(ns schnaq.api.toolbelt
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [ring.util.http-response :refer [not-found]]))

(s/def ::error keyword?)
(s/def ::message string?)
(s/def ::error-body
  (s/keys :req-un [::error ::message]))

(def response-error-body
  {:body ::error-body})

(>defn build-error-body
  "Builds common error responses. Provide an `error-message` and it builds a
  body for an api response."
  [error-type error-message]
  [::error ::message :ret ::error-body]
  {:error error-type
   :message error-message})

(def invalid-share-hash-message "Invalid share-hash.")
(def not-found-hash-invalid
  "Return 403 with invalid-share-hash message."
  (not-found
    (build-error-body :invalid-share-hash invalid-share-hash-message)))

(defn get-doc
  "Look the docstring up in the meta-description of a function.
  Usage: `(get-doc #'ping)`"
  [fn]
  (:doc (meta fn)))