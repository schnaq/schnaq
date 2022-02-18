(ns schnaq.interface.routes
  (:require [clojure.walk :as walk]
            [oops.core :refer [oset!]]
            [re-frame.core :as rf]
            [reitit.coercion.spec]
            [reitit.frontend :as reitit-front]
            [reitit.frontend.easy :as reitit-front-easy]
            [reitit.frontend.history :as rfh]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.analytics.core :as analytics]
            [schnaq.interface.code-of-conduct :as coc]
            [schnaq.interface.integrations.wetog.routes :as wetog-routes]
            [schnaq.interface.pages.about-us :as about-us]
            [schnaq.interface.pages.lead-magnet :as lead-magnet]
            [schnaq.interface.pages.legal-note :as legal-note]
            [schnaq.interface.pages.press :as press]
            [schnaq.interface.pages.privacy :as privacy]
            [schnaq.interface.pages.privacy-extended :as privacy-extended]
            [schnaq.interface.pages.publications :as publications]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.routing :as route-utils]
            [schnaq.interface.views.admin.control-center :as admin-center]
            [schnaq.interface.views.discussion.admin-center :as discussion-admin]
            [schnaq.interface.views.discussion.card-view :as discussion-card-view]
            [schnaq.interface.views.discussion.dashboard :as dashboard]
            [schnaq.interface.views.errors :as error-views]
            [schnaq.interface.views.feed.overview :as feed]
            [schnaq.interface.views.feedback.admin :as feedback-admin]
            [schnaq.interface.views.graph.view :as graph-view]
            [schnaq.interface.views.hub.overview :as hubs]
            [schnaq.interface.views.hub.settings :as hub-settings]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.product.pages :as product-overview]
            [schnaq.interface.views.qa.inputs :as qanda]
            [schnaq.interface.views.schnaq.create :as create]
            [schnaq.interface.views.schnaq.summary :as summary]
            [schnaq.interface.views.startpage.core :as startpage-views]
            [schnaq.interface.views.startpage.pricing :as pricing-view]
            [schnaq.interface.views.subscription :as subscription-views]
            [schnaq.interface.views.user.edit-account :as edit-account]
            [schnaq.interface.views.user.edit-notifications :as edit-notifications]
            [schnaq.interface.views.user.themes :as themes]))

;; The controllers can be used to execute things at the start and the end of applying
;; the new route.

