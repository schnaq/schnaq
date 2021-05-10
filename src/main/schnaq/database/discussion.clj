(ns schnaq.database.discussion
  "Discussion related functions interacting with the database."
  (:require [clojure.data :as cdata]
            [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn ? >defn-]]
            [schnaq.config :as config]
            [schnaq.database.main :refer [transact query fast-pull] :as main-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.toolbelt :as toolbelt]
            [schnaq.user :as user]
            [taoensso.timbre :as log])
  (:import (java.util UUID Date)))

(def statement-rules
  '[[(statements-from-argument ?argument ?statements)
     [?argument :argument/conclusion ?statements]]
    [(statements-from-argument ?argument ?statements)
     [?argument :argument/premises ?statements]]
    [(statements ?discussion ?statements)
     (or-join [?discussion ?statements]
              [?discussion :discussion/starting-statements ?statements]
              (and [?arguments :argument/discussions ?discussion]
                   (statements-from-argument ?arguments ?statements)))]])

(def statement-pattern
  "Representation of a statement. Oftentimes used in a Datalog pull pattern."
  [:db/id
   :statement/content
   :statement/version
   :statement/deleted?
   :statement/created-at
   {:statement/author user-db/combined-user-pattern}])

(def argument-pattern
  "Defines the default pattern for arguments. Oftentimes used in pull-patterns
  in a Datalog query bind the data to this structure."
  [:db/id
   :argument/version
   {:argument/author user-db/combined-user-pattern}
   {:argument/type [:db/ident]}
   {:argument/premises statement-pattern}
   {:argument/conclusion
    (conj statement-pattern
          :argument/version
          {:argument/author user-db/combined-user-pattern}
          {:argument/type [:db/ident]}
          {:argument/premises statement-pattern}
          {:argument/conclusion statement-pattern})}])

(def ^:private argument-pattern-with-secret-premises
  [:db/id
   :argument/version
   {:argument/author user-db/combined-user-pattern}
   {:argument/type [:db/ident]}
   {:argument/premises (conj statement-pattern :statement/creation-secret)}
   {:argument/conclusion
    (conj statement-pattern
          :argument/version
          {:argument/author user-db/combined-user-pattern}
          {:argument/type [:db/ident]}
          {:argument/premises statement-pattern}
          {:argument/conclusion statement-pattern})}])

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
   {:discussion/author user-db/combined-user-pattern}])

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
   {:discussion/author user-db/combined-user-pattern}])

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
              [?args :argument/conclusion ?parent]
              [?args :argument/premises ?child]]]
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

