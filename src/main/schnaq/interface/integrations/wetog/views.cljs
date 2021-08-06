(ns schnaq.interface.integrations.wetog.views
  (:require [re-frame.core :as rf]
            [schnaq.interface.utils.js-wrapper :as jsw]))

(rf/reg-event-fx
  :wetog/initialize-from-data
  (fn [{:keys [db]} _]
    (let [share-hash (jsw/data-attribute "schnaq-integration" "shareHash")
          display-name (jsw/data-attribute "schnaq-integration" "displayName")]
      {:db (assoc-in db [:user :names :display] display-name)
       :fx [[:dispatch [:schnaq/load-by-share-hash share-hash]]]})))