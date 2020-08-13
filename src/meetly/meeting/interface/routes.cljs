(ns meetly.meeting.interface.routes
  (:require [meetly.meeting.interface.views.startpage :as startpage-views]
            [meetly.meeting.interface.views.agenda.agenda :as agenda-views]
            [meetly.meeting.interface.views.agenda.edit :as agenda-edit]
            [meetly.meeting.interface.views.meeting.meetings :as meeting-views]
            [meetly.meeting.interface.views.meeting.overview :as meetings-overview]
            [meetly.meeting.interface.views.meeting.single :as meeting-single]
            [meetly.meeting.interface.views.meeting.after-create :as meeting-created]
            [meetly.meeting.interface.config :refer [config]]
            [meetly.meeting.interface.text.display-data :refer [labels]]
            [meetly.meeting.interface.views.discussion.discussion :as discussion-views]
            [meetly.meeting.interface.views.feedback :as feedback]
            [meetly.meeting.interface.analytics.core :as analytics]
            [reitit.coercion.spec]
            [re-frame.core :as rf]))

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
    (when (not= "production" (:environment config))
      [""
       {:name :routes/meetings
        :view meetings-overview/meeting-view
        :link-text (labels :router/all-meetings)}])
    ["/create"
     {:name :routes/meetings.create
      :view meeting-views/create-meeting-form-view
      :link-text (labels :router/create-meeting)}]
    ["/:share-hash"
     {:parameters {:path {:share-hash string?}}
      :controllers [{:parameters {:path [:share-hash]}
                     :start (fn [{:keys [path]}]
                              (let [hash (:share-hash path)]
                                (rf/dispatch [:load-meeting-by-share-hash hash])))}]}
     ["/:admin-hash"
      {:parameters {:path {:admin-hash string?}}
       :controllers [{:parameters {:path [:share-hash :admin-hash]}
                      :start (fn [{:keys [path]}]
                               (let [share-hash (:share-hash path)
                                     edit-hash (:admin-hash path)]
                                 (rf/dispatch [:meeting/check-admin-credentials share-hash edit-hash])))}]}
      ["/edit"
       {:name :routes/edit
        :view agenda-edit/edit-view
        :controllers [{:parameters {:path [:share-hash]}
                       :start (fn [{:keys [path]}]
                                (let [hash (:share-hash path)]
                                  (rf/dispatch [:agenda/load-for-edit hash])))}]}]
      ["/created"
       {:name :routes.meeting.created
        :view meeting-created/after-meeting-creation-view
        :link-text (labels :router/meeting-created)}]]
     ["/"
      {:name :routes/meetings.show
       :view meeting-single/single-meeting-view
       :link-text (labels :router/show-single-meeting)
       :controllers [{:parameters {:path [:share-hash]}
                      :start (fn [{:keys [path]}]
                               (let [hash (:share-hash path)]
                                 (rf/dispatch [:load-agendas hash])))
                      :stop (fn []
                              (rf/dispatch [:clear-current-agendas]))}]}]
     ["/agenda"
      ["/add"
       {:name :routes/meetings.agenda
        :view agenda-views/agenda-view
        :link-text (labels :router/add-agendas)}]
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
         :name :routes/meetings.discussion.start
         :view discussion-views/discussion-start-view
         :link-text (labels :router/start-discussion)}]
       ["/continue"
        {:name :routes/meetings.discussion.continue
         :view discussion-views/discussion-loop-view
         :link-text (labels :router/continue-discussion)
         :controllers [{:parameters {:path [:id :share-hash]}
                        :start (fn [{:keys [path]}]
                                 (rf/dispatch [:handle-reload-on-discussion-loop (:id path) (:share-hash path)]))}]}]]]]]
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
     ;; TODO
     :view startpage-views/startpage-view}]])