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
    (let [{:keys [id share-hash statement-id]} (get-in db [:current-route :parameters :path])
          nickname (get-in db [:user :name] "Anonymous")]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/react-to/statement")
                          :format (ajax/transit-request-format)
                          :params {:share-hash share-hash
                                   :discussion-id id
                                   :conclusion-id statement-id
                                   :nickname nickname
                                   :premise new-premise
                                   :reaction reaction}
                          :response-format (ajax/transit-response-format)
                          :on-success [:discussion.reaction.statement/added]
                          :on-failure [:ajax.error/as-notification]}]]})))

(rf/reg-event-fx
  :discussion.undercut.statement/send
  ;; todo del
  (fn [{:keys [db]} [_ new-premise]]
    (let [{:keys [id share-hash]} (get-in db [:current-route :parameters :path])
          history (get-in db [:history :full-context] [])
          nickname (get-in db [:user :name] "Anonymous")]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/argument/undercut")
                          :format (ajax/transit-request-format)
                          :params {:share-hash share-hash
                                   :discussion-id id
                                   :selected (last history)
                                   :nickname nickname
                                   :premise new-premise
                                   :previous-id (:db/id (nth history (- (count history) 2)))}
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
