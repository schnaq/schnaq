(ns schnaq.interface.routes
  (:require [clojure.walk :as walk]
            [oops.core :refer [oset!]]
            [re-frame.core :as rf]
            [reitit.coercion.spec]
            [reitit.frontend :as reitit-front]
            [reitit.frontend.easy :as reitit-front-easy]
            [reitit.frontend.history :as rfh]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.specs]
            [schnaq.interface.analytics.core :as analytics]
            [schnaq.interface.components.lexical.editor :as lexical]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.pages.start :refer [startpage]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.routing :as route-utils]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.admin.control-center :as admin-center]
            [schnaq.interface.views.discussion.moderation-center :as discussion-admin]
            [schnaq.interface.views.discussion.card-view :as discussion-card-view]
            [schnaq.interface.views.discussion.dashboard :as dashboard]
            [schnaq.interface.views.errors :as error-views]
            [schnaq.interface.views.feed.overview :as feed]
            [schnaq.interface.views.feedback.admin :as feedback-admin]
            [schnaq.interface.views.graph.view :as graph-view]
            [schnaq.interface.views.hub.overview :as hubs]
            [schnaq.interface.views.hub.settings :as hub-settings]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.presentation :as presentation]
            [schnaq.interface.views.qa.inputs :as qanda]
            [schnaq.interface.views.registration :as registration]
            [schnaq.interface.views.schnaq.create :as create]
            [schnaq.interface.views.schnaq.summary :as summary]
            [schnaq.interface.views.subscription :as subscription-views]
            [schnaq.interface.views.user.edit-account :as edit-account]
            [schnaq.interface.views.user.edit-notifications :as edit-notifications]
            [schnaq.interface.views.user.themes :as themes]
            [schnaq.interface.views.user.welcome :as welcome]
            [schnaq.links :as links]))

;; The controllers can be used to execute things at the start and the end of applying
;; the new route.

