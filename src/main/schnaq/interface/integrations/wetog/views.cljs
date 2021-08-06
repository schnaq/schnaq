(ns schnaq.interface.integrations.wetog.views
  (:require [re-frame.core :as rf]
            [schnaq.interface.utils.js-wrapper :as jsw]))


(defn discussion-start
  "The start of a discussion."
  []
  [:div "hallo"])

(rf/reg-event-fx
  :wetog/initialize-from-data
  (fn [{:keys [db]} _]
    (let [share-hash (jsw/data-attribute "schnaq-integration" "shareHash")
          display-name (jsw/data-attribute "schnaq-integration" "displayName")]
      {:db (-> db
               (assoc-in [:user :names :display] display-name)
               (assoc-in [:wetog :current :share-hash] share-hash))})))