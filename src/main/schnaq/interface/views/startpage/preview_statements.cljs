(ns schnaq.interface.views.startpage.preview-statements
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.config :as config]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.discussion.conclusion-card :as conclusion-card]
            [schnaq.shared-toolbelt :as stools]))

(defn display-example-statements
  "Displays interactive example statements from the discussion specified in the config.
  When no statements are found displays a static image instead"
  []
  (let [statement-id @(rf/subscribe [:preview-statement])]
    (if statement-id
      [:div.rounded-1.shadow-lg
       [conclusion-card/statement-card statement-id]]
      [:img.img-fluid {:src (img-path :startpage.example/statements)
                       :alt (labels :startpage.example.statements/alt-text)}])))

(rf/reg-sub
 :preview-statement
 (fn [db]
   (:preview-statements db)))

(rf/reg-event-fx
 :discussion.query/preview-statement
 (fn [{:keys [db]} [_ share-hash statement-id api-url]]
   {:fx [(http/xhrio-request
          db :get "/discussion/statement/info"
          [:preview-statements/success]
          {:statement-id statement-id
           :share-hash share-hash
           :display-name (tools/current-display-name db)}
          []
          api-url)]}))

(rf/reg-event-fx
 :preview-statements/success
 (fn [{:keys [db]} [_ {:keys [conclusion children]}]]
   {:db (-> db
            (assoc :preview-statements (:db/id conclusion))
            (update-in [:schnaq :statements] merge (stools/normalize :db/id (conj children conclusion))))}))

(rf/reg-event-fx
 :load-preview-statements
 (fn [_ _]
   (let [share-hash config/example-share-hash
         api-url config/example-api-url]
     {:fx [[:dispatch [:schnaq/load-by-share-hash share-hash api-url]]
           [:dispatch [:discussion.query/preview-statement share-hash config/example-statement api-url]]]})))
