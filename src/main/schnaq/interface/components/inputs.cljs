(ns schnaq.interface.components.inputs
  "A number of schnaq-typical inputs."
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [com.fulcrologic.guardrails.core :refer [>defn => ?]]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.files :as files]))

(defn text
  "Build a text-input component."
  ([placeholder]
   [text placeholder nil])
  ([placeholder attrs]
   [:input.form-control.my-1
    (merge {:type "text"
            :placeholder placeholder}
           attrs)]))

(>defn file
  "Input field to upload a file.
  Allowed files can be controlled by providing an `:accept` attribute
  (see `image`).
  Stores the file in a temporary field in the app-db, where it can than be
  used to transfer it to, e.g., the backend."
  ([label input-id temporary-file-location]
   [(s/or :string string? :component :re-frame/component) string? (s/coll-of keyword?) => :re-frame/component]
   [file label input-id temporary-file-location {}])
  ([label input-id temporary-file-location attrs]
   [(s/or :string string? :component :re-frame/component) string? (s/coll-of keyword?) map? => :re-frame/component]
   [:div
    [:label.form-label {:for input-id} label]
    [:input.form-control
     (merge
      {:type :file
       :id input-id
       :on-change #(files/store-temporary-file % temporary-file-location)}
      attrs)]
    (when-let [mime-types (:accept attrs)]
      [:small.text-muted (labels :file/allowed-types) ": "
       (str/join ", " (map #(second (str/split % #"/")) mime-types))])]))

(>defn image
  "Input field to upload image.
  Pre configures the allowed mime types for images. Same as `file`."
  ([label input-id temporary-image-location]
   [(s/or :string string? :component :re-frame/component) string? (s/coll-of keyword?) => :re-frame/component]
   [file label input-id temporary-image-location {:accept shared-config/allowed-mime-types-images}])
  ([label input-id temporary-image-location attrs]
   [(s/or :string string? :component :re-frame/component) string? (s/coll-of keyword?) map? => :re-frame/component]
   [file label input-id temporary-image-location (merge {:accept shared-config/allowed-mime-types-images} attrs)]))

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
