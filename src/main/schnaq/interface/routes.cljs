(ns schnaq.interface.routes
  (:require [schnaq.interface.analytics.core :as analytics]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.agenda.agenda :as agenda-views]
            [schnaq.interface.views.agenda.edit :as agenda-edit]
            [schnaq.interface.views.discussion.discussion :as discussion-views]
            [schnaq.interface.views.errors :as error-views]
            [schnaq.interface.views.feedback :as feedback]
            [schnaq.interface.views.meeting.after-create :as meeting-created]
            [schnaq.interface.views.meeting.meetings :as meeting-views]
            [schnaq.interface.views.meeting.overview :as meetings-overview]
            [schnaq.interface.views.meeting.single :as meeting-single]
            [schnaq.interface.views.startpage :as startpage-views]
            [schnaq.interface.views.graph.view :as graph-view]
            [re-frame.core :as rf]
            [reitit.coercion.spec]))

;; It is important to note, that we navigate by not calling /meetings for example,
;; but by calling #/meetings. The anchor triggers reitit inside of re-frame instead
;; of hard reloading and dispatching to the web-server.

;; The controllers can be used to execute things at the start and the end of applying
;; the new route.
(def routes
  ["/"
   {:coercion reitit.coercion.spec/coercion}                ;; Enable Spec coercion for all routes
   [""
    {:name :routes/startpage
     :view startpage-views/startpage-view
     :link-text (labels :router/startpage)}]
   ["meetings"
    (when-not toolbelt/production?
      [""
       {:name :routes/meetings
        :view meetings-overview/meeting-view
        :link-text (labels :router/all-meetings)}])
    ["/create"
     {:name :routes.meeting/create
      :view meeting-views/create-meeting-form-view
      :link-text (labels :router/create-meeting)}]
    ["/:share-hash"
     {:parameters {:path {:share-hash string?}}
      :controllers [{:parameters {:path [:share-hash]}
                     :start (fn [{:keys [path]}]
                              (rf/dispatch [:load-meeting-by-share-hash (:share-hash path)]))}]}
     ["/:admin-hash"
      {:parameters {:path {:admin-hash string?}}
       :controllers [{:parameters {:path [:share-hash :admin-hash]}
                      :start (fn [{:keys [path]}]
                               (let [{:keys [share-hash admin-hash]} path]
                                 (rf/dispatch [:meeting/check-admin-credentials share-hash admin-hash])))}]}
      ["/edit"
       {:name :routes.meeting/edit
        :view agenda-edit/edit-view
        :controllers [{:parameters {:path [:share-hash]}
                       :start (fn [{:keys [path]}]
                                (rf/dispatch [:agenda/load-for-edit (:share-hash path)]))}]}]
      ["/created"
       {:name :routes.meeting/created
        :view meeting-created/after-meeting-creation-view
        :link-text (labels :router/meeting-created)}]]
     ["/"
      {:name :routes.meeting/show
       :view meeting-single/single-meeting-view
       :link-text (labels :router/show-single-meeting)
       :controllers [{:parameters {:path [:share-hash]}
                      :start (fn [{:keys [path]}]
                               (rf/dispatch [:agenda/load-and-redirect (:share-hash path)]))
                      :stop #(rf/dispatch [:clear-current-agendas])}]}]
     ["/agenda"
      ["/add"
       {:name :routes.agenda/add
        :view agenda-views/agenda-view
        :link-text (labels :router/add-agendas)
        :controllers [{:start #(rf/dispatch [:agenda/redirect-on-reload])}]}]
      ["/:id"
       {:parameters {:path {:id int?}}
        :controllers [{:parameters {:path [:share-hash :id]}
                       :start (fn [{:keys [path]}]
                                (rf/dispatch [:load-agenda-information (:share-hash path) (:id path)]))}]}
       ["/start"
        {:controllers [{:parameters {:path [:share-hash :id]}
                        :start (fn []
                                 (rf/dispatch [:start-discussion])
                                 (rf/dispatch [:discussion.history/clear]))}]
         :name :routes.discussion/start
         :view discussion-views/discussion-start-view
         :link-text (labels :router/start-discussion)}]
       ["/continue"
        {:name :routes.discussion/continue
         :view discussion-views/discussion-loop-view
         :link-text (labels :router/continue-discussion)
         :controllers [{:parameters {:path [:id :share-hash]}
                        :start (fn [{:keys [path]}]
                                 (rf/dispatch [:handle-reload-on-discussion-loop (:id path) (:share-hash path)]))}]}]
       ["/graph"
        {:name :routes/graph-view
         :view graph-view/view
         :link-text (labels :router/graph-view)
         :controllers [{:identity (fn [] (random-uuid))
                        :start #(rf/dispatch [:graph/load-data-for-discussion])}]}]]]]]
   ["feedbacks"
    {:name :routes/feedbacks
     :view feedback/overview
     :link-text (labels :router/all-feedbacks)}]
   ["analytics"
    {:name :routes/analytics
     :view analytics/analytics-dashboard-view
     :link-text (labels :router/analytics)
     :controllers [{:start (fn []
                             (rf/dispatch [:admin/set-password (js/prompt "Enter the Admin Password to see analytics")])
                             (rf/dispatch [:analytics/load-dashboard]))}]}]
   ["invalid-link"
    {:name :routes/invalid-link
     :view error-views/invalid-admin-link-view
     :link-text (labels :router/invalid-link)}]])