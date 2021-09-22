(ns schnaq.interface.views.startpage.preview-statements
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.config :as config]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.discussion.conclusion-card :as conclusion-card]))


(defn- interactive-example-statements []
  (let [statement-1 @(rf/subscribe [:example-statement/by-id config/example-statement-1])
        statement-2 @(rf/subscribe [:example-statement/by-id config/example-statement-2])
        statement-3 @(rf/subscribe [:example-statement/by-id config/example-statement-3])]
    (when (and statement-1 statement-2 statement-3)
      [:div
       [:div.example-statement-1.shadow-lg
        [conclusion-card/statement-card nil statement-1]]
       [:div..example-statement-2.shadow-lg
        [conclusion-card/statement-card nil statement-2]]
       [:div.example-statement-3.shadow-lg
        [conclusion-card/statement-card nil statement-3]]])))

(defn display-example-statements
  "Displays interactive example statements from the discussion specified in the config.
  When no statements are found displays a static image instead"
  []
  (let [static-image? @(rf/subscribe [:example-statement/static-image-fallback?])]
    (if static-image?
      [:img.img-fluid {:src (img-path :startpage.example/statements)}]
      [interactive-example-statements])))

(rf/reg-sub
  :example-statement/by-id
  (fn [db [_ id]]
    (get-in db [:preview-statements id])))

(rf/reg-sub
  :example-statement/static-image-fallback?
  (fn [db _]
    (get-in db [:preview-statements :image-fallback])))

(rf/reg-event-fx
  :discussion.query.example-statement/by-id
  (fn [{:keys [db]} [_ share-hash statement-id]]
    {:fx [(http/xhrio-request
            db :get "/discussion/statement/info"
            [:preview-statements/by-id-success]
            {:statement-id statement-id
             :share-hash share-hash}
            [:preview-statements/default])]}))

(rf/reg-event-fx
  :preview-statements/by-id-success
  (fn [{:keys [db]} [_ {:keys [conclusion]}]]
    {:db (assoc-in db [:preview-statements (:db/id conclusion)] conclusion)}))

(rf/reg-event-fx
  :preview-statements/default
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:preview-statements :image-fallback] true)}))

(rf/reg-event-fx
  :load-example-statements
  (fn [{:keys [db]} _]
    (let [share-hash config/example-share-hash
          id-1 config/example-statement-1
          id-2 config/example-statement-2
          id-3 config/example-statement-3]
      {:db (assoc-in db [:current-route :path-params :share-hash] share-hash)
       :fx [[:dispatch [:schnaq/load-by-share-hash share-hash]]
            [:dispatch [:discussion.query.example-statement/by-id share-hash id-1]]
            [:dispatch [:discussion.query.example-statement/by-id share-hash id-2]]
            [:dispatch [:discussion.query.example-statement/by-id share-hash id-3]]]})))
