(ns schnaq.interface.utils.language
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.utils.localstorage :as localstorage]))

(defn locale []
  (oget js/navigator :language))

(defn init-language
  "Initializes the language of the client (if a preference is saved in localstorage)."
  []
  (when-let [language (keyword (localstorage/get-item :schnaq/language))]
    (rf/dispatch [:set-locale language])))

(defn set-language
  "Sets the language in the app and saves the selection for future reference.
  Use keywords as locales.

  e.g. `:en` for englisch or `:de` for german.
  Saves a stringified version without colon in the localstorage and the key to the config."
  [language]
  (localstorage/set-item! :schnaq/language (name language))
  (rf/dispatch [:set-locale language]))