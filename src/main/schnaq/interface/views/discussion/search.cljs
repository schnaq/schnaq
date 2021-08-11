(ns schnaq.interface.views.discussion.search
  (:require [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.text.display-data :refer [labels img-path]]
            [schnaq.interface.views.discussion.card-elements :as elements]
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
        results @(rf/subscribe [:schnaq.search.current/result])
        page-wrapper (if shared-config/embedded? pages/embeddable-view pages/with-discussion-header)
        empty-search? (= "" search-string)]
    [page-wrapper
     {:page/heading (labels :schnaq.search/title)}
     [:div.container-fluid
      [:div.row
       [:div.col-md-6.col-lg-4.py-4.px-0.px-md-3
        [:div.panel-white.mb-4
         [:div.d-inline-block
          [:h2 (labels :schnaq.search/heading)]
          [:div.row.mx-0.mt-4.mb-3
           [:img.dashboard-info-icon-sm {:src (img-path :icon-search)}]
           [:div.text.display-6.my-auto.mx-3
            (if empty-search?
              (labels :schnaq.search/no-input)
              search-string)]]]
         [:div.row.m-0
          [:img.dashboard-info-icon-sm {:src (img-path :icon-posts)}]
          (if (or empty-search? (empty? results))
            [:p.mx-3 (labels :schnaq.search/new-search-title)]
            [:p.mx-3 (str (count results) " " (labels :schnaq.search/results))])]]]
       [:div.col-md-6.col-lg-8.py-4.px-0.px-md-3
        [elements/action-view true]
        (when-not empty-search?
          (for [statement results]
            [:div.p-2.w-lg-50.d-inline-block
             {:key (:db/id statement)}
             [card/statement-card nil statement]]))]]]]))

(defn view []
  [search-view])