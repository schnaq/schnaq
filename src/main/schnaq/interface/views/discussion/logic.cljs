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

(defn arg-type->attitude
  "Returns an attitude deduced from an argument-type."
  [arg-type]
  (cond
    (#{:argument.type/attack :argument.type/undercut} arg-type) "disagree"
    (#{:argument.type/support} arg-type) "agree"
    :else "neutral"))

(defn attitude->symbol
  "Returns an fa symbol identifier based on attitude"
  [attitude]
  (cond
    (= "disagree" (str attitude)) :minus
    (= "agree" (str attitude)) :plus
    :else :circle))

(rf/reg-event-fx
  :discussion.reaction.statement/send
  (fn [{:keys [db]} [_ reaction new-premise]]
    (let [{:keys [share-hash statement-id]} (get-in db [:current-route :parameters :path])
          nickname (get-in db [:user :names :display] default-anonymous-display-name)]
      {:fx [(http/xhrio-request
              db :post "/discussion/react-to/statement"
              [:discussion.reaction.statement/added]
              {:share-hash share-hash
               :conclusion-id statement-id
               :nickname nickname
               :premise new-premise
               :reaction reaction}
              [:ajax.error/as-notification])]})))

(rf/reg-event-fx
  :discussion.reaction.statement/added
  (fn [{:keys [db]} [_ response]]
    (let [new-argument (:new-argument response)
          new-premise (-> new-argument
                          :argument/premises
                          first
                          (assoc :meta/argument-type (:argument/type new-argument)
                                 :db/txInstant (.now js/Date)))]
      {:db (update-in db [:discussion :premises :current]
                      conj new-premise)
       :fx [[:dispatch [:notification/new-content]]
            [:dispatch [:discussion.statements/add-creation-secret new-premise]]]})))

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
  "Submits a newly created premise as an undercut, rebut or support."
  [form]
  (let [new-text-element (oget form [:premise-text])
        new-text (oget new-text-element [:value])
        pro-con-disabled? @(rf/subscribe [:schnaq.selected/pro-con?])
        argument-type @(rf/subscribe [:form/argument-type])
        choice (if pro-con-disabled?
                 :neutral
                 (keyword (name argument-type)))]
    (rf/dispatch [:discussion.reaction.statement/send choice new-text])
    (rf/dispatch [:form/should-clear [new-text-element]])))

(rf/reg-event-fx
  :discussion.query.statement/by-id
  (fn [{:keys [db]} _]
    (let [{:keys [share-hash statement-id]} (get-in db [:current-route :parameters :path])]
      {:fx [(http/xhrio-request
              db :post "/discussion/statement/info"
              [:discussion.query.statement/by-id-success]
              {:statement-id statement-id
               :share-hash share-hash}
              [:ajax.error/as-notification])]})))

(rf/reg-event-fx
  :discussion.query.statement/by-id-success
  (fn [{:keys [db]} [_ {:keys [conclusion premises undercuts]}]]
    {:db (->
           (assoc-in db [:discussion :conclusions :selected] conclusion)
           (assoc-in [:discussion :premises :current] (concat premises undercuts)))
     :fx [[:dispatch [:discussion.history/push conclusion]]
          [:dispatch [:visited/set-visited-statements conclusion]]]}))