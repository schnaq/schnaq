(ns schnaq.interface.views.discussion.card-elements
  (:require [clojure.string :as cstring]
            [goog.functions :as gfun]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.conclusion-card :as cards]
            [schnaq.interface.views.discussion.filters :as filters]
            [schnaq.interface.views.howto.elements :as how-to-elements]
            [schnaq.shared-toolbelt :as shared-tools]
            [schnaq.user :as user-utils]))

(defn- back-button
  "Return to your schnaqs Button"
  []
  (let [history @(rf/subscribe [:discussion-history])
        has-history? (seq history)
        back-feed (toolbelt/current-overview-navigation-route)
        back-history [:discussion.history/time-travel 1]
        back-label (if has-history?
                     (labels :history.back/label)
                     (labels :history.all-schnaqs/label))
        navigation-target (cond
                            has-history? back-history
                            shared-config/embedded? nil
                            :else back-feed)
        tooltip (if has-history? :history.back/tooltip :history.all-schnaqs/tooltip)]
    (when navigation-target
      [:div.d-flex.flex-row.h-100
       [tooltip/text
        (labels tooltip)
        [:button.btn.btn-dark.button-discussion-options.h-100.p-3
         {:on-click #(rf/dispatch navigation-target)}
         [:div.d-flex
          [icon :arrow-left "m-auto"]]]]
       [:small.my-auto.ms-2.d-none.d-xxl-block back-label]])))

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

(defn history-view
  "History view displayed in the left column in the desktop view."
  []
  (let [history @(rf/subscribe [:discussion-history])
        indexed-history (map-indexed #(vector (- (count history) %1 1) %2) history)
        has-history? (seq indexed-history)]
    (when has-history?
      [:section.history-wrapper
       [:h5.p-2.text-center (labels :history/title)]
       [discussion-start-button]
       ;; history
       (for [[index statement] indexed-history]
         (let [max-word-count 20
               nickname (user-utils/statement-author statement)
               user (:statement/author statement)
               statement-content (-> statement :statement/content)
               tooltip-text (gstring/format "%s %s" (labels :tooltip/history-statement) nickname)
               history-content [:div
                                [:div.d-flex.flex-row
                                 [:h6 (labels :history.statement/user) " " (toolbelt/truncate-to-n-chars nickname 20)]
                                 [:div.ms-auto [common/avatar user 22]]]
                                (toolbelt/truncate-to-n-words statement-content max-word-count)]]
           [:article {:key (str "history-container-" (:db/id statement))}
            [:div.history-thread-line {:key (str "history-divider-" (:db/id statement))}]
            [:div.d-inline-block.d-md-block.text-dark.w-100
             {:key (str "history-" (:db/id statement))}
             (let [attitude (name (or (:statement/type statement) :neutral))]
               [:div.card-history.clickable.w-100
                {:on-click #(rf/dispatch [:discussion.history/time-travel index])}
                [:div.d-flex.flex-row
                 [:div {:class (str "highlight-card-" attitude)}]
                 [:div.history-card-content
                  (if (zero? index)
                    history-content
                    [tooltip/text tooltip-text history-content {:placement :right}])]]])]]))])))

(rf/reg-event-fx
 :discussion.add.statement/starting
 (fn [{:keys [db]} [_ form]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         statement-text (oget form [:statement-text :value])]
     {:db (update-in db [:schnaq :selected :meta-info :all-statements] inc)
      :fx [(http/xhrio-request db :post "/discussion/statements/starting/add"
                               [:discussion.add.statement/starting-success form]
                               {:statement statement-text
                                :share-hash share-hash
                                :display-name (toolbelt/current-display-name db)}
                               [:ajax.error/as-notification])]})))

(rf/reg-event-fx
 :discussion.add.statement/starting-success
 (fn [_ [_ form new-starting-statements]]
   (let [starting-conclusions (:starting-conclusions new-starting-statements)
         statement-with-creation-secret (first (filter :statement/creation-secret
                                                       starting-conclusions))]
     {:fx [[:dispatch [:notification/add
                       #:notification{:title (labels :discussion.notification/new-content-title)
                                      :body (labels :discussion.notification/new-content-body)
                                      :context :success}]]
           [:dispatch [:discussion.query.conclusions/set-starting new-starting-statements]]
           (when statement-with-creation-secret
             [:dispatch [:discussion.statements/add-creation-secret statement-with-creation-secret]])
           [:form/clear form]]})))

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
 (fn [{:keys [db]} [_ {:keys [starting-conclusions]}]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         visited (map :db/id starting-conclusions)]
     {:db (-> db
              (assoc-in [:discussion :premises :current] (shared-tools/normalize :db/id starting-conclusions))
              (update-in [:visited :statement-ids share-hash] #(set (concat %1 %2)) visited))
      ;; hier die seen setzen
      :fx [[:dispatch [:votes.local/reset]]]})))

(rf/reg-event-db
 :votes.local/reset
 (fn [db _]
   (update db :votes
           dissoc :up
           dissoc :down)))

;; -----------------------------------------------------------------------------

(defn- sort-options
  "Displays the different sort options for card elements."
  []
  (let [sort-method @(rf/subscribe [:discussion.statements/sort-method])]
    [tooltip/text (labels :badges/sort)
     (if (= :newest sort-method)
       [:button.btn.btn-sm.btn-primary.h-100
        {:on-click #(rf/dispatch [:discussion.statements.sort/set :popular])}
        (labels :badges.sort/newest)]
       [:button.btn.btn-sm.btn-primary.h-100
        {:on-click #(rf/dispatch [:discussion.statements.sort/set :newest])}
        (labels :badges.sort/popular)])]))

;; -----------------------------------------------------------------------------

(rf/reg-event-db
 :discussion.statements.sort/set
 (fn [db [_ method]]
   (assoc-in db [:discussion :statements :sort-method] method)))

(rf/reg-sub
 :discussion.statements/sort-method
 (fn [db _]
   (get-in db [:discussion :statements :sort-method] :newest)))

(defn- show-how-to []
  [:div.py-5
   (if @(rf/subscribe [:schnaq.routes/starting?])
     [how-to-elements/quick-how-to-schnaq]
     [how-to-elements/quick-how-to-pro-con])])

(def throttled-in-schnaq-search
  (gfun/throttle
   #(rf/dispatch [:discussion.statements/search (oget % [:target :value])])
   500))

(defn- search-clear-button
  [clear-id]
  (let [search-string @(rf/subscribe [:schnaq.search.current/search-string])
        action-icon (if (cstring/blank? search-string) :search :times)]
    [:button.btn.button-muted.h-100
     {:on-click (fn [_e]
                  (toolbelt/clear-input clear-id)
                  (rf/dispatch [:schnaq.search.current/clear-search-string]))}
     [icon action-icon "m-auto"]]))

(defn search-bar
  "A search-bar to search inside a schnaq."
  []
  (let [search-input-id "search-bar"
        route-name @(rf/subscribe [:navigation/current-route-name])
        selected-statement-id (get-in @(rf/subscribe [:navigation/current-route]) [:path-params :statement-id])]
    [:form.h-100
     {:on-submit #(.preventDefault %)
      :key (str route-name selected-statement-id)}
     [:div.input-group.search-bar.h-100.panel-white.p-0
      [:input.form-control.my-auto.search-bar-input.h-100
       {:id search-input-id
        :type "text"
        :aria-label "Search"
        :placeholder (labels :schnaq.search/input)
        :name "search-input"
        :on-key-up #(throttled-in-schnaq-search %)}]
      [search-clear-button search-input-id]]]))

(defn action-view []
  [:div.d-inline-block.text-dark.w-100.mb-3.mx-1.mx-md-0
   [:div.d-flex.flex-row.flex-wrap.panel-white.p-2
    [:div.me-1.me-lg-2.me-xxl-5.pe-lg-2
     [back-button]]
    [:div.d-flex
     [:div.me-1.mx-lg-2.pe-0.pe-lg-2
      [sort-options]]
     [:div.h-100
      (when (= :routes.schnaq/start @(rf/subscribe [:navigation/current-route-name]))
        [filters/filter-answered-statements])]]
    [:div.ms-auto.flex-grow-1.flex-md-grow-0.mt-3.mt-md-0
     [search-bar]]]])

(defn discussion-view
  "Displays a history  and input field on the left and conclusions in its center"
  []
  [:div.container-fluid.px-0.px-md-3
   [:div.row
    [:div.col-md-12.col-xxl-8.py-0.pt-md-3
     [:div.d-none.d-md-block [action-view]]]]
   [:div.d-md-none [action-view]]
   [cards/conclusion-cards-list]
   [:div.d-md-none [history-view]]
   [:div.mx-auto
    {:class (when-not shared-config/embedded? "col-11 col-md-12 col-lg-12 col-xl-10")}
    [show-how-to]]
   [:div.d-none.d-md-block [history-view]]])

(rf/reg-sub
 :schnaq.search.current/search-string
 (fn [db _]
   (get-in db [:search :schnaq :current :search-string] "")))

(rf/reg-event-db
 :schnaq.search.current/clear-search-string
 (fn [db _]
   (assoc-in db [:search :schnaq :current :search-string] "")))

(rf/reg-sub
 :schnaq.search.current/result
 (fn [db _]
   (get-in db [:search :schnaq :current :result] [])))
