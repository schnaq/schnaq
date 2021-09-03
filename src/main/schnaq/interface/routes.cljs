(ns schnaq.interface.routes
  (:require [goog.object :as gobj]
            [oops.core :refer [oset!]]
            [re-frame.core :as rf]
            [reitit.coercion.spec]
            [reitit.frontend :as reitit-front]
            [reitit.frontend.easy :as reitit-front-easy]
            [reitit.frontend.history :as rfh]
            [schnaq.config.shared :as config]
            [schnaq.interface.analytics.core :as analytics]
            [schnaq.interface.code-of-conduct :as coc]
            [schnaq.interface.integrations.wetog.routes :as wetog-routes]
            [schnaq.interface.pages.about-us :as about-us]
            [schnaq.interface.pages.alphazulu :as az]
            [schnaq.interface.pages.lead-magnet :as lead-magnet]
            [schnaq.interface.pages.legal-note :as legal-note]
            [schnaq.interface.pages.press :as press]
            [schnaq.interface.pages.privacy :as privacy]
            [schnaq.interface.pages.privacy-extended :as privacy-extended]
            [schnaq.interface.pages.publications :as publications]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.admin.control-center :as admin-center]
            [schnaq.interface.views.discussion.admin-center :as discussion-admin]
            [schnaq.interface.views.discussion.card-view :as discussion-card-view]
            [schnaq.interface.views.discussion.dashboard :as dashboard]
            [schnaq.interface.views.discussion.search :as discussion-search]
            [schnaq.interface.views.errors :as error-views]
            [schnaq.interface.views.feed.overview :as feed]
            [schnaq.interface.views.feedback.admin :as feedback-admin]
            [schnaq.interface.views.graph.view :as graph-view]
            [schnaq.interface.views.howto.how-to :as how-to]
            [schnaq.interface.views.hub.overview :as hubs]
            [schnaq.interface.views.hub.settings :as hub-settings]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.schnaq.create :as create]
            [schnaq.interface.views.schnaq.summary :as summary]
            [schnaq.interface.views.schnaq.value :as value]
            [schnaq.interface.views.startpage.core :as startpage-views]
            [schnaq.interface.views.startpage.pricing :as pricing-view]
            [schnaq.interface.views.user.edit-account :as edit-account]))

;; The controllers can be used to execute things at the start and the end of applying
;; the new route.

(def ^:private schnaq-start-controllers
  [{:parameters {:path [:share-hash]}
    :start (fn []
             (rf/dispatch [:discussion.history/clear])
             (rf/dispatch [:updates.periodic/starting-conclusions true])
             (rf/dispatch [:discussion.query.conclusions/starting]))
    :stop (fn []
            (rf/dispatch [:updates.periodic/starting-conclusions false])
            (rf/dispatch [:statement.edit/reset-edits]))}])

(defn language-controllers
  "Returns controllers for the desired locale switch and redirect."
  [locale]
  [{:parameters {:path [:rest-url]}
    :start (fn [{:keys [path]}]
             (rf/dispatch [:language/set-and-redirect locale (:rest-url path)]))}])

