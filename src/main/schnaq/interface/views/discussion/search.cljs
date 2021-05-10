(ns schnaq.interface.views.discussion.search
  (:require [re-frame.core :as rf]
            [schnaq.interface.views.discussion.conclusion-card :as card]
            [schnaq.interface.views.discussion.logic :as logic]
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
        results @(rf/subscribe [:schnaq.search.current/result])]
    [pages/with-nav
     {:page/title "Suche"}
     [:div.container
      [:p "Du hast gesucht!"]
      [:p "Nach: " search-string]
      (for [statement results]
        [:div.py-2
         {:key (:db/id statement)}
         [card/statement-card
          nil statement (logic/arg-type->attitude (:meta/argument-type statement))]])]]))

(defn view []
  [search-view])