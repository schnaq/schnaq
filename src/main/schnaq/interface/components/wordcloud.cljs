(ns schnaq.interface.components.wordcloud
  (:require ["react-bootstrap" :refer [Button]]
            ["react-wordcloud" :default ReactWordcloud]
            ["stopword" :refer [deu eng]]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [com.fulcrologic.guardrails.core :refer [>defn >defn-]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.database.specs :as specs]
            [schnaq.interface.components.colors :refer [colors]]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.preview :as preview]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.file-download :as file-download]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.loading :refer [spinner-icon]]))

(def stopwords
  "Stopwords which should be removed from the wordcloud."
  (set/union (set deu) (set eng)))

(def ^:private words-to-be-wordclouded
  "Reduce total number of words which are rendered in the wordcloud."
  50)

(s/def ::text ::specs/non-blank-string)
(s/def ::value number?)
(s/def ::word
  (s/keys :req-un [::text ::value]))

(>defn extract-link-text-from-md
  "Check if text contains a markdown link. If yes, then return the link's name,
  else return the word."
  [word]
  [string? :ret string?]
  (if-let [link (re-seq #"\[(.*)\](.*)" word)]
    (-> link first second)
    word))

(>defn remove-md-links
  "Remove all occurrences of markdown links."
  [s]
  [string? :ret string?]
  (str/replace s #"\[([\w|\s]*)\]\(\S*\)" "$1"))

(>defn- convert-fulltext
  "Convert a fulltext to the format our wordcloud requires."
  [fulltext]
  [string? :ret (s/coll-of ::word)]
  (for [[word total] (->> (str/split (remove-md-links fulltext) #"\s")
                          (map extract-link-text-from-md)
                          (map #(str/replace % #"[^A-z0-9äöüÄÖÜß]" "")) ;; remove all non-word characters
                          (map str/lower-case)
                          (remove #(stopwords %))
                          (remove empty?)
                          frequencies)]
    {:text word
     :value total}))

(def ^:private options
  {:colors (vals (dissoc colors :white))
   :enableTooltip true
   :enableOptimizations true
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

(defn- wordcloud-download-button
  "Download wordcloud as svg."
  [svg]
  (when svg
    [:> Button {:variant :link
                :class "text-muted p-0 pe-2 align-self-end"
                :on-click #(file-download/download-svg-node svg "wordcloud.svg")}
     [tooltip/text
      (labels :schnaq.wordcloud/download)
      [:span [icon :file-download "me-1"]]]]))

(defn wordcloud
  "Create a wordcloud based on the data that is passed in."
  [_input]
  (let [wc (r/atom nil)]
    (fn [input]
      (if-let [words (->> input
                          (sort-by :value)
                          reverse
                          (take words-to-be-wordclouded))]
        (let [svg (when @wc (-> @wc (oget :children) first (oget :children) first))]
          (if (empty? words)
            [:div.p-3.m-3.p-lg-5.m-lg-5.text-muted
             (labels :schnaq.wordcloud/no-words-yet)
             [icon :smile-beam "ms-1"]]
            [:div {:ref #(when-not @wc (reset! wc %))}
             [:> ReactWordcloud {:words words :options options}]
             [:div.text-end [wordcloud-download-button svg]]]))
        [:div.text-center.py-3 [spinner-icon]]))))

(defn wordcloud-preview
  "If user is pro-user display a wordcloud and if not show a preview instead."
  []
  (if @(rf/subscribe [:user/pro?])
    [wordcloud @(rf/subscribe [:wordcloud/words])]
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
   (let [old-words (get-in db [:wordcloud :words])
         new-words (convert-fulltext string-representation)]
     (when (not= old-words new-words)
       (assoc-in db [:wordcloud :words] new-words)))))

(rf/reg-sub
 :wordcloud/words
 (fn [db]
   (get-in db [:wordcloud :words])))
