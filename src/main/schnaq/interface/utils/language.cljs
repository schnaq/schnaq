(ns schnaq.interface.utils.language
  (:require [clojure.string :as str]
            [hodgepodge.core :refer [local-storage]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :as config]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.utils.localstorage :as localstorage]))

(defn locale []
  (oget js/navigator :language))

(defn default-language
  "Returns the keyword for the default browser language.
  If the default language is not supported by schnaq, english is chosen by default."
  []
  (-> (jq/browser-language)
      (str/split "-")
      first
      keyword
      #{:en :de}
      (or :en)))

(defn init-language
  "Initializes the language of the client (if a preference is saved in localstorage)."
  []
  (let [language (or (:schnaq/language local-storage) (default-language))]
    (reset! config/user-language language)
    (rf/dispatch [:set-locale language])))

(defn set-language
  "Sets the language in the app and saves the selection for future reference.
  Use keywords as locales.

  e.g. `:en` for english or `:de` for german.
  Saves the keyword in the localstorage and sets the key to the config."
  [language]
  (localstorage/assoc-item! :schnaq/language language)
  (reset! config/user-language language)
  (rf/dispatch [:set-locale language]))