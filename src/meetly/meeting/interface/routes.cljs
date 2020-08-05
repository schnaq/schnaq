(ns meetly.meeting.interface.routes
  (:require [meetly.meeting.interface.views.startpage :as startpage-views]
            [meetly.meeting.interface.views.agenda :as agenda-views]
            [meetly.meeting.interface.views.meeting.meetings :as meeting-views]
            [meetly.meeting.interface.views.meeting.overview :as meetings-overview]
            [meetly.meeting.interface.views.meeting.single :as meeting-single]
            [meetly.meeting.interface.views.discussion :as discussion-views]
            [meetly.meeting.interface.config :refer [config]]
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
   ["meetings"
    (when (not= "production" (:environment config))
      [""
       {:name :routes/meetings
        :view meetings-overview/meeting-view
        :link-text "Meetings"}])
    ["/create"
     {:name :routes/meetings.create
      :view meeting-views/create-meeting-form-view
      :link-text "Create Meeting"}]
    ["/:share-hash"
     {:parameters {:path {:share-hash string?}}
      :controllers [{:parameters {:path [:share-hash]}
                     :start (fn [{:keys [path]}]
                              (let [hash (:share-hash path)]
                                (rf/dispatch [:load-meeting-by-share-hash hash])))}]}
     ["/"
      {:name :routes/meetings.show
       :view meeting-single/single-meeting-view
       :link-text "Show Meeting"
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
        :link-text "Add Agenda"}]
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
         :view discussion-views/discussion-start-view}]
       ["/continue"
        {:name :routes/meetings.discussion.continue
         :view discussion-views/discussion-loop-view
         :controllers [{:parameters {:path [:id :share-hash]}
                        :start (fn [{:keys [path]}]
                                 (rf/dispatch [:handle-reload-on-discussion-loop (:id path) (:share-hash path)]))}]}]]]]]
   ["startpage"
    {:name :routes/startpage
     :view startpage-views/startpage-view
     :link-text "Meetly"}]])