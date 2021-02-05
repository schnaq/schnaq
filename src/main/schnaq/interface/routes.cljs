(ns schnaq.interface.routes
  (:require [re-frame.core :as rf]
            [reitit.coercion.spec]
            [schnaq.interface.analytics.core :as analytics]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.admin.control-center :as admin-center]
            [schnaq.interface.views.brainstorm.create :as brainstorm-create]
            [schnaq.interface.code-of-conduct :as coc]
            [schnaq.interface.views.discussion.card-view :as discussion-card-view]
            [schnaq.interface.views.errors :as error-views]
            [schnaq.interface.views.feed.overview :as feed]
            [schnaq.interface.views.feedback.admin :as feedback-admin]
            [schnaq.interface.views.meeting.admin-center :as meeting-admin]
            [schnaq.interface.views.meeting.overview :as meetings-overview]
            [schnaq.interface.views.privacy :as privacy]
            [schnaq.interface.views.startpage.core :as startpage-views]
            [schnaq.interface.views.startpage.pricing :as pricing-view]
            [schnaq.interface.views.graph.view :as graph-view]))

;; The controllers can be used to execute things at the start and the end of applying
;; the new route.

(def ^:private schnaq-start-controllers
  [{:parameters {:path [:share-hash]}
    :start (fn []
             (rf/dispatch [:discussion.history/clear])
             (rf/dispatch [:updates.periodic/starting-conclusions true])
             (rf/dispatch [:discussion.query.conclusions/starting]))
    :stop (fn []
            (rf/dispatch [:updates.periodic/starting-conclusions false]))}])

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
   ["admin/center"
    {:name :routes/admin-center
     :view admin-center/center-overview-route
     :link-text (labels :router/admin-center)
     :controllers [{:start (fn []
                             (rf/dispatch [:admin/set-password (js/prompt "Admin Password")])
                             (rf/dispatch [:schnaqs.public/load]))}]}]
   ["code-of-conduct"
    {:name :routes/code-of-conduct
     :view coc/view
     :link-text (labels :router/code-of-conduct)}]
   ["schnaqs"
    ["/public"
     {:name :routes/public-discussions
      :view feed/public-discussions-view
      :link-text (labels :router/public-discussions)
      :controllers [{:start (fn [] (rf/dispatch [:schnaqs.public/load]))}]}]
    ["/my"
     {:name :routes.meetings/my-schnaqs
      :view feed/personal-discussions-view
      :link-text (labels :router/my-schnaqs)
      :controllers [{:start (fn [] (rf/dispatch [:schnaqs.visited/load]))}]}]]
   ["schnaq"
    ["/create"
     {:name :routes.brainstorm/create
      :view brainstorm-create/create-brainstorm-view
      :link-text (labels :router/create-brainstorm)
      :controllers [{:start (fn [_] (rf/dispatch [:username/open-dialog]))}]}]
    ["/:share-hash"
     {:parameters {:path {:share-hash string?}}
      :controllers [{:parameters {:path [:share-hash]}
                     :start (fn [{:keys [path]}]
                              (rf/dispatch [:schnaq/load-by-share-hash (:share-hash path)]))}]}
     ["/"
      {:controllers schnaq-start-controllers
       :name :routes.schnaq/start
       :view discussion-card-view/view
       :link-text (labels :router/start-discussion)}]
     ["/statement/:statement-id"
      {:name :routes.schnaq.select/statement
       :parameters {:path {:statement-id int?}}
       :view discussion-card-view/view
       :controllers [{:parameters {:path [:share-hash :statement-id]}
                      :start (fn []
                               (rf/dispatch [:discussion.query.statement/by-id]))
                      :stop (fn []
                              (rf/dispatch [:visited.statement-nums/to-localstorage]))}]}]
     ["/graph"
      {:name :routes/graph-view
       :view graph-view/graph-view-entrypoint
       :link-text (labels :router/graph-view)
       :controllers [{:identity (fn [] (random-uuid))
                      :start (fn []
                               (rf/dispatch [:spinner/active! true])
                               (rf/dispatch [:updates.periodic/graph true])
                               (rf/dispatch [:graph/load-data-for-discussion]))
                      :stop (fn []
                              (rf/dispatch [:updates.periodic/graph false]))}]}]]]
   ["pricing"
    {:name :routes/pricing
     :view pricing-view/pricing-view
     :link-text (labels :router/pricing)}]
   ["privacy"
    {:name :routes/privacy
     :view privacy/view
     :link-text (labels :router/privacy)}]
   ["meetings"
    {:controllers [{:start (fn [_] (rf/dispatch [:username/open-dialog]))}]}
    (when-not toolbelt/production?
      [""
       {:name :routes/meetings
        :view meetings-overview/meeting-view-entry
        :link-text (labels :router/all-meetings)}])
    ["/:share-hash"
     {:parameters {:path {:share-hash string?}}
      :controllers [{:parameters {:path [:share-hash]}
                     :start (fn [{:keys [path]}]
                              (rf/dispatch [:schnaq/load-by-share-hash (:share-hash path)]))}]}
     ["/:edit-hash"
      {:parameters {:path {:edit-hash string?}}
       :controllers [{:parameters {:path [:share-hash :edit-hash]}
                      :start (fn [{:keys [path]}]
                               (let [{:keys [share-hash edit-hash]} path]
                                 (rf/dispatch [:meeting/check-admin-credentials share-hash edit-hash])))}]}
      ["/manage"
       {:name :routes.meeting/admin-center
        :view meeting-admin/admin-center-view
        :link-text (labels :router/meeting-created)
        :controllers [{:parameters {:path [:share-hash :edit-hash]}
                       :start (fn [{:keys [path]}]
                                (let [share-hash (:share-hash path)
                                      edit-hash (:edit-hash path)]
                                  (rf/dispatch [:schnaq/load-by-hash-as-admin share-hash edit-hash])
                                  (rf/dispatch [:schnaqs.save-admin-access/to-localstorage
                                                share-hash edit-hash])))}]}]]
     ["/"
      ;; DEPRECATED: Do not use at all. This has the same effect as `:routes.schnaq/start`
      {:name :routes.meeting/show
       :view discussion-card-view/view
       :link-text (labels :router/show-single-meeting)
       :controllers schnaq-start-controllers}]
     ["/agenda"
      ["/:id"
       {:parameters {:path {:id int?}}}
       ["/discussion"
        ["/start"
         ;; DEPRECATED: Use the shorter `:routes.schnaq/start`
         {:controllers [{:parameters {:path [:share-hash :id]}
                         :start (fn []
                                  (rf/dispatch [:discussion.history/clear])
                                  (rf/dispatch [:updates.periodic/starting-conclusions true])
                                  (rf/dispatch [:discussion.query.conclusions/starting]))
                         :stop (fn []
                                 (rf/dispatch [:updates.periodic/starting-conclusions false]))}]
          :name :routes.discussion/start
          :view discussion-card-view/view
          :link-text (labels :router/start-discussion)}]
        ["/selected/:statement-id"
         ;; DEPRECATED: Use the shorter `:routes.schnaq.select/statement`
         {:name :routes.discussion.select/statement
          :parameters {:path {:statement-id int?}}
          :view discussion-card-view/view
          :controllers [{:parameters {:path [:share-hash :id :statement-id]}
                         :start (fn []
                                  (rf/dispatch [:discussion.query.statement/by-id]))}]}]]
       ["/graph"
        ;; DEPRECATED: Use the shorter `:routes/graph-view`
        {:name :routes/graph-view-old
         :view graph-view/graph-view-entrypoint
         :link-text (labels :router/graph-view)
         :controllers [{:identity (fn [] (random-uuid))
                        :start (fn []
                                 (rf/dispatch [:updates.periodic/graph true])
                                 (rf/dispatch [:graph/load-data-for-discussion]))
                        :stop (fn []
                                (rf/dispatch [:updates.periodic/graph false]))}]}]]]]]
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