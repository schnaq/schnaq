(ns schnaq.interface.views.discussion.logic
  (:require [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.localstorage :refer [from-localstorage]]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.shared-toolbelt :as stools]))

(defn- react-to-statement-call!
  "A call to the route for adding a reaction to a statement."
  [db statement-id premise-text statement-type locked? on-success-fx]
  (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
    (when share-hash
      (http/xhrio-request
       db :post "/discussion/react-to/statement"
       on-success-fx
       {:share-hash share-hash
        :conclusion-id statement-id
        :premise premise-text
        :statement-type statement-type
        :locked? locked?
        :display-name (tools/current-display-name db)}
       [:ajax.error/as-notification]))))

(rf/reg-event-fx
 :discussion.reaction.statement/send
 (fn [{:keys [db]} [_ statement-type new-premise locked?]]
   (let [statement-id (get-in db [:current-route :parameters :path :statement-id])]
     {:fx [(react-to-statement-call! db statement-id new-premise statement-type locked?
                                     [:discussion.reaction.statement/added statement-id])]})))

(rf/reg-event-fx
 :discussion.reply.statement/send
 (fn [{:keys [db]} [_ statement-id statement-type new-premise]]
   {:fx [(react-to-statement-call! db statement-id new-premise statement-type false
                                   [:discussion.reply.statement/added statement-id])]}))

(defn- add-reaction-success
  "Generic return value for event where a statement reaction was added."
  [db parent-statement-id new-statement]
  {:db (-> db
           (update-in [:schnaq :statements parent-statement-id :meta/sub-statement-count] inc)
           (update-in [:schnaq :statements parent-statement-id :statement/children] conj (:db/id new-statement))
           (assoc-in [:schnaq :statements (:db/id new-statement)] new-statement))
   :fx [[:dispatch [:notification/new-content]]
        [:dispatch [:discussion.statements/add-creation-secret new-statement]]]})

(rf/reg-event-fx
 :discussion.reaction.statement/added
 (fn [{:keys [db]} [_ parent-id {:keys [new-statement]}]]
   (update (add-reaction-success db parent-id new-statement)
           :db #(update-in % [:schnaq :statement-slice :current-level] (comp set conj) (:db/id new-statement)))))

(rf/reg-event-fx
 :discussion.reply.statement/added
 (fn [{:keys [db]} [_ parent-statement-id {:keys [new-statement]}]]
   (add-reaction-success db parent-statement-id new-statement)))

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
   (let [discussion-creation-secrets (from-localstorage :discussion/creation-secrets)
         discussion-schnaqs-creation-secrets (from-localstorage :discussion.schnaqs/creation-secrets)]
     (cond-> db
       discussion-creation-secrets (assoc-in [:discussion :statements :creation-secrets] discussion-creation-secrets)
       discussion-schnaqs-creation-secrets (assoc-in [:discussion :schnaqs :creation-secrets] discussion-schnaqs-creation-secrets)))))

(rf/reg-sub
 :schnaq.discussion.statements/creation-secrets
 (fn [db _]
   (get-in db [:discussion :statements :creation-secrets])))

(rf/reg-sub
 :schnaq.discussion/creation-secrets
 (fn [db _]
   (get-in db [:discussion :schnaqs :creation-secrets])))

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
  (let [new-text-element (oget form [:statement])
        new-text (oget new-text-element [:value])
        pro-con-disabled? @(rf/subscribe [:schnaq.state/pro-con?])
        form-statement-type @(rf/subscribe [:form/statement-type :selected])
        statement-type (if pro-con-disabled?
                         :statement.type/neutral
                         form-statement-type)]
    (rf/dispatch [:discussion.reaction.statement/send statement-type new-text
                  (boolean (oget form ["?lock-card?" :checked]))])
    (rf/dispatch [:form/should-clear [new-text-element]])))

