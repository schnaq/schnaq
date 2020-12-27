(ns schnaq.interface.views.discussion.logic
  (:require [ajax.core :as ajax]
            [ghostwheel.core :refer [>defn]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels]]))

(>defn calculate-votes
  "Calculates the votes without needing to reload."
  [statement vote-type vote-store]
  [map? keyword? map? :ret number?]
  (let [[internal-key db-key] (if (= vote-type :upvotes)
                                [:meta/upvotes :up]
                                [:meta/downvotes :down])
        vote-change (get-in vote-store [db-key (:db/id statement)] 0)]
    (+ (internal-key statement) vote-change)))

(defn arg-type->attitude
  "Returns an attitude deduced from an argument-type."
  [arg-type]
  (cond
    (#{:argument.type/attack :argument.type/undercut} arg-type) "disagree"
    (#{:argument.type/support} arg-type) "agree"
    :else "neutral"))

(rf/reg-event-fx
  :discussion.reaction.statement/send
  (fn [{:keys [db]} [_ reaction new-premise]]
    (let [{:keys [share-hash statement-id]} (get-in db [:current-route :parameters :path])
          nickname (get-in db [:user :name] "Anonymous")]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/react-to/statement")
                          :format (ajax/transit-request-format)
                          :params {:share-hash share-hash
                                   :conclusion-id statement-id
                                   :nickname nickname
                                   :premise new-premise
                                   :reaction reaction}
                          :response-format (ajax/transit-response-format)
                          :on-success [:discussion.reaction.statement/added]
                          :on-failure [:ajax.error/as-notification]}]]})))

(rf/reg-event-fx
  :discussion.reaction.statement/added
  (fn [{:keys [db]} [_ response]]
    (let [new-argument (:new-argument response)
          new-premise (-> new-argument
                          :argument/premises
                          first
                          (assoc :meta/argument-type (:argument/type new-argument)))]
      {:db (update-in db [:discussion :premises :current]
                      conj new-premise)
       :fx [[:dispatch [:notification/new-content]]]})))

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
        choice (oget form [:premise-choice :value])]
    (case choice
      "against-radio" (rf/dispatch [:discussion.reaction.statement/send :attack new-text])
      "for-radio" (rf/dispatch [:discussion.reaction.statement/send :support new-text]))
    (rf/dispatch [:form/should-clear [new-text-element]])))

(rf/reg-event-fx
  :discussion.query.statement/by-id
  (fn [{:keys [db]} _]
    (let [{:keys [share-hash statement-id]} (get-in db [:current-route :parameters :path])]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/statement/info")
                          :format (ajax/transit-request-format)
                          :params {:statement-id statement-id
                                   :share-hash share-hash}
                          :response-format (ajax/transit-response-format)
                          :on-success [:discussion.query.statement/by-id-success]
                          :on-failure [:ajax.error/as-notification]}]]})))

(rf/reg-event-fx
  :discussion.query.statement/by-id-success
  (fn [{:keys [db]} [_ {:keys [conclusion premises undercuts]}]]
    {:db (->
           (assoc-in db [:discussion :conclusions :selected] conclusion)
           (assoc-in [:discussion :premises :current] (concat premises undercuts)))
     :fx [[:dispatch [:discussion.history/push conclusion]]]}))