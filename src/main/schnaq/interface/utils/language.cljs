(ns schnaq.interface.utils.language
  (:require [hodgepodge.core :refer [local-storage]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :as config]
            [schnaq.interface.utils.localstorage :as localstorage]))

(defn locale []
  (oget js/navigator :language))

(defn init-language
  "Initializes the language of the client (if a preference is saved in localstorage)."
  []
  (when-let [language (:schnaq/language local-storage)]
    (reset! config/user-language language)
    (rf/dispatch [:set-locale language])))

(defn set-language
  "Sets the language in the app and saves the selection for future reference.
  Use keywords as locales.

  e.g. `:en` for english or `:de` for german.
  Saves the keyword in the localstorage and sets the key to the config."
  [language]
  (localstorage/set-item! :schnaq/language language)
  (reset! config/user-language language)
  (rf/dispatch [:set-locale language]))