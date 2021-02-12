(ns schnaq.database.discussion
  "Discussion related functions interacting with the database."
  (:require [clojure.spec.alpha :as s]
            [datomic.client.api :as d]
            [ghostwheel.core :refer [>defn ? >defn-]]
            [schnaq.config :as config]
            [schnaq.meeting.database :refer [transact new-connection query] :as main-db]
            [schnaq.meeting.specs :as specs]
            [schnaq.toolbelt :as toolbelt]
            [taoensso.timbre :as log])
  (:import (clojure.lang ExceptionInfo)))

(def argument-pattern
  "Defines the default pattern for arguments. Oftentimes used in pull-patterns
  in a Datalog query bind the data to this structure."
  [:db/id
   :argument/version
   {:argument/author [:user/nickname]}
   {:argument/type [:db/ident]}
   {:argument/premises main-db/statement-pattern}
   {:argument/conclusion
    (conj main-db/statement-pattern
          :argument/version
          {:argument/author [:user/nickname]}
          {:argument/type [:db/ident]}
          {:argument/premises main-db/statement-pattern}
          {:argument/conclusion main-db/statement-pattern})}])

(def discussion-pattern
  "Representation of a discussion. Oftentimes used in a Datalog pull pattern."
  [:db/id
   :discussion/title
   :discussion/description
   {:discussion/states [:db/ident]}
   {:discussion/starting-statements main-db/statement-pattern}
   :discussion/share-hash
   :discussion/header-image-url
   {:discussion/author main-db/minimal-user-pattern}])

(def discussion-pattern-private
  "Holds sensitive information as well."
  (conj discussion-pattern :discussion/edit-hash))

(def ^:private discussion-pattern-minimal
  [:db/id
   :discussion/title
   {:discussion/states [:db/ident]}
   :discussion/share-hash
   :discussion/header-image-url
   {:discussion/author [:db/id :user/nickname]}])

(>defn get-statement
  "Returns the statement given an id."
  [statement-id]
  [:db/id :ret ::specs/statement]
  (d/pull (d/db (new-connection)) main-db/statement-pattern statement-id))

(>defn starting-statements
  "Returns all starting-statements belonging to a discussion."
  [share-hash]
  [:db/id :ret (s/coll-of ::specs/statement)]
  (flatten
    (query
      '[:find (pull ?statements statement-pattern)
        :in $ ?share-hash statement-pattern
        :where [?discussion :discussion/share-hash ?share-hash]
        [?discussion :discussion/starting-statements ?statements]]
      share-hash main-db/statement-pattern)))

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
                '[:find ?statement-ids (count ?chi) (distinct ?nickname)
                  :in $ % [?statement-ids ...]
                  :where
                  (transitive-child-7 ?statement-ids ?chi)
                  [?chi :statement/author ?authors]
                  [?authors :user/nickname ?nickname]]
                (transitive-child-rules 7) statement-ids))))

(defn discussion-by-share-hash-template
  "Returns one discussion which can be reached by a certain share-hash. (schnaqs only ever have one)
  Apply the pull-patern you like."
  [share-hash template-pattern]
  (-> (query
        '[:find (pull ?discussion discussion-pattern)
          :in $ ?share-hash discussion-pattern
          :where [?discussion :discussion/share-hash ?share-hash]]
        share-hash template-pattern)
      (toolbelt/pull-key-up :db/ident)
      ffirst))

(defn discussion-by-share-hash
  "Query discussion and apply public discussion pattern to it."
  [share-hash]
  (discussion-by-share-hash-template share-hash discussion-pattern))

(defn discussion-by-share-hash-private
  "Query discussion and apply the private discussion pattern."
  [share-hash]
  (discussion-by-share-hash-template share-hash discussion-pattern-private))