(>defn delete-statements!
  "Deletes all statements, without explicitly checking anything."
  [statement-ids]
  [(s/coll-of :db/id) :ret associative?]
  (transact (mapv #(vector :db/add % :statement/deleted? true) statement-ids)))

(>defn- pack-premises
  "Packs premises into a statement-structure."
  ([premises user-id]
   [(s/coll-of :statement/content) :db/id :ret (s/coll-of map?)]
   (mapv (fn [premise] {:db/id (str "premise-" premise)
                        :statement/author user-id
                        :statement/content premise
                        :statement/version 1
                        :statement/created-at (Date.)})
         premises))
  ([premises user-id creation-secrets]
   [(s/coll-of :statement/content) :db/id (s/coll-of :statement/creation-secret) :ret (s/coll-of map?)]
   (mapv #(assoc %1 :statement/creation-secret %2) (pack-premises premises user-id) creation-secrets)))

(>defn prepare-new-argument
  "Prepares a new argument for transaction. Optionally sets a temporary id."
  ([discussion-id user-id conclusion premises temporary-id]
   [:db/id :db/id :statement/content (s/coll-of :statement/content) :db/id :ret map?]
   (merge
     (prepare-new-argument discussion-id user-id conclusion premises)
     {:db/id temporary-id}))
  ([discussion-id user-id conclusion premises]
   [:db/id :db/id :statement/content (s/coll-of :statement/content) :ret map?]
   {:argument/author user-id
    :argument/premises (pack-premises premises user-id)
    :argument/conclusion {:db/id (str "conclusion-" conclusion)
                          :statement/author user-id
                          :statement/content conclusion
                          :statement/version 1
                          :statement/created-at (Date.)}
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions [discussion-id]}))

(defn- build-new-statement
  "Builds a new statement for transaction."
  ([user-id content]
   (build-new-statement user-id content (str "conclusion-" content)))
  ([user-id content temp-id]
   {:db/id temp-id
    :statement/author user-id
    :statement/content content
    :statement/version 1
    :statement/created-at (Date.)}))

(>defn add-starting-statement!
  "Adds a new starting-statement and returns the newly created id."
  [share-hash user-id statement-content registered-user?]
  [:discussion/share-hash :db/id :statement/content any? :ret :db/id]
  (let [minimum-statement (build-new-statement user-id statement-content "add/starting-argument")
        new-statement (if registered-user?
                        minimum-statement
                        (assoc minimum-statement :statement/creation-secret (.toString (UUID/randomUUID))))
        temporary-id (:db/id new-statement)
        discussion-id (:db/id (discussion-by-share-hash share-hash))]
    (get-in @(transact [new-statement
                        [:db/add discussion-id :discussion/starting-statements temporary-id]])
            [:tempids temporary-id])))

(defn all-arguments-for-conclusion
  "Get all arguments for a given conclusion."
  [conclusion-id]
  (-> (query
        '[:find [(pull ?arguments argument-pattern) ...]
          :in $ argument-pattern ?conclusion
          :where [?arguments :argument/conclusion ?conclusion]]
        argument-pattern conclusion-id)
      (toolbelt/pull-key-up :db/ident)))

(defn all-premises-for-conclusion
  "Get all premises for a given conclusion."
  [conclusion-id]
  (let [statements
        (query
          '[:find (pull ?statements statement-pattern) (pull ?type [:db/ident])
            :keys :statement :argument-type
            :in $ statement-pattern ?conclusion
            :where [?arguments :argument/conclusion ?conclusion]
            [?arguments :argument/premises ?statements]
            [?arguments :argument/type ?type]]
          statement-pattern conclusion-id)]
    (map (fn [{:keys [statement argument-type]}]
           (-> statement
               (assoc :meta/argument-type argument-type)
               (toolbelt/pull-key-up :db/ident)))
         statements)))

(defn statements-undercutting-premise
  "Return all statements that are used to undercut an argument where `statement-id`
  is used as one of the premises in the undercut argument."
  [statement-id]
  (query
    '[:find [(pull ?undercutting-statements statement-pattern) ...]
      :in $ statement-pattern ?statement-id
      :where [?arguments :argument/premises ?statement-id]
      [?undercutting-arguments :argument/conclusion ?arguments]
      [?undercutting-arguments :argument/premises ?undercutting-statements]]
    statement-pattern statement-id))

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

(defn all-arguments-for-discussion
  "Returns all arguments belonging to a discussion, identified by share-hash."
  [share-hash]
  (-> (query
        '[:find [(pull ?discussion-arguments argument-pattern) ...]
          :in $ argument-pattern ?share-hash
          :where [?discussion :discussion/share-hash ?share-hash]
          [?discussion-arguments :argument/discussions ?discussion]]
        argument-pattern share-hash)
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

(>defn- new-premises-for-statement!
  "Creates a new argument based on a statement, which is used as conclusion."
  [share-hash user-id new-conclusion-id new-statement-string argument-type registered-user?]
  [:discussion/share-hash :db/id :db/id :statement/content :argument/type any? :ret associative?]
  (let [discussion-id (:db/id (discussion-by-share-hash share-hash))
        new-arguments
        [{:db/id (str "argument-" new-statement-string)
          :argument/author user-id
          :argument/premises (if registered-user?
                               (pack-premises [new-statement-string] user-id)
                               (pack-premises [new-statement-string] user-id [(.toString (UUID/randomUUID))]))
          :argument/conclusion new-conclusion-id
          :argument/version 1
          :argument/type argument-type
          :argument/discussions [discussion-id]}]]
    @(transact new-arguments)))

(>defn react-to-statement!
  "Create a new statement reacting to another statement. Returns the newly created argument."
  [share-hash user-id statement-id reacting-string reaction registered-user?]
  [:discussion/share-hash :db/id :db/id :statement/content keyword? any? :ret ::specs/argument]
  (let [argument-id
        (get-in
          (new-premises-for-statement! share-hash user-id statement-id reacting-string reaction registered-user?)
          [:tempids (str "argument-" reacting-string)])
        argument-pattern (if registered-user? argument-pattern argument-pattern-with-secret-premises)]
    (toolbelt/pull-key-up
      (fast-pull argument-id argument-pattern)
      :db/ident)))

(>defn new-discussion
  "Adds a new discussion to the database."
  [discussion-data public?]
  [map? boolean? :ret :db/id]
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
  (distinct
    (concat
      (query
        '[:find [(pull ?statements statement-pattern) ...]
          :in $ ?share-hash statement-pattern
          :where [?discussion :discussion/share-hash ?share-hash]
          [?arguments :argument/discussions ?discussion]
          (or
            [?arguments :argument/conclusion ?statements]
            [?arguments :argument/premises ?statements])
          [?statements :statement/version _]]
        share-hash statement-pattern)
      ;; When there are no reactions to the starting statement, the starting statements
      ;; need to be checked explicitly, because there will be no arguments containing them.
      (query
        '[:find [(pull ?statements statement-pattern) ...]
          :in $ ?share-hash statement-pattern
          :where [?discussion :discussion/share-hash ?share-hash]
          [?discussion :discussion/starting-statements ?statements]]
        share-hash statement-pattern))))

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
                (:statement/content statement))})
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
  (or
    (query
      '[:find ?discussion .
        :in $ ?statement ?hash
        :where [?discussion :discussion/share-hash ?hash]
        [?argument :argument/discussions ?discussion]
        (or [?argument :argument/premises ?statement]
            [?argument :argument/conclusion ?statement])]
      statement-id share-hash)
    (query
      '[:find ?discussion .
        :in $ ?statement ?hash
        :where [?discussion :discussion/share-hash ?hash]
        [?discussion :discussion/starting-statements ?statement]]
      statement-id share-hash)))

