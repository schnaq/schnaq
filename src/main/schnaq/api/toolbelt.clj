(ns schnaq.api.toolbelt
  (:require [com.fulcrologic.guardrails.core :refer [>defn]]
            [ring.util.http-response :refer [not-found]]
            [schnaq.database.specs]))

(def response-error-body
  {:body :api.response/error-body})

(>defn build-error-body
  "Builds common error responses. Provide an `error-message` and it builds a
  body for an api response."
  [error-type error-message]
  [:api.response/error :api.response/message :ret :api.response/error-body]
  {:error error-type
   :message error-message})

(defn- build-not-found-response
  "Helper function to build unified not-found responses."
  [error-type error-message]
  (not-found
   (build-error-body error-type error-message)))

(def invalid-rights-message "You to not have enough permissions to access this data.")
(def invalid-share-hash-message "Invalid share-hash.")
(def invalid-access-code-message "Invalid access code.")
(def not-found-hash-invalid
  "Return 403 with invalid-share-hash message."
  (build-not-found-response :share-hash/invalid invalid-share-hash-message))
(def access-code-invalid
  "Return 403 with invalid-access-code message."
  (build-not-found-response :access-code/invalid invalid-access-code-message))

(defn get-doc
  "Look the docstring up in the meta-description of a function.
  Usage: `(get-doc #'ping)`"
  [fn]
  (:doc (meta fn)))
