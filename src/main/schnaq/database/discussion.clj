(ns schnaq.database.discussion
  "Discussion related functions interacting with the database."
  (:require [clojure.data :as cdata]
            [clojure.spec.alpha :as s]
            [datomic.api :as d]
            [ghostwheel.core :refer [>defn ? >defn-]]
            [schnaq.config :as config]
            [schnaq.database.main :refer [transact query fast-pull] :as main-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.toolbelt :as toolbelt]
            [schnaq.user :as user]
            [taoensso.timbre :as log])
  (:import (java.util UUID Date)))

(def statement-pattern
  "Representation of a statement. Oftentimes used in a Datalog pull pattern."
  [:db/id
   :statement/content
   :statement/version
   :statement/deleted?
   :statement/created-at
   :statement/parent
   {:statement/type [:db/ident]}
   {:statement/author user-db/public-user-pattern}])

(def ^:private statement-pattern-with-secret
  (conj statement-pattern :statement/creation-secret))

(def discussion-pattern
  "Representation of a discussion. Oftentimes used in a Datalog pull pattern."
  [:db/id
   :discussion/title
   :discussion/description
   {:discussion/states [:db/ident]}
   {:discussion/starting-statements statement-pattern}
   :discussion/share-hash
   :discussion/header-image-url
   :discussion/created-at
   {:discussion/author user-db/public-user-pattern}])

(def discussion-pattern-private
  "Holds sensitive information as well."
  (conj discussion-pattern :discussion/edit-hash))

(def discussion-pattern-minimal
  [:db/id
   :discussion/title
   {:discussion/states [:db/ident]}
   :discussion/share-hash
   :discussion/header-image-url
   :discussion/created-at
   {:discussion/author user-db/public-user-pattern}])

(>defn starting-statements
  "Returns all starting-statements belonging to a discussion."
  [share-hash]
  [:db/id :ret (s/coll-of ::specs/statement)]
  (query
    '[:find [(pull ?statements statement-pattern) ...]
      :in $ ?share-hash statement-pattern
      :where [?discussion :discussion/share-hash ?share-hash]
      [?discussion :discussion/starting-statements ?statements]]
    share-hash statement-pattern))

(defn transitive-child-rules
  "Returns a set of rules for finding transitive children entities of a given
  node up to depth of `depth`.
  For example, calling this function with a depth of 10 would return a
  rule set against which you could query descendants anywhere from
  direct children to 10 levels of \"children-of-children\" by using the `transitive-child-10` rule."
  [depth]
  (let [sib-sym (fn [i]
                  (symbol (str "transitive-child-" i)))]
    (apply concat
           '[[(transitive-child-1 ?parent ?child)
              [?child :statement/parent ?parent]]]
           (for [i (range 2 (inc depth))]
             [[(list (sib-sym i) '?parent '?child)
               (list 'transitive-child-1 '?parent '?child)]
              [(list (sib-sym i) '?parent '?child)
               (list (sib-sym (dec i)) '?parent '?middlelink)
               (list (sib-sym (dec i)) '?middlelink '?child)]]))))

(defn child-node-info
  "Takes a list of statement-ids and returns a map {id meta-info-map} for the statements."
  [statement-ids]
  (apply merge
         (map #(hash-map (first %) {:sub-statements (second %)
                                    :authors (last %)})
              (query
                '[:find ?statement-ids (count ?children) (distinct ?nickname)
                  :in $ % [?statement-ids ...]
                  :where
                  ;; We pick a transitive depth of 7 as a sweet-spot. The deeper the rule
                  ;; goes, the more complicated and slower the query gets. 10 is too slow
                  ;; for our purposes, but 5 is maybe not deep enough for the typical discussion
                  ;; 7 offers a good speed while being deep enough for most discussions.
                  (transitive-child-7 ?statement-ids ?children)
                  [?children :statement/author ?authors]
                  (or
                    [?authors :user/nickname ?nickname]
                    [?authors :user.registered/display-name ?nickname])]
                (transitive-child-rules 7) statement-ids))))