(>defn change-statement-text-and-type
  "Changes the content of a statement to `new-content` and the type to `new-type` if it's an argument."
  [statement-id new-type new-content]
  [:db/id :argument/type :statement/content :ret ::specs/statement]
  (log/info "Statement" statement-id "edited with new content.")
  (if-let [argument (main-db/fast-pull statement-id '[:argument/_premises])]
    (let [argument-id (-> argument :argument/_premises first :db/id)]
      (log/info "Argument" argument-id "updated to new type " new-type)
      @(transact [[:db/add statement-id :statement/content new-content]
                  [:db/add argument-id :argument/type new-type]]))
    @(transact [[:db/add statement-id :statement/content new-content]]))
  (fast-pull statement-id statement-pattern))

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

(>defn search-schnaq
  "Searches the content of statements in a schnaq and returns the corresponding statement ids."
  [share-hash search-string]
  [:discussion/share-hash ::specs/non-blank-string :ret (s/coll-of :db/id)]
  (query '[:find [?statements ...]
           :in $ % ?share-hash ?search-string
           :where [?discussion :discussion/share-hash ?share-hash]
           (statements ?discussion ?statements)
           [(fulltext $ :statement/content ?search-string) [[?statements _ _ _]]]]
         statement-rules share-hash search-string))
