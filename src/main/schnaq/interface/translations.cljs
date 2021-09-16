(ns schnaq.interface.translations
  (:require [schnaq.interface.config :refer [user-language]]
            [schnaq.interface.translations.english :as english]
            [schnaq.interface.translations.german :as german]
            [schnaq.interface.translations.polish :as polish]
            [taoensso.tempura :refer [tr]]))

(def ^:private translations
  {:en english/labels
   :de german/labels
   :pl polish/labels})

(defn labels
  "Get a localized resource for the requested key. Returns either a string or a hiccup
  element. Optionally tempura parameters can be passed."
  [resource-key & params]
  (let [default-lang :de]
    (tr {:dict translations} [@user-language default-lang] [resource-key] (vec params))))