(defn discussion-by-share-hash
  "Query discussion and apply public discussion pattern to it."
  [share-hash]
  (toolbelt/pull-key-up
    (fast-pull [:discussion/share-hash share-hash] discussion-pattern)
    :db/ident))

(defn discussion-by-share-hash-private
  "Query discussion and apply the private discussion pattern."
  [share-hash]
  (toolbelt/pull-key-up
    (fast-pull [:discussion/share-hash share-hash] discussion-pattern-private)
    :db/ident))

(>defn valid-discussions-by-hashes
  "Returns all discussions that are valid (non deleted e.g.). Input is a collection of share-hashes."
  [share-hashes]
  [(s/coll-of :discussion/share-hash) :ret (s/coll-of ::specs/discussion)]
  (-> (query
        '[:find [(pull ?discussions discussion-pattern) ...]
          :in $ [?share-hashes ...] discussion-pattern
          :where [?discussions :discussion/share-hash ?share-hashes]
          (not-join [?discussions]
                    [?discussions :discussion/states :discussion.state/deleted])]
        share-hashes discussion-pattern-minimal)
      (toolbelt/pull-key-up :db/ident)))

(>defn children-for-statement
  "Returns all children for a statement. (Statements that have the input set as a parent)."
  [parent-id]
  [:db/id :ret (s/coll-of ::specs/statement)]
  (-> (query '[:find [(pull ?children statement-pattern) ...]
               :in $ ?parent statement-pattern
               :where [?children :statement/parent ?parent]]
             parent-id statement-pattern)
      (toolbelt/pull-key-up :db/ident)))

(defn delete-statement!
  "Deletes a statement. Hard delete if there are no children, delete flag if there are.
  Check the same for the parent and continue recursively until the root."
  [statement-id]
  (let [statement-to-delete (fast-pull statement-id statement-pattern)
        parent (fast-pull (get-in statement-to-delete [:statement/parent :db/id] )
                          [:db/id
                           :statement/deleted?])
        children (children-for-statement statement-id)]
    (if (seq children)
      (do
        (log/info "Statement will set deletion marker:" statement-id)
        (transact [[:db/add statement-id :statement/deleted? true]]))
      (do
        (log/info "Statement id scheduled for deletion:" statement-id)
        @(transact [[:db/retractEntity statement-id]])
        (when (:statement/deleted? parent)
          (delete-statement! (:db/id parent)))))))

(>defn delete-statements!
  "Deletes all statements, without explicitly checking anything."
  [statement-ids]
  [(s/coll-of :db/id) :ret associative?]
  (log/info "Statement ids scheduled for deletion:" statement-ids)
  (doseq [statement-id statement-ids] (delete-statement! statement-id)))

(defn- build-new-statement
  "Builds a new statement for transaction."
  ([user-id content discussion-id]
   (build-new-statement user-id content discussion-id (str "conclusion-" content)))
  ([user-id content discussion-id temp-id]
   {:db/id temp-id
    :statement/author user-id
    :statement/content content
    :statement/version 1
    :statement/created-at (Date.)
    :statement/discussions [discussion-id]}))

(>defn add-starting-statement!
  "Adds a new starting-statement and returns the newly created id."
  [share-hash user-id statement-content registered-user?]
  [:discussion/share-hash :db/id :statement/content any? :ret :db/id]
  (let [discussion-id (:db/id (discussion-by-share-hash share-hash))
        minimum-statement (build-new-statement user-id statement-content discussion-id)
        new-statement (if registered-user?
                        minimum-statement
                        (assoc minimum-statement :statement/creation-secret (.toString (UUID/randomUUID))))
        temporary-id (:db/id new-statement)]
    (get-in @(transact [new-statement
                        [:db/add discussion-id :discussion/starting-statements temporary-id]])
            [:tempids temporary-id])))

(>defn all-discussions-by-title
  "Query all discussions based on the title. Could possible be multiple
  entities."
  [title]
  [string? :ret (s/coll-of ::specs/discussion)]
  (-> (query
        '[:find [(pull ?discussions discussion-pattern) ...]
          :in $ discussion-pattern ?title
          :where [?discussions :discussion/title ?title]]
        discussion-pattern title)
      (toolbelt/pull-key-up :db/ident)))

