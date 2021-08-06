(ns schnaq.interface.integrations.wetog.views
  (:require [re-frame.core :as rf]
            [schnaq.interface.utils.js-wrapper :as jsw]
            [schnaq.interface.views.discussion.card-view :as discussion-cards]))


(defn discussion-start
  "The start of a discussion."
  []
  [discussion-cards/derive-view discussion-cards/discussion-start-view discussion-cards/selected-conclusion-view])

(rf/reg-event-fx
  :wetog/initialize-from-data
  (fn [{:keys [db]} _]
    (let [share-hash (jsw/data-attribute "schnaq-integration" "shareHash")
          display-name (jsw/data-attribute "schnaq-integration" "displayName")]
      {:db (assoc-in db [:user :names :display] display-name)
       :fx [[:dispatch [:schnaq/load-by-share-hash share-hash]]]})))