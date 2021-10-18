(ns schnaq.interface.views.discussion.card-view
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.discussion.card-elements :as elements]
            [schnaq.interface.views.pages :as pages]
            [clojure.string :as cstring]))

(rf/reg-event-fx
  :discussion.statements/search
  (fn [{:keys [db]} [_ search-string dynamic?]]
    (if (str/blank? search-string)
      {:db (-> (assoc-in db [:search :schnaq :current :result] [])
               (assoc-in [:search :schnaq :current :search-string] search-string))}
      (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
        {:db (assoc-in db [:search :schnaq :current :search-string] search-string)
         :fx [(http/xhrio-request db :get "/discussion/statements/search" [:discussion.statements.search/success]
                                  {:share-hash share-hash
                                   :search-string search-string
                                   :display-name (tools/current-display-name db)})
              (when-not dynamic? [:dispatch [:navigation/navigate :routes.search/schnaq {:share-hash share-hash}]])]}))))

(rf/reg-event-db
  :discussion.statements.search/success
  (fn [db [_ {:keys [matching-statements]}]]
    (assoc-in db [:search :schnaq :current :result] matching-statements)))

(defn- discussion-view
  "The first step after starting a discussion."
  []
  (let [schnaq @(rf/subscribe [:schnaq/selected])]
    [elements/discussion-view (:discussion/share-hash schnaq)]))

(rf/reg-sub
  :discussion.statements/show
  ;; The statements which should be shown in the discussion view right now.
  :<- [:discussion.premises/current]
  :<- [:schnaq.search.current/result]
  :<- [:schnaq.search.current/search-string]
  (fn [[premises search-results search-string] _]
    (if (cstring/blank? search-string)
      premises
      search-results)))

(rf/reg-sub
  :discussion.premises/current
  (fn [db _]
    (get-in db [:discussion :premises :current] [])))

(rf/reg-sub
  :discussion.conclusion/selected
  (fn [db _]
    (get-in db [:discussion :conclusion :selected])))

;; -----------------------------------------------------------------------------

(defn derive-view []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])]
    [pages/with-discussion-header
     {:page/heading (:discussion/title current-discussion)}
     [discussion-view]]))

(defn view []
  [derive-view])
