(ns schnaq.interface.utils.toolbelt
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as string]
            [com.fulcrologic.guardrails.core :refer [>defn ? =>]]
            [goog.dom :as gdom]
            [goog.dom.dataset :as dataset]
            [oops.core :refer [oset! oget oget+]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.specs :as specs]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.utils.tooltip :as tooltip]))

(defn height-to-scrollheight!
  "Get current scroll height and set the height of the element accordingly.
  Used for textareas to grow with input."
  [element]
  (oset! element [:style :height] "0.5rem")
  (oset! element [:style :height] (str (+ 2 (oget element [:scrollHeight])) "px")))

(defn- reset-form-height!
  "Reset all formfields with dynamicHeights.
  Textareas with the attribute ':data-dynamic-height' will reset their height to one line.
  E.g. after submitting of a form all dynamic height fields will be reset to one line."
  [fields]
  (doseq [field fields]
    ;; ? : nil if not present
    (when (oget field [:dataset :?dynamicHeight])
      (height-to-scrollheight! field))))

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
   (run! #(oset! % [:value] default) fields)
   (reset-form-height! fields)))

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

(defn update-statement-in-list
  "Updates the content of a statement in a collection."
  [coll new-statement]
  (map #(if (= (:db/id new-statement) (:db/id %)) new-statement %) coll))

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

(defn ctrl-press?
  "Check for a ctrl + `keyCode` combination in `event`."
  [event keyCode]
  (and (.-ctrlKey event) (= keyCode (.-keyCode event))))

(defn scroll-to-id
  [id]
  (let [clean-id (if (= \# (first id)) (subs id 1) id)
        element (.getElementById js/document clean-id)
        state (.-readyState js/document)]
    (when (and element (= state "complete"))
      (.scrollIntoView element))))

(defn data-attribute
  "Reads a dataset attribute from some element by id."
  [element-id data-key]
  (-> element-id
      gdom/getElement
      (dataset/get data-key)))

(defn clear-input
  "Clears an input field."
  [id]
  (when-let [element (js/document.getElementById id)]
    (set! (.-value element) "")))

(>defn convert-premises
  "Take normalized premises and convert them to a collection."
  [db]
  [map? => (s/coll-of ::specs/statement)]
  (if-let [premises (get-in db [:discussion :premises :current])]
    (map second premises)
    '()))

(>defn set-wordcloud-in-current-schnaq
  "Check in app db at selected schnaq whether to display a word cloud."
  [db]
  [map? => map?]
  (let [show-wordcloud?
        (some
         #(= % :discussion.visible.entities/wordcloud)
         (get-in db [:schnaq :selected :discussion.visible/entities]))]
    (assoc-in db [:schnaq :current :display-wordcloud?] show-wordcloud?)))
