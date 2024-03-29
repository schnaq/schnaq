(ns schnaq.interface.views.discussion.card-elements
  (:require ["react-bootstrap" :refer [Button]]
            [clojure.string :as cstring]
            [goog.functions :as gfun]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.config :as config]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.markdown :refer [as-markdown]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.filters :as filters]
            [schnaq.shared-toolbelt :as shared-tools]
            [schnaq.user :as user-utils]))

(defn- back-button
  "Return to your schnaqs Button"
  []
  (let [has-history? (seq @(rf/subscribe [:discussion-history]))
        back-feed (toolbelt/current-overview-navigation-route)
        back-history [:discussion.history/time-travel 1]
        back-label (if has-history?
                     (labels :history.back/label)
                     (labels :history.all-schnaqs/label))
        navigation-target (if has-history? back-history back-feed)
        tooltip (if has-history? :history.back/tooltip :history.all-schnaqs/tooltip)]
    (when navigation-target
      [:div.d-flex.flex-row.panel-white-sm
       [tooltip/text
        (labels tooltip)
        [:button.btn.btn-dark
         {:on-click #(rf/dispatch navigation-target)}
         [:div.d-flex
          [icon :arrow-left "m-auto"]]]]
       [:small.my-auto.ms-2 back-label]])))

(defn- discussion-start-button
  "Discussion start button for history view"
  []
  (let [schnaq @(rf/subscribe [:schnaq/selected])
        title (:discussion/title schnaq)]
    [:a.text-decoration-none
     {:href (navigation/href :routes.schnaq/start {:share-hash (:discussion/share-hash schnaq)})}
     [:div.clickable.card-history-home.text-dark
      [tooltip/text
       (labels :history.home/tooltip)
       [:div.text-center
        [:h6 title]
        [:p.text-muted.mb-0 (labels :history.home/text)]
        [badges/static-info-badges]]
       {:placement :right}]]]))

(defn history-card
  "A single history-card comprising the history together"
  [index statement-id]
  (let [max-word-count 20
        statement @(rf/subscribe [:schnaq/statement statement-id])
        nickname (user-utils/statement-author statement)
        user (:statement/author statement)
        statement-content (:statement/content statement)
        tooltip-text (gstring/format "%s %s" (labels :tooltip/history-statement) nickname)
        history-content [:div
                         [:div.d-flex.flex-row
                          [:h6 (labels :history.statement/user) " " (toolbelt/truncate-to-n-chars nickname 20)]
                          [:div.ms-auto [common/avatar :size 22 :user user]]]
                         (as-markdown (toolbelt/truncate-to-n-words statement-content max-word-count))]]
    [:article
     [:div.history-thread-line]
     [:div.d-inline-block.d-md-block.text-dark.w-100
      (let [attitude (name (or (:statement/type statement) :neutral))]
        [:div.card-history.clickable.w-100
         {:on-click #(rf/dispatch [:discussion.history/time-travel index])}
         [:div.d-flex.flex-row
          [:div {:class (str "highlight-card-" attitude)}]
          [:div.history-card-content
           (if (zero? index)
             history-content
             [tooltip/text tooltip-text history-content {:placement :right}])]]])]]))

(defn history-view
  "History view displayed in the left column in the desktop view."
  []
  (let [history-ids @(rf/subscribe [:discussion-history])
        indexed-history (map-indexed #(vector (- (count history-ids) %1 1) %2) history-ids)
        has-history? (seq indexed-history)]
    (when has-history?
      [:section.history-wrapper
       [:h5.p-2.text-center (labels :history/title)]
       [discussion-start-button]
       ;; history
       (for [[index statement-id] indexed-history]
         (with-meta [history-card index statement-id]
           {:key (str "history-" statement-id)}))])))

(rf/reg-event-fx
 :discussion.add.statement/starting
 (fn [{:keys [db]} [_ statement-text locked?]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         username (get-in db [:user :names :display])
         rand-id (rand-int 9999999)]
     {:db (-> db
              (update-in [:schnaq :selected :meta-info :all-statements] inc)
              (assoc-in [:schnaq :statements rand-id] {:db/id rand-id
                                                       :statement/author {:user/nickname username}
                                                       :statement/version 1
                                                       :statement/content statement-text
                                                       :statement/locked? locked?})
              (update-in [:schnaq :statement-slice :current-level] (comp set conj) rand-int))
      :fx [(http/xhrio-request db :post "/discussion/statements/starting/add"
                               [:discussion.add.statement.starting/success]
                               {:statement statement-text
                                :share-hash share-hash
                                :display-name (toolbelt/current-display-name db)
                                :locked? locked?}
                               [:ajax.error/as-notification])]})))

(rf/reg-event-fx
 :discussion.add.statement.starting/success
 (fn [{:keys [db]} [_ new-starting-statements]]
   (let [starting-conclusion (:starting-conclusion new-starting-statements)
         starting-id (:db/id starting-conclusion)
         statement-with-creation-secret? (:statement/creation-secret starting-conclusion)
         share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:db (-> db
              (assoc-in [:schnaq :statements starting-id] starting-conclusion)
              (update-in [:schnaq :statement-slice :current-level] conj starting-id)
              (update-in [:visited :statement-ids share-hash] #(set (conj %1 %2)) starting-id))
      :fx [[:dispatch [:notification/add
                       #:notification{:title (labels :discussion.notification/new-content-title)
                                      :body (labels :discussion.notification/new-content-body)
                                      :context :success}]]
           [:dispatch [:votes.local/reset]]
           [:dispatch [:schnaq.wordcloud/from-current-premises]]
           (when statement-with-creation-secret?
             [:dispatch [:discussion.statements/add-creation-secret starting-conclusion]])]})))

(rf/reg-event-fx
 :discussion.query.conclusions/starting
 (fn [{:keys [db]} _]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:fx [(http/xhrio-request db :get "/discussion/conclusions/starting"
                               [:discussion.query.conclusions/set-starting]
                               {:share-hash share-hash
                                :display-name (toolbelt/current-display-name db)})]})))

(rf/reg-event-fx
 :discussion.query.conclusions/set-starting
 (fn [{:keys [db]} [_ {:keys [starting-conclusions children]}]]
   (when starting-conclusions
     (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
           statement-ids (map :db/id starting-conclusions)]
       {:db (-> db
                (update-in [:schnaq :statements] merge
                           (shared-tools/normalize :db/id (concat starting-conclusions children)))
                (assoc-in [:schnaq :statement-slice :current-level] statement-ids)
                (update-in [:visited :statement-ids share-hash] #(set (concat %1 %2)) statement-ids))
        ;; hier die seen setzen
        :fx [[:dispatch [:votes.local/reset]]
             [:dispatch [:schnaq.wordcloud/from-current-premises]]]}))))

;; -----------------------------------------------------------------------------

(defn- sort-options
  "Displays the different sort options for card elements."
  []
  (let [sort-method @(rf/subscribe [:discussion.statements/sort-method])]
    [tooltip/text (labels :badges/sort)
     (if (= :newest sort-method)
       [:button.btn.btn-sm.btn-primary.btn-invisible-border
        {:on-click #(rf/dispatch [:discussion.statements.sort/set :popular])}
        (labels :badges.sort/newest)]
       [:button.btn.btn-sm.btn-primary.btn-invisible-border
        {:on-click #(rf/dispatch [:discussion.statements.sort/set :newest])}
        (labels :badges.sort/popular)])]))

(defn- question-filter-button
  "Question filter."
  []
  (let [active? @(rf/subscribe [:filters/questions?])]
    [tooltip/text "Nur Fragen anzeigen"
     [:button.btn.btn-sm
      {:on-click (if active?
                   #(rf/dispatch [:filters.deactivate/questions])
                   #(rf/dispatch [:filters.activate/questions]))
       :class (if active? "btn-primary btn-invisible-border" "btn-outline-primary")}
      (labels :filters.option/questions)]]))

;; -----------------------------------------------------------------------------

(rf/reg-event-db
 :discussion.statements.sort/set
 (fn [db [_ method]]
   (assoc-in db [:discussion :statements :sort-method] method)))

(rf/reg-sub
 :discussion.statements/sort-method
 (fn [db _]
   (get-in db [:discussion :statements :sort-method] :newest)))

(def throttled-in-schnaq-search
  (gfun/throttle
   #(rf/dispatch [:discussion.statements/search (oget % [:target :value])])
   500))

(defn- search-clear-button
  [clear-id]
  (let [search-string @(rf/subscribe [:schnaq.search.current/search-string])
        action-icon (if (cstring/blank? search-string) :search :times)]
    [:button.btn.button-muted.py-0
     {:on-click (fn [_e]
                  (toolbelt/clear-input clear-id)
                  (rf/dispatch [:schnaq.search.current/clear-search-string]))}
     [icon action-icon]]))

(defn search-bar
  "A search-bar to search inside a schnaq."
  []
  (let [search-input-id "search-bar"
        route-name @(rf/subscribe [:navigation/current-route-name])
        selected-statement-id (get-in @(rf/subscribe [:navigation/current-route]) [:path-params :statement-id])]
    [:form.my-auto
     {:on-submit #(.preventDefault %)
      :key (str route-name selected-statement-id)}
     [:div.input-group.search-bar.panel-white.p-0
      [:input.form-control.my-auto.search-bar-input.py-0
       {:id search-input-id
        :type "text"
        :aria-label "Search"
        :placeholder (labels :schnaq.search/input)
        :name "search-input"
        :on-key-up throttled-in-schnaq-search}]
      [search-clear-button search-input-id]]]))

(rf/reg-sub
 :ui/setting
 (fn [db [_ field]]
   (get-in db [:ui :settings field])))

(rf/reg-event-db
 :ui.settings/parse-query-parameters
 (fn [db [_ query]]
   (assoc-in db [:ui :settings] query)))

(defn discussion-options-navigation
  "Navigation bar on top of the discussion contents."
  []
  (when-not @(rf/subscribe [:ui/setting :hide-discussion-options])
    [:div.d-flex.flex-row.pt-1.pt-xl-0
     (when-not config/in-iframe?
       [:div.me-auto [back-button]])
     [tooltip/html
      [:section.px-1
       [:div.d-flex.flex-row.py-2
        [:div.pe-1 [sort-options]]
        [question-filter-button]]
       (when @(rf/subscribe [:routes.schnaq/start?])
         [filters/filter-answered-statements])
       [:div.py-3 [search-bar]]]
      [:> Button {:variant "outline-primary" :size :sm :className "panel-white-sm"}
       (labels :discussion.navbar/discussion-settings)]
      {:appendTo js/document.body}]]))

(defn locked-statement-icon
  "Indicator that a statement is locked."
  ([]
   [locked-statement-icon nil])
  ([statement-id]
   [tooltip/text
    (labels :statement.locked/tooltip)
    [:span.badge.rounded-pill
     (when (and statement-id @(rf/subscribe [:user/moderator?]))
       {:class "clickable"
        :on-click #(rf/dispatch [:statement.lock/toggle statement-id false])})
     [icon :lock "text-primary"]]]))

(defn pinned-statement-icon
  "Indicator that a statement is pinned. Click it to unpin, if moderator and beta-user."
  [statement-id]
  [tooltip/text
   (labels :statement.pinned/tooltip)
   [:span.badge.rounded-pill
    (when (and statement-id @(rf/subscribe [:user/pro?]) @(rf/subscribe [:user/moderator?]))
      {:class "clickable"
       :on-click #(rf/dispatch [:statement.pin/toggle statement-id false])})
    [icon :pin "text-primary"]]])

(rf/reg-sub
 :schnaq.search.current/search-string
 (fn [db _]
   (get-in db [:search :schnaq :current :search-string] "")))

(rf/reg-event-db
 :schnaq.search.current/clear-search-string
 (fn [db _]
   (update-in db [:search :schnaq :current] dissoc :search-string)))

(rf/reg-sub
 :schnaq.search.current/result
 (fn [db _]
   (get-in db [:search :schnaq :current :result] [])))
