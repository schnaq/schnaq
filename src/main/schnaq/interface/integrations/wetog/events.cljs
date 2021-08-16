(ns schnaq.interface.integrations.wetog.events
  (:require [re-frame.core :as rf]
            [schnaq.interface.utils.js-wrapper :as jsw]))

(rf/reg-event-fx
  :wetog/initialize-from-data
  (fn [{:keys [db]} _]
    (let [share-hash (jsw/data-attribute "schnaq-integration" "shareHash")
          display-name (jsw/data-attribute "schnaq-integration" "displayName")]
      {:db (-> db
               (assoc-in [:user :names :display] display-name)
               (assoc-in [:schnaq :selected :discussion/share-hash] share-hash))
       :fx [[:dispatch [:schnaq/load-by-share-hash share-hash]]]})))

(rf/reg-event-fx
  :initialize/wetog-integration
  (fn [_ _]
    {:fx [[:dispatch [:get-csrf-token]]
          [:dispatch [:how-to-visibility/from-localstorage-to-app-db]]
          ;; TODO Use the JWT from wetog here instead of keycoak
          [:dispatch [:schnaq.discussion-secrets/load-from-localstorage]]]}))