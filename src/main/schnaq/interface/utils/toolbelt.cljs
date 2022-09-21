(ns schnaq.interface.utils.toolbelt
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as string]
            [com.fulcrologic.guardrails.core :refer [=> >defn ?]]
            [oops.core :refer [oget oget+ oset!]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.utils.tooltip :as tooltip]
            [spec-tools.core :as st]))

(>defn reset-form-fields!
  "Takes a collection of form input fields and resets their DOM representation
  to a specific value. If no default is provided, will always set to the blank
  string.

  Example usage in a form submit event:
  `(let [element (oget e [:target :elements :contact-name])]
     (reset-form-fields! [element]))`"
  ([fields]
   [any? :ret nil?]
   (reset-form-fields! fields ""))
  ([fields default]
   [any? string? :ret nil?]
   (run! #(oset! % [:value] default) fields)))

(>defn truncate-to-n-words
  "Truncate string to n words."
  [text n-words]
  [string? nat-int? :ret string?]
  (let [s (string/split text #" ")]
    (if (< n-words (count s))
      (string/join " " (conj (vec (take n-words s)) "…"))
      text)))

(>defn truncate-to-n-chars-string
  "Truncate a string to the first x chars and return it with … at the end as a string."
  [text char-count]
  [(? string?) nat-int? :ret (? string?)]
  (if (< char-count (count text))
    (apply str (concat (take char-count text) "…"))
    text))

(>defn truncate-in-the-middle
  "Truncate string in the middle. Keeps on the left and right side `char-count`
  characters."
  [text char-count]
  [(? string?) pos-int? => (? string?)]
  (when (pos-int? char-count)
    (if (< char-count (count text))
      (apply str (concat (take char-count text) "…" (take-last char-count text)))
      text)))

(>defn truncate-to-n-chars
  "Truncate a string to the first x chars and return it in a tooltiped span."
  [text char-count]
  [(? string?) nat-int? :ret (? (s/or :truncated :re-frame/component :normal string?))]
  (if (< char-count (count text))
    [tooltip/text
     text
     [:span (truncate-to-n-chars-string text char-count)]]
    text))

(>defn obfuscate-text
  "Reverse string and add css-class, which re-reverses the string."
  [text]
  [string? :ret :re-frame/component]
  [:span.obfuscate
   (apply str (reverse text))])

(defn get-selection-from-event
  "Helper for retrieving selected attribute after an event."
  [event]
  (let [options (oget event :target :options)
        selection-index (str (oget event :target :selectedIndex))]
    (oget+ options selection-index :value)))

(defn get-current-selection
  "Helper for retrieving current selection from any select element."
  [selection]
  (let [options (oget selection :options)
        selection-index (str (oget selection :selectedIndex))]
    (oget+ options selection-index :value)))

(defn current-display-name
  "Central fn for extracting current display-name from db."
  [db]
  (get-in db [:user :names :display] shared-config/default-anonymous-display-name))

(defn current-overview-link
  "Builds a href link to either the last selected hub or all visited schnaqs.
  When the user is not logged in ':routes.schnaqs/personal' will be selected."
  []
  (let [selected-hub @(rf/subscribe [:hub/selected])
        user-not-authenticated? (not @(rf/subscribe [:user/authenticated?]))]
    (if (or (nil? selected-hub) user-not-authenticated?)
      (navigation/href :routes.schnaqs/personal)
      (navigation/href :routes/hub {:keycloak-name selected-hub}))))

(defn current-overview-navigation-route
  "Builds :navigation event parameter to navigate to the last selected hub or all visited schnaqs.
   When the user is not logged in ':routes.schnaqs/personal' will be selected."
  []
  (let [selected-hub @(rf/subscribe [:hub/selected])
        user-not-authenticated? (not @(rf/subscribe [:user/authenticated?]))]
    (if (or (nil? selected-hub) user-not-authenticated?)
      [:navigation/navigate :routes.schnaqs/personal]
      [:navigation/navigate :routes/hub {:keycloak-name selected-hub}])))

(defn checked-values
  "Returns a list of all values that have been checked provided a list of Checkboxes from a form."
  [checkboxes]
  (map #(js/parseInt (oget % :value))
       (filter #(oget % :checked) checkboxes)))

(>defn ctrl-press?
  "Check for a ctrl + `key` combination in `event`. Don't use keyCode, as it is 
  deprecated."
  [event key]
  [any? string? => boolean?]
  (and (oget event :ctrlKey) (= key (oget event :key))))

(defn scroll-to-id
  [id]
  (let [clean-id (if (= \# (first id)) (subs id 1) id)
        element (.getElementById js/document clean-id)
        state (.-readyState js/document)]
    (when (and element (= state "complete"))
      (.scrollIntoView element))))

(defn clear-input
  "Clears an input field."
  [id]
  (when-let [element (js/document.getElementById id)]
    (set! (.-value element) "")))

(defn new-activation-focus
  "Resets the show-index and sets a new activation focus in a db."
  [db new-focus-id]
  (-> db
      (assoc-in [:schnaq :selected :discussion/activation-focus] new-focus-id)
      (assoc-in [:schnaq :activations :show-index] 0)))

(>defn filename-from-url
  "Takes a string-encoded url and extracts the filename from it."
  [url]
  [string? => (? string?)]
  (try
    (let [url (-> url js/URL. .-pathname (.split "/") .pop)]
      (not-empty url))
    (catch js/Object _e)))

(defn form->coerced-map
  "Takes all form elements and transforms them to a map. Coerces values by their
  specs. If there is no spec existent for the field's value, returns the value
  itself as a string."
  [form-elements]
  (->> form-elements
       (map (fn [field]
              (let [spec (keyword (oget field :name))]
                [(keyword (oget field :name))
                 (st/coerce spec (oget field :value) st/string-transformer)])))
       (remove #(= "" (second %)))
       (into {})))

(def session-storage-enabled?
  "Check if session storage is enabled in the user's client."
  (try js/sessionStorage
       true
       (catch js/Object _e false)))