(defn reply-to-statement
  "Reply directly to a statement via a submitted form.
  Updates :statement/children and :meta/sub-statement-count afterwards in app-db."
  [parent-id attitude form]
  (let [new-text-element (oget+ form [:statement])
        new-text (oget new-text-element [:value])]
    (rf/dispatch [:discussion.reply.statement/send parent-id attitude new-text])
    (rf/dispatch [:form/should-clear [new-text-element]])))

(rf/reg-event-fx
 :discussion.query.statement/by-id
 (fn [{:keys [db]} _]
   (let [statement-id (get-in db [:current-route :parameters :path :statement-id])
         share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         ;; Hier direkt das Statement aus der DB holen wenn es da ist
         new-conclusion (get-in db [:schnaq :statements statement-id])]
     ;; set new conclusion immediately if it's in db already, so loading times are reduced
     (cond->
      {:fx [[:dispatch [:loading/toggle [:statements? true]]]
            (http/xhrio-request
             db :get "/discussion/statement/info"
             [:discussion.query.statement/by-id-success]
             {:statement-id statement-id
              :share-hash share-hash
              :display-name (tools/current-display-name db)}
             [:discussion.redirect/to-root share-hash])]}
       new-conclusion (update :db (fn [_ _ _ _] (assoc-in db [:statements :focus] (:db/id new-conclusion)))
                              :fx conj [:discussion.history/push new-conclusion])))))

(rf/reg-event-fx
 :discussion.redirect/to-root
 (fn [_ [_ share-hash]]
   {:fx [[:dispatch [:navigation/navigate :routes.schnaq/start
                     {:share-hash share-hash}]]]}))

(rf/reg-event-fx
 :discussion.query.statement/by-id-success
 (fn [{:keys [db]} [_ {:keys [conclusion premises history children]}]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         statements (stools/normalize :db/id (conj (concat premises history children) conclusion))]
     {:db (-> db
              (assoc-in [:statements :focus] (:db/id conclusion))
              (update-in [:schnaq :statements] merge statements)
              (assoc-in [:schnaq :statement-slice :current-level] (map :db/id premises))
              (assoc-in [:history :full-context] (vec (map :db/id history))))
      :fx [[:dispatch [:loading/toggle [:statements? false]]]
           [:dispatch [:votes.local/reset]]
           [:dispatch [:discussion.history/push conclusion]]
           [:dispatch [:visited/set-visited-statements conclusion]]
           [:dispatch [:notification/set-visited-statements share-hash conclusion premises]]]})))

(rf/reg-event-db
 :schnaq.statements.current/dissoc
 (fn [db]
   (update-in db [:schnaq :statement-slice] dissoc :current-level)))

(rf/reg-event-fx
 :discussion.current/dissoc
 (fn [{:keys [db]}]
   {:db (-> db
            (update :schnaq dissoc :selected)
            (update :schnaq dissoc :current)
            (dissoc :wordcloud)
            (update :discussion dissoc :conclusion))
    :fx [[:dispatch [:schnaq.statements.current/dissoc]]]}))

(rf/reg-event-db
 :statement/update
 (fn [db [_ {:keys [statement]}]]
   (assoc-in db [:schnaq :statements (:db/id statement)] statement)))

(rf/reg-sub
 :statements/replies
 :<- [:schnaq/statements]
 ;; Returns a list of reply ids
 (fn [statements [_ parent-id]]
   (let [parent (get statements parent-id)]
     (filter #(not-any? #{":check"} (:statement/labels %))
             (stools/select-values statements (:statement/children parent)))
     (->> (:statement/children parent)
          (stools/select-values statements)
          (filter #(not-any? #{":check"} (:statement/labels %)))
          (map :db/id)))))

(rf/reg-sub
 :statements/answers
 :<- [:schnaq/statements]
 ;; Returns a list of answer ids
 (fn [statements [_ parent-id]]
   (let [parent (get statements parent-id)]
     (->> (:statement/children parent)
          (stools/select-values statements)
          (filter #(some #{":check"} (:statement/labels %)))
          (map :db/id)))))
