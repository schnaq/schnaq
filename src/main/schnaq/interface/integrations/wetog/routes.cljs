(ns schnaq.interface.integrations.wetog.routes
  (:require [re-frame.core :as rf]
            [reitit.coercion.spec]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.discussion.card-view :as discussion-card-view]
            [schnaq.interface.views.discussion.search :as discussion-search]))

(def routes
  ["/"
   {:coercion reitit.coercion.spec/coercion
    :controllers [{:start (fn [] (rf/dispatch [:wetog/initialize-from-data]))}]}
   [""
    {:name :routes.schnaq/start
     :view discussion-card-view/view
     :link-text (labels :router/startpage)
     :controllers [{:parameters {:path [:share-hash]}
                    :start (fn []
                             (rf/dispatch [:discussion.history/clear])
                             (rf/dispatch [:updates.periodic/starting-conclusions true])
                             (rf/dispatch [:discussion.query.conclusions/starting]))
                    :stop (fn []
                            (rf/dispatch [:updates.periodic/starting-conclusions false])
                            (rf/dispatch [:statement.edit/reset-edits]))}]}]
   ["statement/:statement-id"
    {:name :routes.schnaq.select/statement
     :parameters {:path {:statement-id int?}}
     :view discussion-card-view/view
     :controllers [{:parameters {:path [:statement-id]}
                    :start (fn []
                             (rf/dispatch [:discussion.query.statement/by-id]))
                    :stop (fn []
                            (rf/dispatch [:visited.statement-nums/to-localstorage])
                            (rf/dispatch [:statement.edit/reset-edits]))}]}]
   ["search"
    {:name :routes.search/schnaq
     :view discussion-search/view}]])