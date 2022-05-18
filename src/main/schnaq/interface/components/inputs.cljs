(ns schnaq.interface.components.inputs
  "A number of schnaq-typical inputs."
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [com.fulcrologic.guardrails.core :refer [>defn => ?]]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.images :as image]))

(defn text
  "Build a text-input component."
  ([placeholder]
   [text placeholder nil])
  ([placeholder attrs]
   [:input.form-control.my-1
    (merge {:type "text"
            :placeholder placeholder}
           attrs)]))

(>defn image
  "Input field to upload image.
  Stores the image in a temporary field in the app-db, where it can than be
  used to transfer it to, e.g., the backend."
  ([label input-id temporary-image-location]
   [(s/or :string string? :component :re-frame/component) string? (s/coll-of keyword?) => :re-frame/component]
   [image label input-id temporary-image-location {}])
  ([label input-id temporary-image-location attrs]
   [(s/or :string string? :component :re-frame/component) string? (s/coll-of keyword?) map? => :re-frame/component]
   [:div
    [:label.form-label {:for input-id} label]
    [:input.form-control
     (merge
      {:type :file
       :id input-id
       :on-change #(image/store-temporary-image % temporary-image-location)
       :accept shared-config/allowed-mime-types}
      attrs)]
    [:small.text-muted (labels :input.file.image/allowed-types) ": "
     (str/join ", " (map #(second (str/split % #"/")) shared-config/allowed-mime-types))]]))

(>defn floating
  "Create a floating input field."
  [placeholder id attrs]
  [string? (s/or :keyword keyword? :string string?) (? map?) => :re-frame/component]
  [:div.form-floating
   [:input.form-control (merge {:id id :placeholder placeholder :name id} attrs)]
   [:label {:for id} placeholder]])

(>defn checkbox
  "Create a checkbox."
  [label id input-name attrs]
  [(s/or :string string? :component :re-frame/component) string? string? (? map?) => :re-frame/component]
  [:div.form-check
   [:input.form-check-input (merge {:id id :type :checkbox :name input-name} attrs)]
   [:label.form-check-label {:for id} label]])