;; IMPORTANT: Routes called here as views do not hot-reload for some reason. Only
;; components inside do regularly. So just use components here that wrap the view you
;; want to function regularly.
(def routes
  ["/"
   {:coercion reitit.coercion.spec/coercion}                ;; Enable Spec coercion for all routes
   ["en/{*rest-url}"
    {:name :routes/force-english
     :controllers (language-controllers :en)}]
   ["de/{*rest-url}"
    {:name :routes/force-german
     :controllers (language-controllers :de)}]
   [""
    {:name :routes/startpage
     :view startpage-views/startpage-view
     :link-text (labels :router/startpage)}]
   ["alphazulu"
    {:name :routes/alphazulu
     :view az/view}]
   ["login"
    {:name :routes/login
     :view pages/login-page
     :link-text (labels :user/login)}]
   ["hub/:keycloak-name"
    {:parameters {:path {:keycloak-name string?}}
     :controllers [{:parameters {:path [:keycloak-name]}
                    :start (fn [{:keys [path]}]
                             (rf/dispatch [:scheduler.after/login [:hub/load (:keycloak-name path)]]))}]}
    ["/"
     {:name :routes/hub
      :view hubs/hub-overview}]
    ["/edit"
     {:name :routes.hub/edit
      :view hub-settings/settings}]]
   ["user"
    ["/account"
     {:name :routes.user.manage/account
      :view edit-account/view
      :link-text (labels :user/edit-account)
      :controllers [{:stop (fn [] (rf/dispatch [:user.picture/reset]))}]}]]
   ["admin"
    ["/center"
     {:name :routes/admin-center
      :view admin-center/center-overview-route
      :link-text (labels :router/admin-center)}]
    ["/feedbacks"
     {:name :routes/feedbacks
      :view feedback-admin/feedbacks-view
      :link-text (labels :router/all-feedbacks)
      :controllers [{:start (fn [] (rf/dispatch [:scheduler.after/login [:feedbacks/fetch]]))}]}]
    ["/analytics"
     {:name :routes/analytics
      :view analytics/analytics-dashboard-entrypoint
      :link-text (labels :router/analytics)
      :controllers [{:start (fn [] (rf/dispatch [:scheduler.after/login [:analytics/load-dashboard]]))}]}]
    ["/summaries"
     {:name :routes.admin/summaries
      :view summary/admin-summaries-view
      :controllers [{:start (fn [] (rf/dispatch [:scheduler.after/login [:summaries/load-all]]))}]}]]
   ["code-of-conduct"
    {:name :routes/code-of-conduct
     :view coc/view
     :link-text (labels :router/code-of-conduct)}]
   ["how-to"
    {:name :routes/how-to
     :view how-to/view
     :link-text (labels :router/how-to)}]
   ["press"
    {:name :routes/press
     :view press/view}]
   ["publications"
    {:name :routes/publications
     :view publications/view}]
   ["schnaqs"
    {:name :routes.schnaqs/personal
     :view feed/page
     :link-text (labels :router/visited-schnaqs)
     :controllers [{:start #(rf/dispatch [:schnaqs.visited/load])}]}]
   ["schnaq"
    {:controllers [{:start #(rf/dispatch [:username/open-dialog])}]}
    ["/create"
     {:name :routes.schnaq/create
      :view create/create-schnaq-view
      :link-text (labels :router/create-schnaq)}]
    ["/:share-hash"
     {:parameters {:path {:share-hash string?}}
      :controllers [{:parameters {:path [:share-hash]}
                     :start (fn [{:keys [path]}]
                              (rf/dispatch [:schnaq/load-by-share-hash (:share-hash path)]))}]}
     [""                                                    ;; When this route changes, reflect the changes in `schnaq.links.get-share-link`.
      {:controllers schnaq-start-controllers
       :name :routes.schnaq/start
       :view discussion-card-view/view
       :link-text (labels :router/start-discussion)}]
     ["/"                                                   ;; Redirect trailing slash schnaq access to non-trailing slash
      {:controllers [{:parameters {:path [:share-hash]}
                      :start (fn [{:keys [path]}]
                               (rf/dispatch [:navigation/navigate :routes.schnaq/start path]))}]}]
     ["/value"
      {:name :routes.schnaq/value
       :view value/schnaq-value-view
       :link-text (labels :router/value)}]
     ["/search"
      {:name :routes.search/schnaq
       :view discussion-search/view}]
     ["/dashboard"
      {:name :routes.schnaq/dashboard
       :view dashboard/view
       :link-text (labels :router/dashboard)
       :controllers [{:parameters {:path [:share-hash]}
                      :start (fn [{:keys [path]}]
                               (rf/dispatch [:schnaq/load-by-share-hash (:share-hash path)])
                               (rf/dispatch [:scheduler.after/login [:schnaq.summary/load]]))}]}]
     ["/summary"
      {:name :routes.schnaq/summary
       :view summary/public-user-view
       :controllers [{:parameters {:path [:share-hash]}
                      :start (fn []
                               (rf/dispatch [:scheduler.after/login [:schnaq.summary/load]]))}]}]
     ["/manage/:edit-hash"
      {:name :routes.schnaq/admin-center
       :view discussion-admin/admin-center-view
       :link-text (labels :router/last-added-schnaq)
       :parameters {:path {:edit-hash string?}}
       :controllers [{:parameters {:path [:share-hash :edit-hash]}
                      :start (fn [{:keys [path]}]
                               (let [{:keys [share-hash edit-hash]} path]
                                 (rf/dispatch [:schnaq/check-admin-credentials share-hash edit-hash])
                                 (rf/dispatch [:schnaq/load-by-hash-as-admin share-hash edit-hash])
                                 (rf/dispatch [:schnaqs.save-admin-access/to-localstorage-and-db
                                               share-hash edit-hash])))}]}]
     ["/statement/:statement-id"
      {:name :routes.schnaq.select/statement
       :parameters {:path {:statement-id int?}}
       :view discussion-card-view/view
       :controllers [{:parameters {:path [:share-hash :statement-id]}
                      :start (fn []
                               (rf/dispatch [:discussion.query.statement/by-id]))
                      :stop (fn [{:keys [path]}]
                              (let [{:keys [share-hash]} path]
                                (rf/dispatch [:visited.statement-nums/to-localstorage])
                                (rf/dispatch [:visited.statement-ids/to-localstorage-and-merge-with-app-db])
                                (rf/dispatch [:visited.statement-ids/send-seen-statements-to-backend share-hash])
                                (rf/dispatch [:statement.edit/reset-edits])))}]}]
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
                              (rf/dispatch [:updates.periodic/graph false])
                              (rf/dispatch [:notifications/reset]))}]}]]]
   ["pricing"
    {:name :routes/pricing
     :view pricing-view/pricing-view
     :link-text (labels :router/pricing)}]
   ["privacy"
    [""
     {:name :routes/privacy
      :view privacy/view
      :link-text (labels :router/privacy)}]
    ["/extended"
     {:name :routes/privacy-extended
      :view privacy-extended/view}]]
   ["about"
    {:name :routes/about-us
     :view about-us/page}]
   ["legal-note"
    {:name :routes/legal-note
     :view legal-note/page}]
   ["datenschutzkonform-arbeiten"
    {:name :routes/lead-magnet
     :view lead-magnet/view}]
   ["error"
    {:name :routes/cause-not-found
     :view error-views/not-found-view-stub
     :link-text (labels :router/not-found-label)
     :controllers [{:identity #(random-uuid)
                    :start #(js-wrap/replace-url "/404")}]}]
   ["403"
    {:name :routes/forbidden-page
     :view error-views/forbidden-page}]
   ["beta-tester-only"
    {:name :routes/beta-only
     :view error-views/only-beta-tester}]
   ["404"
    {:name :routes/true-404-view
     :view error-views/true-404-entrypoint
     :link-text (labels :router/true-404-view)}]])

(def router
  (reitit-front/router
    (if config/embedded?
      wetog-routes/routes
      routes)
    ;; This disables automatic conflict checking. So: Please check your own
    ;; routes that there are no conflicts.
    {:conflicts nil}))

(defn- on-navigate [new-match]
  (let [window-hash (.. js/window -location -hash)]
    (when (not config/embedded?)
      (if (empty? window-hash)
        (.scrollTo js/window 0 0)
        (oset! js/document "onreadystatechange"
               #(js-wrap/scroll-to-id window-hash)))))
  (if new-match
    (rf/dispatch [:navigation/navigated new-match])
    (rf/dispatch [:navigation/navigate :routes/cause-not-found])))

(defn init-routes! []
  (reitit-front-easy/start!
    router
    on-navigate
    {:use-fragment config/embedded?
     :ignore-anchor-click? (fn [router e el uri]
                             (and (rfh/ignore-anchor-click? router e el uri)
                                  (not= "false" (gobj/get (.-dataset el) "reititHandleClick"))
                                  ;; Ignore anchor-click when there is no fragment present e.g. schnaq.com/#newsletter
                                  (empty? (.-hash el))))}))

(defn parse-route
  "Parses a URL with the schnaq routes and returns the full match object."
  [url]
  (reitit-front/match-by-path router url))
