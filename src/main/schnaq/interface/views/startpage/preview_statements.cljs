(ns schnaq.interface.views.startpage.preview-statements
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.config :as config]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.discussion.conclusion-card :as conclusion-card]))

(defn- interactive-example-statements []
  (let [statement-1 @(rf/subscribe [:example-statement/by-id config/example-statement-1])
        statement-2 @(rf/subscribe [:example-statement/by-id config/example-statement-2])
        statement-3 @(rf/subscribe [:example-statement/by-id config/example-statement-3])]
    (when (and statement-1 statement-2 statement-3)
      [:<>
       [:div.example-statement-1.rounded-1.shadow-lg
        [conclusion-card/statement-card statement-1]]
       [:div.example-statement-2.rounded-1.shadow-lg
        [conclusion-card/statement-card statement-2]]
       [:div.example-statement-3.rounded-1.shadow-lg
        [conclusion-card/statement-card statement-3]]])))

(defn display-example-statements
  "Displays interactive example statements from the discussion specified in the config.
  When no statements are found displays a static image instead"
  []
  (if @(rf/subscribe [:example-statement/static-image-fallback?])
    [:img.img-fluid {:src (img-path :startpage.example/statements)}]
    [interactive-example-statements]))

(rf/reg-sub
  :example-statement/by-id
  (fn [db [_ id]]
    (when (get-in db [:current-route :path-params :share-hash])
      (get-in db [:preview-statements id]))))

(rf/reg-sub
  :example-statement/static-image-fallback?
  (fn [db _]
    (get-in db [:preview-statements :image-fallback])))

(rf/reg-event-fx
  :discussion.query.example-statement/by-id
  (fn [{:keys [db]} [_ share-hash statement-id api-url]]
    {:fx [(http/xhrio-request
            db :get "/discussion/statement/info"
            [:preview-statements/by-id-success]
            {:statement-id statement-id
             :share-hash share-hash
             :display-name (tools/current-display-name db)}
            [:preview-statements/default]
            api-url)]}))

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
          api-url config/example-api-url
          id-1 config/example-statement-1
          id-2 config/example-statement-2
          id-3 config/example-statement-3]
      {:db (assoc-in db [:current-route :path-params :share-hash] share-hash)
       :fx [[:dispatch [:schnaq/load-by-share-hash share-hash api-url]]
            [:dispatch [:discussion.query.example-statement/by-id share-hash id-1 api-url]]
            [:dispatch [:discussion.query.example-statement/by-id share-hash id-2 api-url]]
            [:dispatch [:discussion.query.example-statement/by-id share-hash id-3 api-url]]]})))