(>defn valid-discussions-by-hashes
  "Returns all discussions that are valid (non deleted e.g.). Input is a collection of share-hashes."
  [share-hashes]
  [(s/coll-of :discussion/share-hash) :ret (s/coll-of ::specs/discussion)]
  (as-> (query
          '[:find (pull ?discussions discussion-pattern)
            :in $ [?share-hashes ...] discussion-pattern
            :where [?discussions :discussion/share-hash ?share-hashes]
            (not-join [?discussions]
                      [?discussions :discussion/states :discussion.state/deleted])]
          share-hashes discussion-pattern-minimal)
        result
        (toolbelt/pull-key-up result :db/ident)
        (map first result)
        (filter #(s/valid? ::specs/discussion %) result)))

(>defn delete-statements!
  "Deletes all statements, without explicitly checking anything."
  [statement-ids]
  [(s/coll-of :db/id) :ret associative?]
  (transact (mapv #(vector :db/add % :statement/deleted? true) statement-ids)))

(>defn- pack-premises
  "Packs premises into a statement-structure."
  [premises user-id]
  [(s/coll-of :statement/content) :db/id
   :ret (s/coll-of map?)]
  (mapv (fn [premise] {:db/id (str "premise-" premise)
                       :statement/author user-id
                       :statement/content premise
                       :statement/version 1})
        premises))

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
                          :statement/version 1}
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
    :statement/version 1}))

(>defn add-starting-statement!
  "Adds a new starting-statement and returns the newly created id."
  [share-hash user-id statement-content]
  [:discussion/share-hash :db/id :statement/content :ret :db/id]
  (let [new-statement (build-new-statement user-id statement-content "add/starting-argument")
        temporary-id (:db/id new-statement)
        discussion-id (:db/id (discussion-by-share-hash share-hash))]
    (get-in (transact [new-statement
                       [:db/add discussion-id :discussion/starting-statements temporary-id]])
            [:tempids temporary-id])))

(defn all-arguments-for-conclusion
  "Get all arguments for a given conclusion."
  [conclusion-id]
  (-> (query
        '[:find (pull ?arguments argument-pattern)
          :in $ argument-pattern ?conclusion
          :where [?arguments :argument/conclusion ?conclusion]]
        argument-pattern conclusion-id)
      (toolbelt/pull-key-up :db/ident)
      flatten))

(defn all-premises-for-conclusion
  "Get all premises for a given conclusion."
  [conclusion-id]
  (map
    #(assoc (first %) :meta/argument-type (second %))
    (-> (query
          '[:find (pull ?statements statement-pattern) (pull ?type [:db/ident])
            :in $ statement-pattern ?conclusion
            :where [?arguments :argument/conclusion ?conclusion]
            [?arguments :argument/premises ?statements]
            [?arguments :argument/type ?type]]
          main-db/statement-pattern conclusion-id)
        (toolbelt/pull-key-up :db/ident))))

(defn statements-undercutting-premise
  "Return all statements that are used to undercut an argument where `statement-id`
  is used as one of the premises in the undercut argument."
  [statement-id]
  (flatten
    (query
      '[:find (pull ?undercutting-statements statement-pattern)
        :in $ statement-pattern ?statement-id
        :where [?arguments :argument/premises ?statement-id]
        [?undercutting-arguments :argument/conclusion ?arguments]
        [?undercutting-arguments :argument/premises ?undercutting-statements]]
      main-db/statement-pattern statement-id)))

(>defn all-discussions-by-title
  "Query all discussions based on the title. Could possible be multiple
  entities."
  [title]
  [string? :ret (s/coll-of ::specs/discussion)]
  (-> (query
        '[:find (pull ?discussions discussion-pattern)
          :in $ discussion-pattern ?title
          :where [?discussions :discussion/title ?title]]
        discussion-pattern title)
      (toolbelt/pull-key-up :db/ident)
      flatten))

(defn all-arguments-for-discussion
  "Returns all arguments belonging to a discussion, identified by share-hash."
  [share-hash]
  (-> (query
        '[:find (pull ?discussion-arguments argument-pattern)
          :in $ argument-pattern ?share-hash
          :where [?discussion :discussion/share-hash ?share-hash]
          [?discussion-arguments :argument/discussions ?discussion]]
        argument-pattern share-hash)
      flatten
      (toolbelt/pull-key-up :db/ident)))

(>defn statements-by-content
  "Returns all statements that have the matching `content`."
  [content]
  [:statement/content
   :ret (s/coll-of ::specs/statement)]
  (map first
       (let [db (d/db (new-connection))]
         (d/q
           '[:find (pull ?statements statement-pattern)
             :in $ statement-pattern ?content
             :where [?statements :statement/content ?content]]
           db main-db/statement-pattern content))))

