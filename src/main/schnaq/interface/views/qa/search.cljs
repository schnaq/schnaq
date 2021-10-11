(ns schnaq.interface.views.qa.search
  (:require [clojure.string :as cstring]
            [goog.functions :as gfun]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]))

(def throttled-search
  (gfun/throttle
    #(rf/dispatch [:schnaq.qa/search (oget % [:?target :value])])
    500))

(rf/reg-event-fx
  :schnaq.qa/search
  (fn [{:keys [db]} [_ search-term]]
    (when-not (cstring/blank? search-term)
      (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
        {:fx [(http/xhrio-request db :get "/discussion/statements/search"
                                  [:schnaq.qa.search/success]
                                  {:share-hash share-hash
                                   :search-string search-term
                                   :display-name (tools/current-display-name db)})]}))))
(rf/reg-event-db
  :schnaq.qa.search/success
  (fn [db [_ {:keys [matching-statements]}]]
    (assoc-in db [:schnaq :qa :search :results] matching-statements)))
