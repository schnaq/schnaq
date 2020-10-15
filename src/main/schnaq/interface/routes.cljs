(ns schnaq.interface.routes
  (:require [re-frame.core :as rf]
            [reitit.coercion.spec]
            [schnaq.interface.analytics.core :as analytics]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.agenda.edit :as agenda-edit]
            [schnaq.interface.views.discussion.discussion :as discussion-views]
            [schnaq.interface.views.errors :as error-views]
            [schnaq.interface.views.feedback.admin :as feedback-admin]
            [schnaq.interface.views.meeting.admin-center :as meeting-admin]
            [schnaq.interface.views.meeting.meetings :as meeting-views]
            [schnaq.interface.views.meeting.overview :as meetings-overview]
            [schnaq.interface.views.meeting.single :as meeting-single]
            [schnaq.interface.views.how-to.view :as how-to]
            [schnaq.interface.views.startpage.core :as startpage-views]
            [schnaq.interface.views.startpage.features :as features-views]
            [schnaq.interface.views.graph.view :as graph-view]))

;; It is important to note, that we navigate by not calling /meetings for example,
;; but by calling #/meetings. The anchor triggers reitit inside of re-frame instead
;; of hard reloading and dispatching to the web-server.

;; The controllers can be used to execute things at the start and the end of applying
;; the new route.