(>defn delete-discussion
  "Adds the deleted state to a discussion"
  [share-hash]
  [:discussion/share-hash :ret (? :discussion/share-hash)]
  (try
    (transact [[:db/add [:discussion/share-hash share-hash]
                :discussion/states :discussion.state/deleted]])
    (log/info (format "Schnaq with share-hash %s has been set to deleted." share-hash))
    share-hash
    (catch ExceptionInfo e
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
  [share-hash user-id new-conclusion-id new-statement-string argument-type]
  [:discussion/share-hash :db/id :db/id :statement/content :argument/type :ret associative?]
  (let [discussion-id (:db/id (discussion-by-share-hash share-hash))
        new-arguments
        [{:db/id (str "argument-" new-statement-string)
          :argument/author user-id
          :argument/premises (pack-premises [new-statement-string] user-id)
          :argument/conclusion new-conclusion-id
          :argument/version 1
          :argument/type argument-type
          :argument/discussions [discussion-id]}]]
    (transact new-arguments)))

(>defn- react-to-statement!
  "Create a new statement reacting to another statement. Returns the newly created argument."
  [share-hash user-id statement-id reacting-string reaction]
  [:discussion/share-hash :db/id :db/id :statement/content keyword? :ret ::specs/argument]
  (let [argument-id
        (get-in
          (new-premises-for-statement! share-hash user-id statement-id reacting-string reaction)
          [:tempids (str "argument-" reacting-string)])]
    (toolbelt/pull-key-up
      (d/pull (d/db (main-db/new-connection)) argument-pattern argument-id)
      :db/ident)))

(>defn support-statement!
  "Create a new statement supporting another statement. Returns the newly created argument."
  [share-hash user-id statement-id supporting-string]
  [:discussion/share-hash :db/id :db/id :statement/content :ret ::specs/argument]
  (react-to-statement! share-hash user-id statement-id supporting-string :argument.type/support))

(>defn attack-statement!
  "Create a new statement attacking another statement. Returns the newly created argument."
  [share-hash user-id statement-id attacking-string]
  [:discussion/share-hash :db/id :db/id :statement/content :ret ::specs/argument]
  (react-to-statement! share-hash user-id statement-id attacking-string :argument.type/attack))

(>defn new-discussion
  "Adds a new discussion to the database."
  [discussion-data public?]
  [map? boolean? :ret :db/id]
  (let [default-states [:discussion.state/open]
        states (cond-> default-states
                       public? (conj :discussion.state/public))]
    (main-db/clean-and-add-to-db! (assoc discussion-data :discussion/states states)
                                  ::specs/discussion)))

(>defn private-discussion-data
  "Return non public meeting data by id."
  [id]
  [int? :ret ::specs/discussion]
  (toolbelt/pull-key-up
    (d/pull (d/db (new-connection)) discussion-pattern-private id)
    :db/ident))

(defn public-discussions
  "Returns all public discussions."
  []
  (->>
    (d/q
      '[:find (pull ?public-discussions discussion-pattern) ?ts
        :in $ discussion-pattern
        :where [?public-discussions :discussion/states :discussion.state/public ?tx]
        (not-join [?public-discussions]
                  [?public-discussions :discussion/states :discussion.state/deleted])
        [?tx :db/txInstant ?ts]]
      (d/db (new-connection)) discussion-pattern)
    (#(toolbelt/pull-key-up % :db/ident))
    (sort-by second toolbelt/comp-compare)
    (map first)))

(>defn all-statements
  "Returns all statements belonging to a discussion."
  [share-hash]
  [:discussion/share-hash :ret (s/coll-of ::specs/statement)]
  (distinct
    (concat
      (flatten
        (query
          '[:find (pull ?statements statement-pattern)
            :in $ ?share-hash statement-pattern
            :where [?discussion :discussion/share-hash ?share-hash]
            [?arguments :argument/discussions ?discussion]
            (or
              [?arguments :argument/conclusion ?statements]
              [?arguments :argument/premises ?statements])
            [?statements :statement/version _]]
          share-hash main-db/statement-pattern))
      ;; When there are no reactions to the starting statement, the starting statements
      ;; need to be checked explicitly, because there will be no arguments containing them.
      (flatten
        (query
          '[:find (pull ?statements statement-pattern)
            :in $ ?share-hash statement-pattern
            :where [?discussion :discussion/share-hash ?share-hash]
            [?discussion :discussion/starting-statements ?statements]]
          share-hash main-db/statement-pattern)))))

(>defn all-statements-for-graph
  "Returns all statements for a discussion. Specially prepared for node and edge generation."
  [share-hash]
  [:discussion/share-hash :ret sequential?]
  (map
    (fn [statement]
      {:author (-> statement :statement/author :user/nickname)
       :id (:db/id statement)
       :label (if (:statement/deleted? statement)
                config/deleted-statement-text
                (:statement/content statement))})
    (all-statements share-hash)))

(defn all-discussions
  "Shows all discussions currently in the db. The route is only for development purposes.
  Shows discussions in all states."
  []
  (->
    (d/q
      '[:find (pull ?discussions discussion-pattern-private)
        :in $ discussion-pattern-private
        :where [?discussions :discussion/title _]]
      (d/db (new-connection)) discussion-pattern-private)
    (toolbelt/pull-key-up :db/ident)
    flatten))

(>defn check-valid-statement-id-for-discussion
  "Checks whether the statement-id matches the share-hash."
  [statement-id share-hash]
  [:db/id :discussion/share-hash :ret (? :db/id)]
  (or
    (ffirst
      (query
        '[:find ?discussion
          :in $ ?statement ?hash
          :where [?discussion :discussion/share-hash ?hash]
          [?argument :argument/discussions ?discussion]
          (or [?argument :argument/premises ?statement]
              [?argument :argument/conclusion ?statement])]
        statement-id share-hash))
    (ffirst
      (query
        '[:find ?discussion
          :in $ ?statement ?hash
          :where [?discussion :discussion/share-hash ?hash]
          [?discussion :discussion/starting-statements ?statement]]
        statement-id share-hash))))
