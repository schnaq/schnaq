(ns schnaq.interface.utils.language
  (:require [clojure.string :as str]
            [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [schnaq.interface.config :as config]))

(defn- browser-language
  "Returns the user's browser language"
  []
  (or (.-language js/navigator)
      (.-userLanguage js/navigator)))

(defn- default-language
  "Returns the keyword for the default browser language.
  If the default language is not supported by schnaq, english is chosen by default."
  []
  (-> (browser-language)
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
  (assoc! local-storage :schnaq/language language)
  (reset! config/user-language language)
  (rf/dispatch [:set-locale language]))

(rf/reg-sub
 :current-locale
 (fn [db _]
   (get db :locale :en)))

(rf/reg-sub
 :current-language
 :<- [:current-locale]
 (fn [locale _]
   (case locale
     :de "Deutsch"
     :en "English")))

(rf/reg-fx
 ;; Changes the HTML lang attribute accordingly.
 :change-document-lang
 (fn [lang-short]
   (let [locale-string (case lang-short
                         :de "de"
                         :en "en"
                         "en")]
     (.setAttribute (.-documentElement js/document) "lang" locale-string))))

(rf/reg-event-fx
 :set-locale
 (fn [{:keys [db]} [_ locale]]
   {:db (assoc db :locale locale)
    :fx [[:change-document-lang locale]]}))

(rf/reg-fx
 ;; Changes more than just the document locale, like changing the key in config and writing it to localstorage.
 ;; (But includes execution of set-locale)
 :switch-language
 (fn [locale]
   (set-language locale)))

(rf/reg-event-fx
 :language/switch
 (fn [_ [_ locale]]
   {:fx [[:switch-language locale]]}))
