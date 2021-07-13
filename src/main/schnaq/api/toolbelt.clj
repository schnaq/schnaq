(ns schnaq.api.toolbelt
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]))

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

(defn get-doc
  "Look the docstring up in the meta-description of a function.
  Usage: `(get-doc #'ping)`"
  [fn]
  (:doc (meta fn)))