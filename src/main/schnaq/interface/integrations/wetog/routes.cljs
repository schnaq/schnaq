(ns schnaq.interface.integrations.wetog.routes
  (:require [re-frame.core :as rf]
            [reitit.coercion.spec]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.discussion.card-view :as discussion-card-view]
            [schnaq.interface.views.discussion.dashboard :as dashboard]
            [schnaq.interface.views.graph.view :as graph-view]))

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
                            (rf/dispatch [:statement.edit/reset-edits])
                            (rf/dispatch [:toggle-replies/clear!]))}]}]
   ["dashboard"
    {:name :routes.schnaq/dashboard
     :view dashboard/embedded-view
     :link-text (labels :router/dashboard)
     :controllers [{:start (fn []
                             (rf/dispatch [:wordcloud/for-current-discussion])
                             (rf/dispatch [:schnaq/refresh-selected])
                             (rf/dispatch [:scheduler.after/login [:schnaq.summary/load]]))}]}]
   ["statement/:statement-id"
    {:name :routes.schnaq.select/statement
     :parameters {:path {:statement-id int?}}
     :view discussion-card-view/view
     :controllers [{:parameters {:path [:statement-id]}
                    :start (fn []
                             (rf/dispatch [:discussion.query.statement/by-id]))
                    :stop (fn []
                            (rf/dispatch [:visited.statement-nums/to-localstorage])
                            (rf/dispatch [:visited.statement-ids/to-localstorage-and-merge-with-app-db])
                            (rf/dispatch [:statement.edit/reset-edits])
                            (rf/dispatch [:toggle-replies/clear!]))}]}]
   ["graph"
    {:name :routes/graph-view
     :view graph-view/graph-view-entrypoint
     :link-text (labels :router/graph-view)
     :controllers [{:identity (fn [] (random-uuid))
                    :start (fn []
                             (rf/dispatch [:spinner/active! true])
                             (rf/dispatch [:updates.periodic/graph true])
                             (rf/dispatch [:graph/load-data-for-discussion]))
                    :stop (fn []
                            (rf/dispatch [:updates.periodic/graph false])
                            (rf/dispatch [:notifications/reset]))}]}]])