(defn language-controllers
  "Returns controllers for the desired locale switch and redirect."
  [locale]
  [{:start (fn []
             (rf/dispatch [:set-locale locale]))}])

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
     :view startpage-views/startpage-view
     :link-text (labels :router/startpage)
     :controllers [{:start #(rf/dispatch [:load-preview-statements])}]}]
   ["/product"
    [""
     {:name :routes/product-page
      :view product-overview/overview-view
      :link-text (labels :router/product)}]
    ["/qa"
     {:name :routes/product-page-qa
      :view product-overview/qa-view
      :link-text (labels :router/product-qa)}]
    ["/poll"
     {:name :routes/product-page-poll
      :view product-overview/poll-view
      :link-text (labels :router/product-poll)}]
    ["/activation"
     {:name :routes/product-page-activation
      :view product-overview/activation-view
      :link-text (labels :router/product-activation)}]]
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
   ["/code-of-conduct"
    {:name :routes/code-of-conduct
     :view coc/view
     :link-text (labels :router/code-of-conduct)}]
   ["/press"
    {:name :routes/press
     :view press/view}]
   ["/publications"
    {:name :routes/publications
     :view publications/view}]
   ["/schnaqs"
    {:name :routes.schnaqs/personal
     :view feed/page
     :link-text (labels :router/visited-schnaqs)
     :controllers [{:start (fn [_]
                             (rf/dispatch [:schnaqs.visited/load])
                             (rf/dispatch [:hub/select! nil]))}]}]
   ["/schnaq"
    ["/create"
     {:name :routes.schnaq/create
      :view create/create-schnaq-view
      :link-text (labels :router/create-schnaq)}]
    ["/:share-hash"
     {:name :routes.schnaq.start/controller-init
      :parameters {:path {:share-hash string?}}
      :controllers [{:parameters {:path [:share-hash]}
                     :start (fn [{:keys [path]}]
                              (rf/dispatch [:body.class/add "theming-enabled"])
                              (rf/dispatch [:schnaq/load-by-share-hash (:share-hash path)])
                              (rf/dispatch [:schnaq/add-visited! (:share-hash path)])
                              (rf/dispatch [:scheduler.after/login [:discussion.statements/mark-all-as-seen (:share-hash path)]])
                              (rf/dispatch [:scheduler.after/login [:discussion.statements/reload]]))
                     :stop (fn []
                             (rf/dispatch [:body.class/remove "theming-enabled"])
                             (rf/dispatch [:filters/clear])
                             (rf/dispatch [:schnaq.selected/dissoc]))}]}
     ["" ;; When this route changes, reflect the changes in `schnaq.links.get-share-link`.
      {:controllers [{:parameters {:path [:share-hash]}
                      :start (fn []
                               (rf/dispatch [:discussion.history/clear])
                               (rf/dispatch [:updates.periodic/starting-conclusions true])
                               (rf/dispatch [:discussion.query.conclusions/starting])
                               (rf/dispatch [:schnaq.polls/load-from-backend])
                               (rf/dispatch [:schnaq.activation/load-from-backend])
                               (rf/dispatch [:updates.periodic/polls true])
                               (rf/dispatch [:updates.periodic/activation true])
                               (rf/dispatch [:schnaq.search.current/clear-search-string]))
                      :stop (fn []
                              (rf/dispatch [:updates.periodic/starting-conclusions false])
                              (rf/dispatch [:updates.periodic/polls false])
                              (rf/dispatch [:updates.periodic/activation false])
                              (rf/dispatch [:schnaq.activation/dissoc])
                              (rf/dispatch [:statement.edit/reset-edits])
                              (rf/dispatch [:visited.statement-ids/send-seen-statements-to-backend])
                              (rf/dispatch [:toggle-replies/clear!])
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
                               (rf/dispatch [:scheduler.after/login [:wordcloud/for-current-discussion]])
                               (rf/dispatch [:scheduler.after/login [:schnaq.summary/load]]))}]}]
     ["/manage/:edit-hash"
      {:name :routes.schnaq/admin-center
       :view discussion-admin/admin-center-view
       :link-text (labels :router/last-added-schnaq)
       :parameters {:path {:edit-hash string?}}
       :controllers [{:parameters {:path [:share-hash :edit-hash]}
                      :start (fn [{:keys [path]}]
                               (let [{:keys [share-hash edit-hash]} path]
                                 (rf/dispatch [:scheduler.after/login [:themes.load/personal]])
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
                               (rf/dispatch [:discussion.query.statement/by-id])
                               (rf/dispatch [:schnaq.search.current/clear-search-string])
                               (rf/dispatch [:filters/clear]))
                      :stop (fn []
                              (rf/dispatch [:visited.statement-nums/to-localstorage])
                              (rf/dispatch [:visited.statement-ids/to-localstorage-and-merge-with-app-db])
                              (rf/dispatch [:visited.statement-ids/send-seen-statements-to-backend])
                              (rf/dispatch [:statement.edit/reset-edits])
                              (rf/dispatch [:toggle-replies/clear!])
                              (rf/dispatch [:toggle-statement-content/clear!]))}]}]
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
                              (rf/dispatch [:notifications/reset]))}]}]]]
   ["/pricing"
    {:name :routes/pricing
     :view pricing-view/pricing-view
     :link-text (labels :router/pricing)
     :controllers [{:start (fn []
                             (rf/dispatch [:load-preview-statements])
                             (rf/dispatch [:pricing/get-prices]))}]}]
   ["/subscription"
    ["/success" {:name :routes.subscription/success
                 :view subscription-views/success-view}]
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
                             (rf/dispatch [:theme/dummy])
                             (rf/dispatch [:scheduler.after/login [:themes.load/personal]]))
                    :stop (fn []
                            (rf/dispatch [:schnaq.selected/dissoc])
                            (rf/dispatch [:themes/dissoc])
                            (rf/dispatch [:theme/reset]))}]}]

   ["/privacy"
    [""
     {:name :routes.privacy/complete
      :view privacy-extended/view}]
    ["/overview"
     {:name :routes.privacy/simple
      :view privacy/view
      :link-text (labels :router/privacy)}]
    ;; Legacy route.
    ["/extended"
     {:name :routes.legacy/privacy-extended
      :controllers [{:start #(rf/dispatch [:navigation/navigate :routes.privacy/complete])}]}]]
   ["/about"
    {:name :routes/about-us
     :view about-us/page}]
   ["/legal-note"
    {:name :routes/legal-note
     :view legal-note/page}]
   ["/datenschutzkonform-arbeiten"
    {:name :routes/lead-magnet
     :view lead-magnet/view}]
   ["/error"
    {:name :routes/cause-not-found
     :view error-views/not-found-view-stub
     :link-text (labels :router/not-found-label)
     :controllers [{:identity #(random-uuid)
                    :start #(js-wrap/replace-url "/404")}]}]
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
   (if shared-config/embedded?
     wetog-routes/routes
     routes)
   ;; This disables automatic conflict checking. So: Please check your own
   ;; routes that there are no conflicts.
   {:conflicts nil}))

(defn- on-navigate [new-match]
  (let [window-hash (.. js/window -location -hash)]
    (when (not shared-config/embedded?)
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
   {:use-fragment shared-config/embedded?
    :ignore-anchor-click? (fn [router e el uri]
                            (and (rfh/ignore-anchor-click? router e el uri)
                                 (empty? (.-hash el))))}))

(defn parse-route
  "Parses a URL with the schnaq routes and returns the full match object."
  [url]
  (reitit-front/match-by-path router url))