(>defn statements-by-content
  "Returns all statements that have the matching `content`."
  [content]
  [:statement/content :ret (s/coll-of ::specs/statement)]
  (query
    '[:find [(pull ?statements statement-pattern) ...]
      :in $ statement-pattern ?content
      :where [?statements :statement/content ?content]]
    statement-pattern content))

(>defn delete-discussion
  "Adds the deleted state to a discussion"
  [share-hash]
  [:discussion/share-hash :ret (? :discussion/share-hash)]
  (try
    @(transact [[:db/add [:discussion/share-hash share-hash]
                 :discussion/states :discussion.state/deleted]])
    (log/info (format "Schnaq with share-hash %s has been set to deleted." share-hash))
    share-hash
    (catch Exception e
      (log/error
        (format "Deletion of discussion with share-hash %s failed. Exception:\n%s"
                share-hash e)))))

(>defn discussion-deleted?
  "Returns whether a discussion has been marked as deleted."
  [share-hash]
  [:discussion/share-hash :ret boolean?]
  (as-> (main-db/query
          '[:find (pull ?states [*])
            :in $ ?share-hash
            :where [?discussion :discussion/share-hash ?share-hash]
            [?discussion :discussion/states ?states]]
          share-hash) q
        (map #(:db/ident (first %)) q)
        (into #{} q)
        (contains? q :discussion.state/deleted)))

(>defn- new-child-statement!
  "Creates a new child statement, that references a parent."
  [discussion-id parent-id new-content statement-type user-id registered-user?]
  [(s/or :id :db/id :tuple vector?) :db/id :statement/content :statement/type :db/id any? :ret associative?]
  @(transact
     [(cond->
        {:db/id (str "new-child-" new-content)
         :statement/author user-id
         :statement/content new-content
         :statement/version 1
         :statement/created-at (Date.)
         :statement/parent parent-id
         :statement/discussions [discussion-id]
         :statement/type statement-type}
        (not registered-user?) (assoc :statement/creation-secret (.toString (UUID/randomUUID))))]))

(>defn react-to-statement!
  "Create a new statement reacting to another statement. Returns the newly created statement."
  [share-hash user-id statement-id reacting-string reaction registered-user?]
  [:discussion/share-hash :db/id :db/id :statement/content keyword? any? :ret ::specs/statement]
  (let [result (new-child-statement! [:discussion/share-hash share-hash] statement-id reacting-string
                                     reaction user-id registered-user?)
        db-after (:db-after result)
        new-child-id (get-in result [:tempids (str "new-child-" reacting-string)])]
    (toolbelt/pull-key-up
      (d/pull db-after statement-pattern-with-secret new-child-id)
      :db/ident)))

(>defn new-discussion
  "Adds a new discussion to the database."
  [discussion-data public?]
  [map? (? boolean?) :ret :db/id]
  (let [default-states [:discussion.state/open]
        states (cond-> default-states
                       public? (conj :discussion.state/public))]
    (main-db/clean-and-add-to-db! (assoc discussion-data
                                    :discussion/states states
                                    :discussion/created-at (Date.))
                                  ::specs/discussion)))

(>defn private-discussion-data
  "Return non public meeting data by id."
  [id]
  [int? :ret ::specs/discussion]
  (toolbelt/pull-key-up
    (fast-pull id discussion-pattern-private)
    :db/ident))

(defn set-discussion-read-only
  "Sets a discussion as read-only."
  [share-hash]
  (main-db/transact [[:db/add [:discussion/share-hash share-hash] :discussion/states :discussion.state/read-only]]))

(defn remove-read-only
  "Removes the read-only restriction from a discussion"
  [share-hash]
  (main-db/transact [[:db/retract [:discussion/share-hash share-hash] :discussion/states :discussion.state/read-only]]))

(defn set-disable-pro-con
  "Sets or removes the pro/con button tag"
  [share-hash disable?]
  (let [enable-transaction [[:db/retract [:discussion/share-hash share-hash]
                             :discussion/states :discussion.state/disable-pro-con]]
        disable-transaction [[:db/add [:discussion/share-hash share-hash]
                              :discussion/states :discussion.state/disable-pro-con]]
        db-transaction (if disable? disable-transaction
                                    enable-transaction)]
    (main-db/transact db-transaction)))

(defn public-discussions
  "Returns all public discussions."
  []
  (-> (query
        '[:find [(pull ?public-discussions discussion-pattern) ...]
          :in $ discussion-pattern
          :where [?public-discussions :discussion/states :discussion.state/public]
          (not-join [?public-discussions]
                    [?public-discussions :discussion/states :discussion.state/deleted])]
        discussion-pattern)
      (toolbelt/pull-key-up :db/ident)))

