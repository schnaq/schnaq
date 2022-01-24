(ns schnaq.interface.components.wordcloud
  (:require ["react-wordcloud" :default ReactWordcloud]
            ["stopwords-de" :as stopwords-de]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [com.fulcrologic.guardrails.core :refer [>defn-]]
            [re-frame.core :as rf]
            [schnaq.database.specs :as specs]
            [schnaq.interface.components.colors :refer [colors]]
            [schnaq.interface.components.preview :as preview]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.loading :refer [spinner-icon]]))

(s/def ::text ::specs/non-blank-string)
(s/def ::value number?)
(s/def ::word
  (s/keys :req-un [::text ::value]))

(>defn- extract-link-text-from-md
  "Check if text contains a markdown link. If yes, then return the link's name,
  else return the word."
  [word]
  [string? :ret string?]
  (if-let [link (re-seq #"\[(.*)\](.*)" word)]
    (-> link first second)
    word))

(>defn- remove-md-links
  "Remove all occurrences of markdown links."
  [s]
  [string? :ret string?]
  (str/replace s #"\[([\w|\s]*)\]\(\S*\)" "$1"))

(>defn- convert-fulltext
  "Convert a fulltext to the format our wordcloud requires."
  [fulltext]
  [string? :ret (s/coll-of ::word)]
  (let [replaced (-> fulltext remove-md-links (str/replace #"\.|!|,|\?|;|-|–" ""))]
    (for [[word total] (->> (str/split replaced #"\s")
                            (remove #((set stopwords-de) %))
                            (map extract-link-text-from-md)
                            (remove empty?)
                            frequencies)]
      {:text word
       :value total})))

(def ^:private options
  {:colors (vals (dissoc colors :white))
   :enableTooltip true
   :deterministic true
   :fontFamily "Poppins"
   :fontSizes [20 60]
   :fontStyle "normal"
   :fontWeight "normal"
   :padding 1
   :rotations 3
   :rotationAngles [0 90]
   :scale "sqrt"
   :spiral "archimedean"
   :transitionDuration 1000})

;; -----------------------------------------------------------------------------

(defn wordcloud
  "Create a wordcloud based on the data in the db."
  []
  (if @(rf/subscribe [:user/pro-user?])
    (if-let [words @(rf/subscribe [:wordcloud/words])]
      [:> ReactWordcloud {:words words :options options}]
      [:div.text-center.py-3 [spinner-icon]])
    [preview/preview-image :preview/wordcloud]))

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :wordcloud/for-current-discussion
 (fn [{:keys [db]}]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:fx [(http/xhrio-request
            db :get "/export/fulltext"
            [:wordcloud/store-words]
            {:share-hash share-hash})]})))

(rf/reg-event-db
 :wordcloud/store-words
 (fn [db [_ {:keys [string-representation]}]]
   (assoc-in db [:wordcloud :words] (convert-fulltext string-representation))))

(rf/reg-sub
 :wordcloud/words
 (fn [db]
   (get-in db [:wordcloud :words])))
