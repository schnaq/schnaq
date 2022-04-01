(ns schnaq.interface.views.discussion.logic
  (:require [hodgepodge.core :refer [local-storage]]
            [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.shared-toolbelt :as shared-tools]))

(rf/reg-event-fx
 :discussion.reaction.statement/send
 (fn [{:keys [db]} [_ statement-type new-premise locked?]]
   (let [statement-id (get-in db [:current-route :parameters :path :statement-id])
         share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     (when share-hash
       {:fx [(http/xhrio-request
              db :post "/discussion/react-to/statement"
              [:discussion.reaction.statement/added]
              {:share-hash share-hash
               :conclusion-id statement-id
               :premise new-premise
               :statement-type statement-type
               :locked? locked?
               :display-name (tools/current-display-name db)}
              [:ajax.error/as-notification])]}))))

(rf/reg-event-fx
 :discussion.reaction.statement/added
 (fn [{:keys [db]} [_ response]]
   (let [new-statement (:new-statement response)]
     {:db (assoc-in db [:discussion :premises :current (:db/id new-statement)] new-statement)
      :fx [[:dispatch [:notification/new-content]]
           [:dispatch [:discussion.statements/add-creation-secret new-statement]]]})))

(rf/reg-event-fx
 :discussion.reply.statement/send
 (fn [{:keys [db]} [_ statement statement-type new-premise]]
   (let [statement-id (:db/id statement)
         share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     (when share-hash
       {:fx [(http/xhrio-request
              db :post "/discussion/react-to/statement"
              [:discussion.reply.statement/added statement]
              {:share-hash share-hash
               :conclusion-id statement-id
               :premise new-premise
               :statement-type statement-type
               :display-name (tools/current-display-name db)}
              [:ajax.error/as-notification])]}))))

(rf/reg-event-fx
 :discussion.reply.statement/added
 (fn [{:keys [db]} [_ parent-statement {:keys [new-statement]}]]
   (let [parent-statement-id (:db/id parent-statement)]
     {:db (-> db
              (update-in [:discussion :premises :current parent-statement-id :meta/sub-statement-count] inc)
              (update-in [:discussion :premises :current parent-statement-id :statement/children] conj new-statement))
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
   (-> db
       (assoc-in [:discussion :statements :creation-secrets] (:discussion/creation-secrets local-storage))
       (assoc-in [:discussion :schnaqs :creation-secrets] (:discussion.schnaqs/creation-secrets local-storage)))))

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
        pro-con-disabled? @(rf/subscribe [:schnaq.selected/pro-con?])
        form-statement-type @(rf/subscribe [:form/statement-type :selected])
        statement-type (if pro-con-disabled?
                         :statement.type/neutral
                         form-statement-type)]
    (rf/dispatch [:discussion.reaction.statement/send statement-type new-text (oget form ["lock-card?" :checked])])
    (rf/dispatch [:form/should-clear [new-text-element]])))

(defn reply-to-statement
  "Reply directly to a statement via a submitted form.
  Updates :statement/children and :meta/sub-statement-count afterwards in app-db."
  [statement-to-reply-to attitude form]
  (let [new-text-element (oget+ form [:statement])
        new-text (oget new-text-element [:value])]
    (rf/dispatch [:discussion.reply.statement/send statement-to-reply-to attitude new-text])
    (rf/dispatch [:form/should-clear [new-text-element]])))

(rf/reg-event-fx
 :discussion.query.statement/by-id
 (fn [{:keys [db]} _]
   (let [statement-id (get-in db [:current-route :parameters :path :statement-id])
         share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         ;; Hier direkt das Statement aus der DB holen wenn es da ist
         new-conclusion (first (filter #(= (:db/id %) statement-id) (get-in db [:discussion :premises :current])))]
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
       new-conclusion (update :db #(assoc-in db [:discussion :conclusion :selected] new-conclusion)
                              :fx conj [:discussion.history/push new-conclusion])))))

(rf/reg-event-fx
 :discussion.redirect/to-root
 (fn [_ [_ share-hash]]
   {:fx [[:dispatch [:navigation/navigate :routes.schnaq/start
                     {:share-hash share-hash}]]]}))

(rf/reg-event-fx
 :discussion.query.statement/by-id-success
 (fn [{:keys [db]} [_ {:keys [conclusion premises history]}]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:db (-> db
              (assoc-in [:discussion :conclusion :selected] conclusion)
              (assoc-in [:discussion :premises :current]
                        (shared-tools/normalize :db/id premises))
              (assoc-in [:history :full-context] (vec history)))
      :fx [[:dispatch [:loading/toggle [:statements? false]]]
           [:dispatch [:discussion.history/push conclusion]]
           [:dispatch [:visited/set-visited-statements conclusion]]
           [:dispatch [:notification/set-visited-statements share-hash conclusion premises]]]})))

(rf/reg-event-db
 :discussion.premises.current/dissoc
 (fn [db]
   (update-in db [:discussion :premises] dissoc :current)))

(rf/reg-event-fx
 :discussion.current/dissoc
 (fn [{:keys [db]}]
   {:db (-> db
            (update :schnaq dissoc :selected)
            (update :schnaq dissoc :current)
            (dissoc :wordcloud)
            (update :discussion dissoc :conclusion))
    :fx [[:dispatch [:discussion.premises.current/dissoc]]]}))