(>defn all-statements
  "Returns all statements belonging to a discussion."
  [share-hash]
  [:discussion/share-hash :ret (s/coll-of ::specs/statement)]
  (->
    (query '[:find [(pull ?statements statement-pattern) ...]
             :in $ ?share-hash statement-pattern
             :where [?discussion :discussion/share-hash ?share-hash]
             [?statements :statement/discussions ?discussion]
             (not [?statements :statement/deleted? true])]
           share-hash statement-pattern)
    (toolbelt/pull-key-up :db/ident)))

(>defn all-statements-for-graph
  "Returns all statements for a discussion. Specially prepared for node and edge generation."
  [share-hash]
  [:discussion/share-hash :ret sequential?]
  (map
    (fn [statement]
      {:author (user/statement-author statement)
       :id (:db/id statement)
       :label (if (:statement/deleted? statement)
                config/deleted-statement-text
                (:statement/content statement))
       :type (:statement/type statement)})
    (all-statements share-hash)))

(defn all-discussions
  "Shows all discussions currently in the db. The route is only for development purposes.
  Shows discussions in all states."
  []
  (-> (query '[:find [(pull ?discussions discussion-pattern-private) ...]
               :in $ discussion-pattern-private
               :where [?discussions :discussion/title _]]
             discussion-pattern-private)
      (toolbelt/pull-key-up :db/ident)))

(>defn check-valid-statement-id-for-discussion
  "Checks whether the statement-id matches the share-hash."
  [statement-id share-hash]
  [:db/id :discussion/share-hash :ret (? :db/id)]
  (query
    '[:find ?discussion .
      :in $ ?statement ?hash
      :where [?discussion :discussion/share-hash ?hash]
      [?statement :statement/discussions ?discussion]]
    statement-id share-hash))

(>defn change-statement-text-and-type
  "Changes the content of a statement to `new-content` and the type to `new-type` if it has a parent."
  [statement new-type new-content]
  [map? (? :statement/type) :statement/content :ret ::specs/statement]
  (let [statement-id (:db/id statement)]
    (log/info "Statement" statement-id "edited with new content.")
    (if (:statement/parent statement)
      (do
        (log/info "Statement" statement-id "updated to new type " new-type)
        @(transact [[:db/add statement-id :statement/content new-content]
                    [:db/add statement-id :statement/type new-type]]))
      @(transact [[:db/add statement-id :statement/content new-content]]))
    (toolbelt/pull-key-up (fast-pull statement-id statement-pattern) :db/ident)))

(>defn add-admin-to-discussion
  "Adds an admin user to a discussion."
  [share-hash keycloak-id]
  [:discussion/share-hash :user.registered/keycloak-id :ret associative?]
  (transact [[:db/add [:discussion/share-hash share-hash] :discussion/admins
              [:user.registered/keycloak-id keycloak-id]]]))

(>defn- build-secrets-map
  "Creates a secrets map for a collection of statements.
  When there is no secret, the statement is skipped."
  [statement-ids]
  [(? (s/coll-of :db/id)) :ret (? map?)]
  (when statement-ids
    (into {}
          (query
            '[:find ?statement ?secret
              :in $ [?statement ...]
              :where [?statement :statement/creation-secret ?secret]]
            statement-ids))))