;; IMPORTANT: Routes called here as views do not hot-reload for some reason. Only
;; components inside do regularly. So just use components here that wrap the view you
;; want to function regularly.
(def routes
  ["/"
   {:coercion reitit.coercion.spec/coercion}                ;; Enable Spec coercion for all routes
   [""
    {:name :routes/startpage
     :view startpage-views/startpage-view
     :link-text (labels :router/startpage)}]
   ["meeting-features"
    {:name :routes.features/meetings
     :view features-views/meeting-features-view
     :link-text (labels :router.features/meetings)}]
   ["discussion-features"
    {:name :routes.features/discussions
     :view features-views/discussion-features-view
     :link-text (labels :router.features/discussion)}]
   ["how-to"
    {:name :routes/how-to
     :view how-to/view
     :link-text (labels :routes/how-to)}]
   ["meetings"
    {:controllers [{:start (fn [_]
                             (rf/dispatch [:username/open-dialog]))}]}
    (when-not toolbelt/production?
      [""
       {:name :routes/meetings
        :view meetings-overview/meeting-view-entry
        :link-text (labels :router/all-meetings)}])
    ["/my"
     {:name :routes.meetings/my-schnaqs
      :view meetings-overview/meeting-view-visited
      :link-text (labels :router/my-schnaqs)
      :controllers [{:start (fn [] (rf/dispatch [:meetings.visited/load]))}]}]
    ["/create"
     {:name :routes.meeting/create
      :view meeting-views/create-meeting-view
      :link-text (labels :router/create-meeting)}]
    ["/:share-hash"
     {:parameters {:path {:share-hash string?}}
      :controllers [{:parameters {:path [:share-hash]}
                     :start (fn [{:keys [path]}]
                              (rf/dispatch [:meeting/load-by-share-hash (:share-hash path)]))}]}
     ["/:edit-hash"
      {:parameters {:path {:edit-hash string?}}
       :controllers [{:parameters {:path [:share-hash :edit-hash]}
                      :start (fn [{:keys [path]}]
                               (let [{:keys [share-hash edit-hash]} path]
                                 (rf/dispatch [:meeting/check-admin-credentials share-hash edit-hash])))}]}
      ["/edit"
       {:name :routes.meeting/edit
        :view agenda-edit/agenda-edit-view
        :controllers [{:parameters {:path [:share-hash :edit-hash]}
                       :start (fn [{:keys [path]}]
                                (rf/dispatch [:agenda/load-for-edit (:share-hash path)])
                                (rf/dispatch [:suggestions/get-updates (:share-hash path) (:edit-hash path)]))
                       :stop #(rf/dispatch [:agenda.edit/reset-edit-updates])}]}]
      ["/manage"
       {:name :routes.meeting/admin-center
        :view meeting-admin/admin-center-view
        :link-text (labels :router/meeting-created)
        :controllers [{:parameters {:path [:share-hash :edit-hash]}
                       :start (fn [{:keys [path]}]
                                (let [share-hash (:share-hash path)
                                      edit-hash (:edit-hash path)]
                                  (rf/dispatch [:meeting/load-by-hash-as-admin share-hash edit-hash])
                                  (rf/dispatch [:meetings.save-admin-access/to-localstorage
                                                share-hash edit-hash])))}]}]]
     ["/"
      {:name :routes.meeting/show
       :view meeting-single/single-meeting-view
       :link-text (labels :router/show-single-meeting)
       :controllers [{:parameters {:path [:share-hash]}
                      :start (fn [{:keys [path]}]
                               (rf/dispatch [:agenda/load-agendas (:share-hash path)]))
                      :stop (fn []
                              (rf/dispatch [:agenda.statement-nums/to-localstorage])
                              (rf/dispatch [:agenda/clear-current]))}]}]
     ["/suggestions"
      {:name :routes.meeting/suggestions
       :view agenda-edit/agenda-suggestion-view
       :controllers [{:parameters {:path [:share-hash]}
                      :start (fn [{:keys [path]}]
                               (rf/dispatch [:agenda/load-for-edit (:share-hash path)]))
                      :stop #(rf/dispatch [:agenda.edit/reset-edit-updates])}]}]
     ["/agenda"
      ["/:id"
       {:parameters {:path {:id int?}}
        :controllers [{:parameters {:path [:share-hash :id]}
                       :start (fn [{:keys [path]}]
                                (rf/dispatch [:agenda/load-chosen (:share-hash path) (:id path)]))}]}
       ["/start"
        {:controllers [{:parameters {:path [:share-hash :id]}
                        :start (fn []
                                 (rf/dispatch [:discussion/start])
                                 (rf/dispatch [:discussion.history/clear])
                                 (rf/dispatch [:discussion.query.conclusions/starting]))}]
         :name :routes.discussion/start
         :view discussion-views/discussion-start-view-entrypoint
         :link-text (labels :router/start-discussion)}]
       ["/continue"
        {:name :routes.discussion/continue
         :view discussion-views/discussion-loop-view-entrypoint
         :link-text (labels :router/continue-discussion)
         :controllers [{:parameters {:path [:id :share-hash]}
                        :start (fn [{:keys [path]}]
                                 (rf/dispatch [:discussion/handle-hard-reload (:id path) (:share-hash path)]))}]}]
       ["/graph"
        {:name :routes/graph-view
         :view graph-view/graph-view-entrypoint
         :link-text (labels :router/graph-view)
         :controllers [{:identity (fn [] (random-uuid))
                        :start #(rf/dispatch [:graph/load-data-for-discussion])}]}]]]]]
   ["feedbacks"
    {:name :routes/feedbacks
     :view feedback-admin/feedbacks-view
     :link-text (labels :router/all-feedbacks)}]
   ["analytics"
    {:name :routes/analytics
     :view analytics/analytics-dashboard-entrypoint
     :link-text (labels :router/analytics)
     :controllers [{:start (fn []
                             (rf/dispatch [:admin/set-password (js/prompt "Enter the Admin Password to see analytics")])
                             (rf/dispatch [:analytics/load-dashboard]))}]}]
   ["error"
    {:name :routes/cause-not-found
     :view error-views/not-found-view-stub
     :link-text (labels :router/not-found-label)
     :controllers [{:identity #(random-uuid)
                    :start #(js-wrap/replace-url "/404/")}]}]
   ["403/"
    {:name :routes/forbidden-page
     :view error-views/forbidden-page}]
   ["404/"
    {:name :routes/true-404-view
     :view error-views/true-404-entrypoint
     :link-text (labels :router/true-404-view)}]])