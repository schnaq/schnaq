(ns schnaq.interface.views.discussion.search
  (:require [goog.string :as gstring]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.discussion.conclusion-card :as card]
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
    [pages/with-discussion-header
     {:page/heading (labels :schnaq.search/title)}
     [:div.container.mt-4
      (if (= "" search-string)
        [:div.w-100.text-center
         [:h4 (labels :schnaq.search/new-search-title)]]
        [:<>
         [:h4.text-center (gstring/format (labels :schnaq.search/heading) search-string)]
         (for [statement results]
           [:div.p-2.w-lg-50.d-inline-block
            {:key (:db/id statement)}
            [card/statement-card nil statement]])])]]))

(defn view []
  [search-view])