(>defn update-authors-from-secrets
  "Takes a dictionary of statement-ids mapped to creation secrets and sets the passed author
  as their author, if the secrets are correct."
  [secrets-map author-id]
  [(? map?) :db/id :ret any?]
  (let [validated-secrets-map (build-secrets-map (keys secrets-map))
        [_ _ valid-secrets] (cdata/diff secrets-map validated-secrets-map)]
    (when valid-secrets
      @(transact
         (mapv #(vector :db/add % :statement/author author-id) (keys valid-secrets))))))

(>defn search-statements
  "Searches the content of statements in a discussion and returns the corresponding statements."
  [share-hash search-string]
  [:discussion/share-hash ::specs/non-blank-string :ret (s/coll-of ::specs/statement)]
  (->
    (query '[:find [(pull ?statements statement-pattern) ...]
             :in $ statement-pattern ?share-hash ?search-string
             :where [?discussion :discussion/share-hash ?share-hash]
             [?statements :statement/discussions ?discussion]
             [(fulltext $ :statement/content ?search-string) [[?statements _ _ _]]]]
           statement-pattern share-hash search-string)
    (toolbelt/pull-key-up :db/ident)))

(def ^:private summary-pattern
  [:db/id
   :summary/discussion
   :summary/requested-at
   :summary/text
   :summary/created-at])

(def ^:private summary-with-discussion-pattern
  [:db/id
   {:summary/discussion [:discussion/title
                         :discussion/share-hash
                         :db/id]}
   :summary/requested-at
   :summary/text
   :summary/created-at
   {:summary/requester [:user.registered/email
                        :user.registered/display-name
                        :user.registered/keycloak-id]}])

(>defn- request-summary
  "Updates an existing summary request and returns the updated version."
  [summary requester]
  [::specs/summary :summary/requester :ret ::specs/summary]
  (let [tx-result @(transact [[:db/add summary :summary/requested-at (Date.)]
                              [:db/add summary :summary/requester requester]])]
    (fast-pull summary summary-pattern (:db-after tx-result))))

(>defn summary
  "Return a summary if it exists for a discussion's share-hash."
  [share-hash]
  [:discussion/share-hash :ret (? ::specs/summary)]
  (query '[:find (pull ?summary summary-pattern) .
           :in $ ?share-hash summary-pattern
           :where [?discussion :discussion/share-hash ?share-hash]
           [?summary :summary/discussion ?discussion]]
         share-hash summary-pattern))

(>defn summary-request
  "Creates a new summary-request if there is none for the discussion. Otherwise updates the request-time."
  [share-hash keycloak-id]
  [:discussion/share-hash :user.registered/keycloak-id :ret ::specs/summary]
  (if-let [summary (:db/id (summary share-hash))]
    (request-summary summary [:user.registered/keycloak-id keycloak-id])
    (let [new-summary {:summary/discussion [:discussion/share-hash share-hash]
                       :summary/requested-at (Date.)
                       :summary/requester [:user.registered/keycloak-id keycloak-id]}]
      (transact [new-summary])
      new-summary)))

(>defn all-summaries-with-discussions
  "Return a collection of all summaries and their discussions."
  []
  [:ret (s/coll-of ::specs/summary)]
  (query '[:find [(pull ?summary summary-pattern) ...]
           :in $ summary-pattern
           :where [?summary :summary/requested-at _]]
         summary-with-discussion-pattern))

(>defn update-summary
  [share-hash new-text]
  [:discussion/share-hash :summary/text :ret ::specs/summary]
  (when-let [summary (:db/id (summary share-hash))]
    (let [tx-result @(transact [{:db/id summary
                                 :summary/text new-text
                                 :summary/created-at (Date.)}])]
      (fast-pull summary summary-with-discussion-pattern (:db-after tx-result)))))

(defn history-for-statement
  "Takes a statement entity and returns the statement-history."
  [statement-id]
  (toolbelt/pull-key-up
    (loop [current statement-id
           history (list)]
      (let [full-statement (fast-pull current statement-pattern)]
        (if (:statement/parent full-statement)
          (recur (-> full-statement :statement/parent :db/id) (conj history full-statement))
          (conj history full-statement))))
    :db/ident))
