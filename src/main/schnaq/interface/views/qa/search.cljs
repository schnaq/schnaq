(ns schnaq.interface.views.qa.search
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as cstring]
            [com.fulcrologic.guardrails.core :refer [>defn ?]]
            [goog.functions :as gfun]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.discussion.conclusion-card :as card]
            [schnaq.shared-toolbelt :as stools]))

(s/def :background/schema #{:dark :light})

(def throttled-search
  (gfun/throttle
   #(rf/dispatch [:schnaq.qa/search (oget % [:?target :value])])
   500))

(rf/reg-event-fx
 :schnaq.qa/search
 (fn [{:keys [db]} [_ search-term]]
   (if (cstring/blank? search-term)
     {:db (assoc-in db [:schnaq :qa :search :results] [])}
     (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
       {:fx [(http/xhrio-request db :get "/schnaq/qa/search"
                                 [:schnaq.qa.search/success]
                                 {:share-hash share-hash
                                  :search-string search-term
                                  :display-name (tools/current-display-name db)})]}))))
(rf/reg-event-db
 :schnaq.qa.search/success
 (fn [db [_ {:keys [matching-statements children]}]]
   (-> db
       (update-in [:schnaq :statements] merge
                  (stools/normalize :db/id (concat matching-statements children)))
       (assoc-in [:schnaq :qa :search :results] (map :db/id matching-statements)))))

(rf/reg-sub
 :schnaq.qa.search/results
 (fn [db _]
   (get-in db [:schnaq :qa :search :results])))

(rf/reg-event-db
 :schnaq.qa.search.results/reset
 (fn [db _]
   (assoc-in db [:schnaq :qa :search :results] [])))

(>defn results-list
  "A list of statement results that came out of search.
  Background type can be either :dark or :light"
  [background-schema]
  [:background/schema :ret (? :re-frame/component)]
  (let [search-results @(rf/subscribe [:schnaq.qa.search/results])
        dark? (= :dark background-schema)]
    (when (seq search-results)
      [:div.mt-3
       [motion/fade-in-and-out
        [:h5.mx-3.mx-md-0
         (when dark? {:class "text-white"})
         (labels :qanda.search/similar-results)]]
       [motion/fade-in-and-out
        [:div.mx-3.mx-md-0
         [:text-sm
          (when dark? {:class "text-white"})
          (labels :qanda.search/similar-results-explanation-1)
          [icon :arrow-up "m-auto"]
          " "
          (labels :qanda.search/similar-results-explanation-2)]]]
       [:div.row
        (for [result-id search-results]
          [:div.statement-column
           {:key (str result-id "-search-result")}
           [motion/fade-in-and-out
            [card/statement-card result-id]]])]])))
