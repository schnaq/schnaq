(ns schnaq.interface.views.discussion.logic
  (:require [ghostwheel.core :refer [>defn]]
            [hodgepodge.core :refer [local-storage]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [default-anonymous-display-name]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.http :as http]))

(>defn calculate-votes
  "Calculates the votes without needing to reload."
  [statement local-votes]
  [map? map? :ret number?]
  (let [up-vote-change (get-in local-votes [:up (:db/id statement)] 0)
        down-vote-change (get-in local-votes [:down (:db/id statement)] 0)]
    (-
      (+ (:meta/upvotes statement) up-vote-change)
      (+ (:meta/downvotes statement) down-vote-change))))

(rf/reg-event-fx
  :discussion.reaction.statement/send
  (fn [{:keys [db]} [_ statement-type new-premise]]
    (let [statement-id (get-in db [:current-route :parameters :path :statement-id])
          share-hash (get-in db [:schnaq :selected :discussion/share-hash])
          nickname (get-in db [:user :names :display] default-anonymous-display-name)]
      {:fx [(http/xhrio-request
              db :post "/discussion/react-to/statement"
              [:discussion.reaction.statement/added]
              {:share-hash share-hash
               :conclusion-id statement-id
               :nickname nickname
               :premise new-premise
               :statement-type statement-type}
              [:ajax.error/as-notification])]})))

(rf/reg-event-fx
  :discussion.reaction.statement/added
  (fn [{:keys [db]} [_ response]]
    (let [new-statement (:new-statement response)]
      {:db (update-in db [:discussion :premises :current]
                      conj new-statement)
       :fx [[:dispatch [:notification/new-content]]
            [:dispatch [:discussion.statements/add-creation-secret new-statement]]]})))

(rf/reg-event-fx
  :discussion.statements/add-creation-secret
  (fn [{:keys [db]} [_ statement]]
    (when (:statement/creation-secret statement)
      (let [updated-secrets (assoc (get-in db [:discussion :statements :creation-secrets])
                              (:db/id statement)
                              (:statement/creation-secret statement))]
        {:db (assoc-in db [:discussion :statements :creation-secrets] updated-secrets)
         :fx [[:localstorage/assoc [:discussion/creation-secrets updated-secrets]]]}))))

(rf/reg-event-db
  :schnaq.discussion-secrets/load-from-localstorage
  (fn [db _]
    (assoc-in db [:discussion :statements :creation-secrets] (:discussion/creation-secrets local-storage))))

(rf/reg-sub
  :schnaq.discussion.statements/creation-secrets
  (fn [db _]
    (get-in db [:discussion :statements :creation-secrets])))

(rf/reg-event-fx
  :notification/new-content
  (fn [_ _]
    {:fx [[:dispatch [:notification/add
                      #:notification{:title (labels :discussion.notification/new-content-title)
                                     :body (labels :discussion.notification/new-content-body)
                                     :context :success}]]]}))

(defn submit-new-premise
  "Submits a newly created child statement as an attack, support or neutral statement."
  [form]
  (let [new-text-element (oget form [:premise-text])
        new-text (oget new-text-element [:value])
        pro-con-disabled? @(rf/subscribe [:schnaq.selected/pro-con?])
        form-statement-type @(rf/subscribe [:form/statement-type])
        statement-type (if pro-con-disabled?
                         :neutral
                         form-statement-type)]
    (rf/dispatch [:discussion.reaction.statement/send statement-type new-text])
    (rf/dispatch [:form/should-clear [new-text-element]])))

(rf/reg-event-fx
  :discussion.query.statement/by-id
  (fn [{:keys [db]} _]
    (let [statement-id (get-in db [:current-route :parameters :path :statement-id])
          share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
      {:fx [(http/xhrio-request
              db :get "/discussion/statement/info"
              [:discussion.query.statement/by-id-success]
              {:statement-id statement-id
               :share-hash share-hash}
              [:discussion.redirect/to-root share-hash])]})))

(rf/reg-event-fx
  :discussion.redirect/to-root
  (fn [_ [_ share-hash]]
    {:fx [[:dispatch [:navigation/navigate :routes.schnaq/start
                      {:share-hash share-hash}]]]}))

(rf/reg-event-fx
  :discussion.query.statement/by-id-success
  (fn [{:keys [db]} [_ {:keys [conclusion premises history]}]]
    {:db (-> db
             (assoc-in [:discussion :conclusions :selected] conclusion)
             (assoc-in [:discussion :premises :current] premises)
             (assoc-in [:history :full-context] (vec history)))
     :fx [[:dispatch [:discussion.history/push conclusion]]
          [:dispatch [:visited/set-visited-statements conclusion]]]}))