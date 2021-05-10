(ns schnaq.interface.views.discussion.search
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.views.pages :as pages]))

(rf/reg-sub
  :schnaq.search.current/search-string
  (fn [db _]
    (get-in db [:search :schnaq :current :search-string] "")))

(rf/reg-sub
  :schnaq.search.current/result
  (fn [db _]
    (get-in db [:search :schnaq :current :result] [])))

(defn- search-view
  []
  (let [search-string @(rf/subscribe [:schnaq.search.current/search-string])
        results @(rf/subscribe [:schnaq.search.current/result])
        current-route @(rf/subscribe [:navigation/current-route])]
    [pages/with-nav
     {:page/title "Suche"}
     [:div.container
      [:p "Du hast gesucht!"]
      [:p "Nach: " search-string]
      (for [result results]
        [:a {:key result
             :href (reitfe/href :routes.schnaq.select/statement
                                {:share-hash (-> current-route :path-params :share-hash)
                                 :statement-id result})}
         [:p result]])]]))

(defn view []
  [search-view])