(defn language-controllers
  "Returns controllers for the desired locale switch and redirect."
  [locale]
  [{:start #(rf/dispatch [:language/switch locale])}])

(defn- check-for-fresh-pro
  "Checks whether a user is freshly subbed (via query in the success url in Stripe) and fire an event for matomo."
  [query]
  (when (= "true" (-> query :query :subbed))
    (matomo/track-event "User Upgrade" "Pro-Upgrade" "Stripe Transaction Success")))

;; IMPORTANT: Routes called here as views do not hot-reload for some reason. Only
;; components inside do regularly. So just use components here that wrap the view you
;; want to function regularly.

(defn- prefix-routes
  "Takes a vector of routes and prefixes any name with the provided prefix."
  [routes prefix]
  (walk/postwalk
   (fn [e]
     (if (and (map? e) (contains? e :name))
       (update e :name #(route-utils/prefix-route-name-locale % prefix))
       e))
   routes))

(def common-routes
  [["/"
    {:name :routes/startpage
     :view startpage
     :controllers [{:stop #(rf/dispatch [:schnaq.join.form/clear])}]}]
   ["/login"
    {:name :routes/login
     :view pages/login-page
     :link-text (labels :user/login)}]
   ["/hub/:keycloak-name"
    {:parameters {:path {:keycloak-name string?}}
     :controllers [{:parameters {:path [:keycloak-name]}
                    :start (fn [{:keys [path]}]
                             (rf/dispatch [:scheduler.after/login [:hub/load (:keycloak-name path)]])
                             (rf/dispatch [:hub/select! (:keycloak-name path)]))}]}
    [""
     {:name :routes/hub
      :view hubs/hub-overview}]
    ["/edit"
     {:name :routes.hub/edit
      :view hub-settings/settings}]]
   ["/user"
    ["/account"
     {:name :routes.user.manage/account
      :view edit-account/view
      :link-text (labels :user/edit-account)
      :controllers [{:start #(rf/dispatch [:scheduler.after/login [:user.subscription/status]])
                     :stop #(rf/dispatch [:user.picture/reset])}]}]
    ["/notifications"
     {:name :routes.user.manage/notifications
      :view edit-notifications/view
      :link-text (labels :user/edit-notifications)
      :controllers [{:stop (fn [] (rf/dispatch [:user.settings.temporary/reset]))}]}]]
   ["/welcome"
    ["" {:name :routes.welcome/free
         :view welcome/welcome-free-user-view}]
    ["/pro"
     {:name :routes.welcome/pro
      :view welcome/welcome-pro-user-view

      :controllers [{:parameters {:query [:subbed]}
                     :start (fn [query] (check-for-fresh-pro query))}]}]]
   ["/admin"
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
   ["/schnaqs"
    {:name :routes.schnaqs/personal
     :view feed/page
     :link-text (labels :router/visited-schnaqs)
     :controllers [{:parameters {:query [:filter]}
                    :start (fn []
                             (rf/dispatch [:schnaqs.visited/load])
                             (rf/dispatch [:hub/select! nil]))}]}]
   ["/schnaq"
    ["/create"
     {:name :routes.schnaq/create
      :view create/create-schnaq-view
      :link-text (labels :router/create-schnaq)}]
    ["/:share-hash"
     {:name :routes.schnaq.start/controller-init
      :parameters {:path {:share-hash :discussion/share-hash}
                   :query :ui.settings/schnaq}
      :controllers [{:parameters {:path [:share-hash]
                                  :query [:hide-discussion-options :hide-navbar
                                          :hide-footer :hide-input :num-rows
                                          :hide-input-replies :hide-activations]}
                     :start (fn [{:keys [path query]}]
                              (rf/dispatch [:ui.settings/parse-query-parameters query])
                              (rf/dispatch [:body.class/add "theming-enabled"])
                              (rf/dispatch [:schnaq/load-by-share-hash (:share-hash path)])
                              (rf/dispatch [:schnaq/add-visited! (:share-hash path)])
                              (rf/dispatch [:scheduler.after/login [:discussion.statements/mark-all-as-seen (:share-hash path)]]))
                     :stop (fn []
                             (rf/dispatch [:body.class/remove "theming-enabled"])
                             (rf/dispatch [:discussion.current/dissoc])
                             (rf/dispatch [:filters/clear])
                             (rf/dispatch [:theme/reset]))}]}
     ["" ;; When this route changes, reflect the changes in `schnaq.links.get-share-link`.
      {:controllers [{:parameters {:path [:share-hash]}
                      :start (fn []
                               (rf/dispatch [:schnaq.statements.current/dissoc])
                               (rf/dispatch [:discussion.history/clear])
                               (rf/dispatch [:updates.periodic.discussion/starting true])
                               (rf/dispatch [:discussion.query.conclusions/starting])
                               (rf/dispatch [:schnaq.polls/load-from-backend])
                               (rf/dispatch [:schnaq.activation/load-from-backend])
                               (rf/dispatch [:schnaq.search.current/clear-search-string]))
                      :stop (fn []
                              (rf/dispatch [:schnaq.statements.current/dissoc])
                              (rf/dispatch [:updates.periodic.discussion/starting false])
                              (rf/dispatch [:schnaq.activation/dissoc])
                              (rf/dispatch [:tour/stop false])
                              (rf/dispatch [:statement.edit/reset-edits])
                              (rf/dispatch [:visited.statement-ids/send-seen-statements-to-backend])
                              (rf/dispatch [:toggle-statement-content/clear!]))}]
       :name :routes.schnaq/start
       :view discussion-card-view/view
       :link-text (labels :router/start-discussion)}]
     ["/" ;; Redirect trailing slash schnaq access to non-trailing slash
      {:controllers [{:parameters {:path [:share-hash]}
                      :start (fn [{:keys [path]}]
                               (rf/dispatch [:navigation/navigate :routes.schnaq/start path]))}]}]
     ["/ask"
      {:name :routes.schnaq/qanda
       :view qanda/qanda-view
       :link-text (labels :router/qanda)
       :controllers [{:start (fn []
                               (rf/dispatch [:schnaq.activation/load-from-backend])
                               (rf/dispatch [:updates.periodic/activation true]))
                      :stop (fn []
                              (rf/dispatch [:updates.periodic/activation false])
                              (rf/dispatch [:schnaq.qa.search.results/reset]))}]}]
     ["/dashboard"
      {:name :routes.schnaq/dashboard
       :view dashboard/view
       :link-text (labels :router/dashboard)
       :controllers [{:parameters {:path [:share-hash]}
                      :start (fn [{:keys [path]}]
                               (rf/dispatch [:schnaq/load-by-share-hash (:share-hash path)])
                               (rf/dispatch [:discussion.query.conclusions/starting])
                               (rf/dispatch [:scheduler.after/login [:wordcloud/for-current-discussion]])
                               (rf/dispatch [:scheduler.after/login [:schnaq.summary/load]]))}]}]
     ["/manage"
      {:name :routes.schnaq/moderation-center
       :view discussion-admin/moderation-center-view
       :link-text (labels :router/last-added-schnaq)
       :controllers [{:start (fn []
                               (rf/dispatch [:scheduler.after/login [:themes.load/personal]]))}]}]
     ["/statement/:statement-id"
      {:name :routes.schnaq.select/statement
       :parameters {:path {:statement-id int?}}
       :view discussion-card-view/view
       :controllers [{:parameters {:path [:share-hash :statement-id]}
                      :start (fn []
                               (rf/dispatch [:discussion.query.statement/by-id])
                               (rf/dispatch [:schnaq.search.current/clear-search-string])
                               (rf/dispatch [:filters/clear]))
                      :stop (fn []
                              (rf/dispatch [:visited.statement-nums/to-localstorage])
                              (rf/dispatch [:visited.statement-ids/to-localstorage-and-merge-with-app-db])
                              (rf/dispatch [:visited.statement-ids/send-seen-statements-to-backend])
                              (rf/dispatch [:statement.edit/reset-edits])
                              (rf/dispatch [:toggle-statement-content/clear!]))}]}]
     ["/present/:entity-id"
      {:name :routes.present/entity
       :parameters {:path {:entity-id int?}}
       :view presentation/view
       :controllers [{:parameters {:path [:share-hash :entity-id]}
                      :start (fn []
                               (rf/dispatch [:updates.periodic.present/poll true])
                               (rf/dispatch [:schnaq.poll/load-from-query]))
                      :stop (fn []
                              (rf/dispatch [:updates.periodic.present/poll false]))}]}]
     ["/graph"
      {:name :routes/graph-view
       :view graph-view/graph-view-entrypoint
       :link-text (labels :router/graph-view)
       :controllers [{:identity (fn [] (random-uuid))
                      :start (fn []
                               (rf/dispatch [:updates.periodic/graph true])
                               (rf/dispatch [:graph/load-data-for-discussion]))
                      :stop (fn []
                              (rf/dispatch [:updates.periodic/graph false])
                              (rf/dispatch [:notifications/reset])
                              (rf/dispatch [:tour/stop false]))}]}]]]
   ["/subscription"
    ["/cancel" {:name :routes.subscription/cancel
                :view subscription-views/cancel-view}]
    ["/redirect/checkout"
     {:view pages/loading-page
      :name :routes.subscription.redirect/checkout
      :controllers [{:parameters {:query [:price-id]}
                     :start (fn [parameters]
                              (rf/dispatch [:scheduler.after/login [:subscription/create-checkout-session (get-in parameters [:query :price-id])]]))}]}]]
   ["/themes"
    {:name :routes.user.manage/themes
     :view themes/view
     :controllers [{:start (fn []
                             (rf/dispatch [:schnaq.selected/dissoc])
                             (rf/dispatch [:theme/dummy])
                             (rf/dispatch [:scheduler.after/login [:themes.load/personal]]))
                    :stop (fn []
                            (rf/dispatch [:schnaq.selected/dissoc])
                            (rf/dispatch [:themes/dissoc])
                            (rf/dispatch [:theme/reset])
                            (rf/dispatch [:tour/stop false]))}]}]
   ["/register"
    ["" {:name :routes.user/register
         :controllers [{:start (fn [parameters]
                                 (rf/dispatch [:user.currency/store (keyword (get-in parameters [:query :currency]))])
                                 (rf/dispatch [:keycloak/register (links/relative-to-absolute-url (navigation/href :routes.user.register/step-2))]))}]}]
    ["/step-2" {:name :routes.user.register/step-2
                :view registration/registration-step-2-view}]
    ["/step-3" {:name :routes.user.register/step-3
                :view registration/registration-step-3-view
                :controllers [{:start #(rf/dispatch [:pricing/get-prices])}]}]]
   (when-not shared-config/production?
     ["/playground/editor"
      {:name :routes.playground/editor
       :view lexical/playground
       :controllers [{:start #(rf/dispatch [:schnaq/share-hash "CAFECAFE-CAFE-CAFE-CAFE-CAFECAFECAFE"])}]}])
   ["/error"
    {:name :routes/cause-not-found
     :view error-views/not-found-view-stub
     :link-text (labels :router/not-found-label)
     :controllers [{:identity #(random-uuid)
                    :start #(.replace (.-location js/window) "/404")}]}]
   ["/beta-tester-only"
    {:name :routes/beta-only
     :view error-views/only-beta-tester}]
   ["/403"
    {:name :routes/forbidden-page
     :view error-views/forbidden-page}]
   ["/404"
    {:name :routes/true-404-view
     :view error-views/true-404-entrypoint
     :link-text (labels :router/true-404-view)}]])

(def routes
  (vec
   (concat
    [""
     {:coercion reitit.coercion.spec/coercion} ;; Enable Spec coercion for all routes
     (vec
      (concat
       ["/en"
        {:name :routes/english-prefix
         :controllers (language-controllers :en)}]
       (prefix-routes common-routes :en)))
     (vec
      (concat
       ["/de"
        {:name :routes/german-prefix
         :controllers (language-controllers :de)}]
       (prefix-routes common-routes :de)))]
    common-routes)))

(def router
  (reitit-front/router
   routes
   ;; This disables automatic conflict checking. So: Please check your own
   ;; routes that there are no conflicts.
   {:conflicts nil}))

(defn- on-navigate [new-match]
  (let [window-hash (.. js/window -location -hash)]
    (if (empty? window-hash)
      (.scrollTo js/window 0 0)
      (oset! js/document "onreadystatechange" #(tools/scroll-to-id window-hash))))
  (if new-match
    (rf/dispatch [:navigation/navigated new-match])
    (rf/dispatch [:navigation/navigate :routes/cause-not-found])))

(defn init-routes! []
  (reitit-front-easy/start!
   router
   on-navigate
   {:use-fragment false
    :ignore-anchor-click? (fn [router e el uri]
                            (and (rfh/ignore-anchor-click? router e el uri)
                                 (empty? (.-hash el))))}))

(defn parse-route
  "Parses a URL with the schnaq routes and returns the full match object."
  [url]
  (reitit-front/match-by-path router url))
