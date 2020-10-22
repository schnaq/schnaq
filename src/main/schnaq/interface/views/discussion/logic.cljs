(ns schnaq.interface.views.discussion.logic
  (:require [ajax.core :as ajax]
            [ghostwheel.core :refer [>defn]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]))


(>defn calculate-votes
  "Calculates the votes without needing to reload."
  [statement vote-type vote-store]
  [map? keyword? map? :ret number?]
  (let [[internal-key db-key] (if (= vote-type :upvotes)
                                [:meta/upvotes :up]
                                [:meta/downvotes :down])
        vote-change (get-in vote-store [db-key (:db/id statement)] 0)]
    (+ (internal-key statement) vote-change)))


(defn deduce-step
  "Deduces the current discussion-loop step by the available options."
  [options]
  (cond
    (some #{:starting-support/new} options) :starting-conclusions/select
    (some #{:undercut/new} options) :select-or-react
    :else :default))

(defn index-of
  "Returns the index of the first occurrence of `elem` in `coll` if its present and
  nil if not."
  [coll elem]
  (when coll
    (let [maybe-index (.indexOf coll elem)]
      (when-not (= maybe-index -1)
        maybe-index))))

(defn args-for-reaction
  "Returns the args for a certain reaction."
  [all-steps all-args reaction]
  (when-let [idx (index-of all-steps reaction)]
    (nth all-args idx)))

(defn arg-type->attitude
  "Returns an attitude deduced from an argument-type."
  [arg-type]
  (cond
    (#{:argument.type/attack :argument.type/undercut} arg-type) "disagree"
    (#{:argument.type/support} arg-type) "agree"
    :else "neutral"))

(rf/reg-event-fx
  :discussion.reaction.starting/send
  (fn [{:keys [db]} [_ reaction new-premise]]
    (let [{:keys [id share-hash statement-id]} (get-in db [:current-route :parameters :path])
          nickname (get-in db [:user :name] "Anonymous")]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/react-to/starting")
                          :format (ajax/transit-request-format)
                          :params {:share-hash share-hash
                                   :discussion-id id
                                   :conclusion-id statement-id
                                   :nickname nickname
                                   :premise new-premise
                                   :reaction reaction}
                          :response-format (ajax/transit-response-format)
                          :on-success [:discussion.reaction.starting/added]
                          :on-failure [:ajax-failure]}]]})))

(rf/reg-event-db
  :discussion.reaction.starting/added
  (fn [db [_ response]]
    (let [new-starting-argument (:new-starting-argument response)
          new-premise (-> new-starting-argument
                          :argument/premises
                          first
                          (assoc :meta/argument-type (:argument/type new-starting-argument)))]
      (update-in db [:discussion :premises :current]
                 conj new-premise))))

(defn submit-new-starting-premise
  "Takes a form input and submits a reaction to a starting conclusion."
  [form]
  (let [new-text-element (oget form [:premise-text])
        new-text (oget new-text-element [:value])
        choice (oget form [:premise-choice :value])
        reaction (if (= choice "against-radio")
                   :attack
                   :support)]
    (rf/dispatch [:discussion.reaction.starting/send reaction new-text])
    (rf/dispatch [:form/should-clear [new-text-element]])))

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
                          :on-failure [:ajax-failure]}]]})))

(rf/reg-event-db
  :discussion.reaction.statement/added
  (fn [db [_ response]]
    (let [new-argument (:new-argument response)
          new-premise (-> new-argument
                          :argument/premises
                          first
                          (assoc :meta/argument-type (:argument/type new-argument)))]
      (update-in db [:discussion :premises :current]
                 conj new-premise))))

(defn submit-new-premise
  "Submits a newly created premise as an undercut, rebut or support."
  [form]
  (let [new-text-element (oget form [:premise-text])
        new-text (oget new-text-element [:value])
        choice (oget form [:premise-choice :value])]
    (case choice
      "against-radio" (rf/dispatch [:discussion.reaction.statement/send :attack new-text])
      "for-radio" (rf/dispatch [:discussion.reaction.statement/send :support new-text])
      "undercut-radio" (rf/dispatch [:discussion/continue :undercut/new (assoc {:whatever :foo} :new/undercut new-text)]))
    (rf/dispatch [:form/should-clear [new-text-element]])))
