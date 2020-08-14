(ns meetly.meeting.interface.utils.toolbelt
  (:require [ghostwheel.core :refer [>defn]]
            [goog.dom :as gdom]
            [goog.dom.classes :as gclasses]
            [meetly.meeting.interface.config :refer [config]]))

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