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


