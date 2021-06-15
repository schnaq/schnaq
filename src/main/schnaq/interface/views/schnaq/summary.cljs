(ns schnaq.interface.views.schnaq.summary
  "All views and events important to extractive summaries can be found here."
  (:require [goog.string :as gstring]
            [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.utils.time :as time]
            [schnaq.interface.views.loading :as loading]
            [schnaq.interface.views.pages :as pages]))

(defn- summary-request-button
  "Requests a summary or a refresh."
  [share-hash]
  (let [request-status @(rf/subscribe [:schnaq.summary/status share-hash])
        summary @(rf/subscribe [:schnaq/summary share-hash])
        locale @(rf/subscribe [:current-locale])
        button-text (case request-status
                      :request-succeeded (labels :summary.user.request-succeeded/label)
                      :requested (labels :summary.user.requested/label)
                      (labels :summary.user.not-requested/label))]
    [:section.d-block.text-center
     [:button.btn.btn-secondary
      (if request-status
        {:disabled true}
        {:on-click #(rf/dispatch [:schnaq.summary/request share-hash])})
      button-text]
     [:p.small.text-muted.mt-2
      (if summary
        [:span (labels :summary.user.status/label) (time/timestamp-with-tooltip (:summary/requested-at summary) locale)]
        (labels :summary.user/cta))]]))

(defn- summary-body
  "Contains the summary an possibly some meta information."
  [schnaq]
  (let [{:summary/keys [created-at text]} @(rf/subscribe [:schnaq/summary (:discussion/share-hash schnaq)])
        locale @(rf/subscribe [:current-locale])
        [updated-at text] (if text
                            [(time/timestamp-with-tooltip created-at locale) text]
                            ["-" "-"])]
    [:<>
     [:h2.text-center (labels :summary.user/label) (:discussion/title schnaq)]
     [:small.text-muted (labels :summary.user/last-updated) updated-at]
     [:p (str text)]]))

(defn- user-summary-view
  []
  (let [current-schnaq @(rf/subscribe [:schnaq/selected])]
    [pages/with-discussion-nav
     {:page/heading (labels :summary.user/heading)
      :page/subheading (labels :summary.user/subheading)
      :condition/needs-beta-tester? true}
     current-schnaq
     [:div.container.panel-white.mt-3
      [summary-request-button (:discussion/share-hash current-schnaq)]
      [summary-body current-schnaq]]]))

(defn public-user-view []
  [user-summary-view])

(defn- list-open-summaries
  "Shows a list of all still open summaries."
  []
  [:div.container.py-4
   (let [summaries @(rf/subscribe [:summaries/open])
         locale @(rf/subscribe [:current-locale])]
     (if summaries
       [:<>
        ;; todo labels
        [:h4 (gstring/format "Number of Open Summaries: %s" (count summaries))]
        [:table.table.table-striped
         [:thead
          [:tr
           [:th {:width "25%"} "Discussion"]
           [:th {:width "15%"} "Requested at"]
           [:th {:width "60%"} "Summary"]]]
         [:tbody
          (for [summary summaries]
            (let [share-hash (-> summary :summary/discussion :discussion/share-hash)
                  summary-id (:db/id summary)]
              [:tr {:key (str "row-" summary-id)}
               [:td [:a {:href (rfe/href :routes.schnaq/start
                                         {:share-hash share-hash})}
                     (-> summary :summary/discussion :discussion/title)]]
               [:td (time/timestamp-with-tooltip (:summary/requested-at summary) locale)]
               [:td [:form
                     {:on-submit (fn [e]
                                   (jq/prevent-default e)
                                   (rf/dispatch
                                     [:summary.admin/send share-hash (str summary-id) (oget e [:currentTarget :elements])]))}
                     [:textarea.form-control {:name (str summary-id) :rows 3 :defaultValue (:summary/text summary)}]
                     [:button.btn.btn-outline-primary.ml-1 {:type "submit"} "Submit"]]]]))]]]
       [loading/loading-placeholder]))])

(defn- list-closed-summaries
  "Shows a list of all closed summaries."
  []
  [:div.container.py-4
   (let [summaries @(rf/subscribe [:summaries/closed])
         locale @(rf/subscribe [:current-locale])]
     (if summaries
       [:<>
        ;; todo labels
        [:h4 (gstring/format "Number of Closed Summaries: %s" (count summaries))]
        [:table.table.table-striped
         [:thead
          [:tr
           [:th {:width "20%"} "Discussion"]
           [:th {:width "15%"} "Requested at"]
           [:th {:width "50%"} "Summary"]
           [:th {:width "15%"} "Closed at"]]]
         [:tbody
          (for [summary summaries]
            [:tr {:key (str "row-" (:db/id summary))}
             [:td [:a {:href (rfe/href :routes.schnaq/start
                                       {:share-hash (-> summary :summary/discussion :discussion/share-hash)})}
                   (-> summary :summary/discussion :discussion/title)]]
             [:td (time/timestamp-with-tooltip (:summary/requested-at summary) locale)]
             [:td (:summary/text summary)]
             [:td (time/timestamp-with-tooltip (:summary/created-at summary) locale)]])]]]
       [loading/loading-placeholder]))])

(defn- admin-summaries
  "Shows all summaries to the admins."
  []
  (pages/with-nav-and-header
    {:page/heading "Zusammenfassungen"
     :page/subheading "Beim drücken von senden, werden diese sofort erstellt."
     :condition/needs-administrator? true}
    [:<>
     [list-open-summaries]
     [:hr]
     [list-closed-summaries]]))

(defn admin-summaries-view []
  [admin-summaries])

(rf/reg-event-fx
  :summary.admin/send
  (fn [{:keys [db]} [_ share-hash html-selector form]]
    {:fx [(http/xhrio-request db :put "/admin/summary/send"
                              [:summary.admin.send/success form]
                              {:new-summary-text (oget+ form [html-selector :value])
                               :share-hash share-hash})]}))

(rf/reg-event-fx
  :summary.admin.send/success
  (fn [{:keys [db]} [_ form response]]
    (let [new-summary (:new-summary response)
          updated-summaries (map #(if (= (:db/id new-summary) (:db/id %)) new-summary %)
                                 (get-in db [:summaries :all]))]
      {:db (assoc-in db [:summaries :all] updated-summaries)
       :fx [[:form/clear form]]})))

(rf/reg-event-fx
  :schnaq.summary/request
  (fn [{:keys [db]} [_ share-hash]]
    {:db (assoc-in db [:schnaq :summary :status share-hash] :requested)
     :fx [(http/xhrio-request db :post "/schnaq/summary/request" [:schnaq.summary.request/success share-hash]
                              {:share-hash share-hash})]}))

(rf/reg-event-db
  :schnaq.summary.request/success
  (fn [db [_ share-hash result]]
    (-> db
        (assoc-in [:schnaq :summary :status share-hash] :request-succeeded)
        (assoc-in [:schnaq :summary :result share-hash] (:summary result)))))

(rf/reg-sub
  :schnaq.summary/status
  (fn [db [_ share-hash]]
    (get-in db [:schnaq :summary :status share-hash])))

(rf/reg-sub
  :schnaq/summary
  (fn [db [_ share-hash]]
    (get-in db [:schnaq :summary :result share-hash])))

(rf/reg-event-fx
  :schnaq.summary/load
  (fn [{:keys [db]} [_ share-hash]]
    {:fx [(http/xhrio-request db :get "/schnaq/summary" [:schnaq.summary.load/success share-hash]
                              {:share-hash share-hash})]}))

(rf/reg-event-db
  :schnaq.summary.load/success
  (fn [db [_ share-hash result]]
    (let [{:summary/keys [created-at requested-at text] :as summary} (:summary result)]
      (cond-> (assoc-in db [:schnaq :summary :result share-hash] summary)
              (or (and requested-at (not text))             ;; Requested, but not finished
                  (and created-at requested-at (> requested-at created-at))) ; Requested update
              (assoc-in [:schnaq :summary :status share-hash] :request-succeeded)))))

(rf/reg-event-fx
  :summaries/load-all
  (fn [{:keys [db]} _]
    {:fx [(http/xhrio-request db :get "/admin/summaries/all" [:summaries.load-all/success])]}))

(rf/reg-event-db
  :summaries.load-all/success
  (fn [db [_ result]]
    (assoc-in db [:summaries :all] (:summaries result))))

(rf/reg-sub
  :summaries/all
  (fn [db _]
    (get-in db [:summaries :all] [])))

(rf/reg-sub
  :summaries/open
  (fn [_ _]
    (rf/subscribe [:summaries/all]))
  (fn [summaries _ _]
    (sort-by
      :summary/requested-at
      (remove #(and (:summary/text %)                       ;; No summary provided yet
                    (< (:summary/requested-at %) (:summary/created-at %))) ;; Update requested after last summary
              summaries))))

(rf/reg-sub
  :summaries/closed
  (fn [_ _]
    (rf/subscribe [:summaries/all]))
  (fn [summaries _ _]
    (sort-by
      :summary/created-at
      (filter #(and (:summary/text %)
                    (< (:summary/requested-at %) (:summary/created-at %)))
              summaries))))
