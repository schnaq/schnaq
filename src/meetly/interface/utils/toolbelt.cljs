(ns meetly.interface.utils.toolbelt
  (:require [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [goog.dom :as gdom]
            [goog.dom.classes :as gclasses]
            [meetly.interface.config :refer [config]]
            [oops.core :refer [oset!]]))

(>defn add-or-remove-class
  "Add or delete a certain class, depending on the evaluation of the predicate."
  [element-id predicate? class]
  [string? boolean? string? :ret boolean?]
  (let [element (gdom/getElement element-id)]
    (if predicate?
      (gclasses/add element class)
      (gclasses/remove element class))))


(>defn production?
  "Checks the configuration for the current environment."
  []
  [:ret boolean?]
  (= "production" (:environment config)))

(>defn reset-form-fields!
  "Takes a collection of form input fields and resets their DOM representation
  to a specific value. If no default is provided, will always set to the blank
  string.

  Example usage in a form submit event:
  `(let [element (oget e [:target :elements :contact-name])]
     (reset-form-fields! [element]))`"
  ([fields]
   [(s/coll-of any?) :ret nil?]
   (reset-form-fields! fields ""))
  ([fields default]
   [(s/coll-of any?) string? :ret nil?]
   (run! #(oset! % [:value] default) fields)))

