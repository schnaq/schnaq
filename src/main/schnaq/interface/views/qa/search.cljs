(ns schnaq.interface.views.qa.search
  (:require [clojure.string :as cstring]
            [goog.functions :as gfun]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.discussion.conclusion-card :as card]))

(def throttled-search
  (gfun/throttle
    #(rf/dispatch [:schnaq.qa/search (oget % [:?target :value])])
    500))

(rf/reg-event-fx
  :schnaq.qa/search
  (fn [{:keys [db]} [_ search-term]]
    (when-not (cstring/blank? search-term)
      (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
        {:fx [(http/xhrio-request db :get "/schnaq/qa/search"
                                  [:schnaq.qa.search/success]
                                  {:share-hash share-hash
                                   :search-string search-term
                                   :display-name (tools/current-display-name db)})]}))))
(rf/reg-event-db
  :schnaq.qa.search/success
  (fn [db [_ {:keys [matching-statements]}]]
    (assoc-in db [:schnaq :qa :search :results] matching-statements)))

(rf/reg-sub
  :schnaq.qa.search/results
  (fn [db _]
    (get-in db [:schnaq :qa :search :results] [])))

(defn results-list
  "A list of statement results that came out of search."
  []
  (let [search-results @(rf/subscribe [:schnaq.qa.search/results])]
    (when (seq search-results)
      [:div.mt-3
       [motion/move-in :top
        [:h5.text-white.mx-3.mx-md-0 (labels :qanda.search/similar-results)]]
       [motion/move-in :top
        [:div.mx-3.mx-md-0
         [:text-sm.text-white
          (labels :qanda.search/similar-results-explanation-1)
          [icon :arrow-up "m-auto"]
          " "
          (labels :qanda.search/similar-results-explanation-2)]]]

       [:div.card-columns.card-columns-discussion
        (for [result search-results]
          (with-meta
            [motion/move-in-spring :bottom
             [card/answer-card result]]
            {:key (str (:db/id result) "-search-result")}))]])))
