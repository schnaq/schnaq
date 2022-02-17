(ns schnaq.database.discussion
  "Discussion related functions interacting with the database."
  (:require [clj-fuzzy.metrics :as fuzzy-metrics]
            [clojure.data :as cdata]
            [clojure.spec.alpha :as s]
            [clojure.string :as cstring]
            [com.fulcrologic.guardrails.core :refer [>defn ? >defn-]]
            [datomic.api :as d]
            [schnaq.config :as config]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.access-codes :as ac]
            [schnaq.database.main :refer [transact query fast-pull] :as main-db]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.toolbelt :as toolbelt]
            [schnaq.user :as user]
            [taoensso.timbre :as log])
  (:import (java.lang Character)
           (java.util UUID Date)))

(def ^:private rules
  "Discussion rules for common use in db queries."
  '[;; all statements for a discussion by share-hash
    [(all-statements ?share-hash ?statements)
     [?discussion :discussion/share-hash ?share-hash]
     [?statements :statement/discussions ?discussion]
     (not [?statements :statement/deleted? true])]])

(>defn starting-statements
  "Returns all starting-statements belonging to a discussion."
  [share-hash]
  [:db/id :ret (s/coll-of ::specs/statement)]
  (toolbelt/pull-key-up
   (query
    '[:find [(pull ?statements pattern) ...]
      :in $ ?share-hash pattern
      :where [?discussion :discussion/share-hash ?share-hash]
      [?discussion :discussion/starting-statements ?statements]]
    share-hash patterns/statement-with-children)))

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

(defn sub-statement-count
  "Takes a list of statement-ids and returns a map {id meta-info-map} for the statements."
  [statement-ids]
  (apply merge
         (map #(hash-map (first %) (second %))
              (query
               '[:find ?statement-ids (count ?children)
                 :in $ % [?statement-ids ...]
                 :where
                 ;; We pick a transitive depth of 7 as a sweet-spot. The deeper the rule
                 ;; goes, the more complicated and slower the query gets. 10 is too slow
                 ;; for our purposes, but 5 is maybe not deep enough for the typical discussion
                 ;; 7 offers a good speed while being deep enough for most discussions.
                 (transitive-child-7 ?statement-ids ?children)]
               (transitive-child-rules 7) statement-ids))))

(defn- pull-discussion
  "Pull a discussion from a database."
  [share-hash pattern]
  [:discussion/share-hash vector? :ret ::specs/discussion]
  (-> (fast-pull [:discussion/share-hash share-hash] pattern)
      toolbelt/pull-key-up
      ac/remove-invalid-and-pull-up-access-codes))

(defn discussion-by-share-hash
  "Query discussion and apply public discussion pattern to it."
  [share-hash]
  (pull-discussion share-hash patterns/discussion))

(defn discussion-by-share-hash-private
  "Query discussion and apply the private discussion pattern."
  [share-hash]
  (pull-discussion share-hash patterns/discussion-private))

(>defn discussions-by-share-hashes
  "Returns all discussions that are valid (non deleted e.g.). Input is a collection of share-hashes."
  [share-hashes]
  [(s/coll-of :discussion/share-hash) :ret (s/coll-of ::specs/discussion)]
  (-> (query
       '[:find [(pull ?discussions discussion-pattern) ...]
         :in $ [?share-hashes ...] discussion-pattern
         :where [?discussions :discussion/share-hash ?share-hashes]
         (not-join [?discussions]
                   [?discussions :discussion/states :discussion.state/deleted])]
       share-hashes patterns/discussion-minimal)
      toolbelt/pull-key-up))

(>defn children-for-statement
  "Returns all children for a statement. (Statements that have the input set as a parent)."
  [parent-id]
  [:db/id :ret (s/coll-of ::specs/statement)]
  (-> (query '[:find [(pull ?children statement-pattern) ...]
               :in $ ?parent statement-pattern
               :where [?children :statement/parent ?parent]]
             parent-id patterns/statement-with-children)
      toolbelt/pull-key-up))

(defn delete-statement!
  "Deletes a statement. Hard delete if there are no children, delete flag if there are.
  Check the same for the parent and continue recursively until the root."
  [statement-id]
  (let [statement-to-delete (fast-pull statement-id patterns/statement)
        parent (fast-pull (get-in statement-to-delete [:statement/parent :db/id])
                          [:db/id
                           :statement/deleted?])
        children (children-for-statement statement-id)]
    (if (seq children)
      (do
        (log/info "Statement will set deletion marker:" statement-id)
        (transact [[:db/add statement-id :statement/deleted? true]])
        :set-marker)
      (do
        (log/info "Statement id scheduled for deletion:" statement-id)
        @(transact [[:db/retractEntity statement-id]])
        (when (:statement/deleted? parent)
          (delete-statement! (:db/id parent)))
        :deleted))))

(>defn delete-statements!
  "Deletes all statements, without explicitly checking anything."
  [statement-ids]
  [(s/coll-of :db/id) :ret associative?]
  (log/info "Statement ids scheduled for deletion:" statement-ids)
  (map delete-statement! statement-ids))

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
       patterns/discussion title)
      toolbelt/pull-key-up))

(>defn statements-by-content
  "Returns all statements that have the matching `content`."
  [content]
  [:statement/content :ret (s/coll-of ::specs/statement)]
  (query
   '[:find [(pull ?statements statement-pattern) ...]
     :in $ statement-pattern ?content
     :where [?statements :statement/content ?content]]
   patterns/statement content))

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
    [(cond-> {:db/id (str "new-child-" new-content)
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
     (fast-pull new-child-id patterns/statement-with-secret db-after))))

(>defn new-discussion
  "Adds a new discussion to the database."
  [discussion-data]
  [map? :ret :db/id]
  (main-db/clean-and-add-to-db! (assoc discussion-data
                                       :discussion/created-at (Date.)
                                       :discussion/creation-secret (.toString (UUID/randomUUID)))
                                ::specs/discussion))

(>defn secret-discussion-data
  "Return non-public discussion data by id."
  [id]
  [int? :ret ::specs/discussion]
  (ac/remove-invalid-and-pull-up-access-codes
   (toolbelt/pull-key-up
    (fast-pull id (conj patterns/discussion-private :discussion/creation-secret)))))

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
        db-transaction (if disable?
                         disable-transaction
                         enable-transaction)]
    (main-db/transact db-transaction)))

(defn mods-mark-only!
  "Allow either mods or everybody to mark correct answers."
  [share-hash mods-only?]
  (let [disable-transaction [[:db/retract [:discussion/share-hash share-hash]
                              :discussion/states :discussion.state.qa/mark-as-moderators-only]]
        enable-transaction [[:db/add [:discussion/share-hash share-hash]
                             :discussion/states :discussion.state.qa/mark-as-moderators-only]]
        db-transaction (if mods-only?
                         enable-transaction
                         disable-transaction)]
    (main-db/transact db-transaction)))

(defn edit-title
  "Edits a schnaq title by share-hash"
  [share-hash title]
  (let [enable-transaction [[:db/add [:discussion/share-hash share-hash]
                             :discussion/title title]]]
    (main-db/transact enable-transaction)))

(>defn all-statements
  "Returns all statements belonging to a discussion."
  [share-hash]
  [:discussion/share-hash :ret (s/coll-of ::specs/statement)]
  (->
   (query '[:find [(pull ?statements statement-pattern) ...]
            :in $ % ?share-hash statement-pattern
            :where (all-statements ?share-hash ?statements)]
          rules share-hash patterns/statement)
   toolbelt/pull-key-up))

(>defn all-statements-from-others
  "Returns all statements belonging to a discussion which are not from a user."
  [keycloak-id share-hash]
  [:user.registered/keycloak-id :discussion/share-hash :ret (s/coll-of ::specs/statement)]
  (->
   (query '[:find [(pull ?statements statement-pattern) ...]
            :in $ % ?keycloak-id ?share-hash statement-pattern
            :where (all-statements ?share-hash ?statements)
            [?statements :statement/author ?author]
            (not [?author :user.registered/keycloak-id ?keycloak-id])]
          rules keycloak-id share-hash patterns/statement)
   toolbelt/pull-key-up))

(defn- new-statements-for-user
  "Retrieve new statements of a discussion for a user"
  [keycloak-id discussion-hash]
  (let [all-statements (all-statements-from-others keycloak-id discussion-hash)
        seen-statements (user-db/known-statement-ids keycloak-id discussion-hash)]
    (remove (fn [statement]
              (some #(= % (:db/id statement)) seen-statements))
            all-statements)))

(>defn new-statements-within-time-slot
  "Returns all new statements, which were created between now and the provided
  timestamp. Looks up the discussion in the current `db` and creates a
  difference between now and the timestamp, which contains all new datoms
  created in this time slot."
  [share-hash timestamp]
  [:discussion/share-hash inst? :ret (s/coll-of ::specs/statement)]
  (let [db (d/db (main-db/new-connection))]
    (d/q '[:find [(pull ?statements pattern) ...]
           :in $ $time-slot ?share-hash pattern
           :where
           [?discussion :discussion/share-hash ?share-hash]
           [$time-slot ?statements :statement/discussions ?discussion]
           (not [?statements :statement/deleted? true])]
         db (d/since db timestamp) share-hash patterns/statement)))

(>defn- new-statements+author->discussion
  "Check for new statements and the corresponding authors in the discussion."
  [discussion timestamp]
  [::specs/discussion inst? :ret ::specs/discussion]
  (let [new-statements (new-statements-within-time-slot (:discussion/share-hash discussion) timestamp)
        from-these-authors (set (map #(get-in % [:statement/author :db/id]) new-statements))]
    (assoc discussion :new-statements {:total (count new-statements)
                                       :authors from-these-authors})))

(>defn discussions-with-new-statements
  "Return all discussions and count their statements, if they received new
  statements between now and the given timestamp."
  [discussions timestamp]
  [(s/coll-of ::specs/discussion) inst? :ret (s/coll-of ::specs/discussion)]
  (->> discussions
       (map #(new-statements+author->discussion % timestamp))
       (remove #(zero? (:total (:new-statements %))))))

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
             patterns/discussion-private)
      toolbelt/pull-key-up))

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
    (toolbelt/pull-key-up (fast-pull statement-id patterns/statement))))

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

(defn levenshtein-max?
  "Levenshtein-Helper for datomic to have a maximum distance."
  [max string-1 string-2]
  (>= max (fuzzy-metrics/levenshtein
           (cstring/lower-case string-1)
           (cstring/lower-case string-2))))

(defn- alphanumeric?
  "Checks whether some char is a Letter or a Digit."
  [char-to-test]
  (or
   (Character/isLetter ^char char-to-test)
   (Character/isDigit ^char char-to-test)))

(defn tokenize-string
  "Tokenizes a string into single tokens for the purpose of searching."
  [content]
  (->> (cstring/split content #"\s")
       (remove cstring/blank?)
       ;; Remove punctuation when generating token
       (map #(cond
               (not (alphanumeric? (first %))) (subs % 1)
               (not (alphanumeric? (last %))) (subs % 0 (dec (count %)))
               :else %))))

(defn- add-synonyms-to-list
  "Go through a list and add all synonyms that can be found in our dictionary.\n
  As of 2021/12 it works for german. We have no language detection."
  [tokens]
  (reduce
   #(if-let [synonyms (get @toolbelt/synonyms-german (cstring/lower-case %2))]
      (concat %1 [%2] synonyms)
      (conj %1 %2))
   [] tokens))

(defn- dynamic-search-query
  "Builds the dynamic search query. One of the bound params needs to be `?statements`.
  `custom-part needs to be a quoted vector."
  [custom-part]
  (concat '[:find [(pull ?statements statement-pattern) ...]
            :in $ statement-pattern ?share-hash [?search-tokens ...] ?distance
            :with ?tokenized-content
            :where [?discussion :discussion/share-hash ?share-hash]]
          custom-part
          '[[?statements :statement/content ?content]
            (not [?statements :statement/deleted? true])
            [(schnaq.database.discussion/tokenize-string ?content) [?tokenized-content ...]]
            [(schnaq.database.discussion/levenshtein-max? ?distance ?search-tokens ?tokenized-content)]]))

(>defn- search-similar-with-n-levenshtein
  "Searches for similar content with a levenshtein distance of n.
  One of the bound params needs to be `?statements`.\n `custom-part needs to be a quoted vector."
  [share-hash search-tokens distance custom-part pattern]
  [:discussion/share-hash (s/coll-of ::specs/non-blank-string) int? (s/coll-of vector?) vector?
   :ret (s/coll-of ::specs/statement)]
  (let [tokens-with-synonyms (add-synonyms-to-list search-tokens)]
    ;; Für Synonyme wird ebenfalls eine Distanz berechnet. Wenn dabei zu viel Müll rauskommt, sollte
    ;; das geändert werden.
    (->>
     (query (dynamic-search-query custom-part)
            pattern share-hash tokens-with-synonyms distance)
     frequencies
     toolbelt/pull-key-up)))

(>defn- generic-statement-search
  "A generic search for statements. Provide which statements you want to search. (quoted vector)"
  [share-hash search-string custom-part pattern]
  [:discussion/share-hash ::specs/non-blank-string (s/coll-of vector?) vector? :ret (s/coll-of ::specs/statement)]
  (let [search-tokens (tokenize-string search-string)
        two-and-less-tokens (filter #(>= 2 (count %)) search-tokens)
        three-four-tokens (filter #(or (= 3 (count %))
                                       (= 4 (count %))) search-tokens)
        five-and-more-tokens (filter #(< 4 (count %)) search-tokens)
        results<=2 (search-similar-with-n-levenshtein share-hash two-and-less-tokens 0 custom-part pattern)
        results=3or4 (search-similar-with-n-levenshtein share-hash three-four-tokens 1 custom-part pattern)
        results>5 (search-similar-with-n-levenshtein share-hash five-and-more-tokens 2 custom-part pattern)]
    (->>
     (merge-with + results<=2 results=3or4 results>5)
     (sort-by val >)
     (map first))))

(>defn search-statements
  "Searches the content of statements in a discussion and returns the corresponding statements."
  [share-hash search-string]
  [:discussion/share-hash ::specs/non-blank-string :ret (s/coll-of ::specs/statement)]
  (generic-statement-search share-hash search-string
                            '[[?statements :statement/discussions ?discussion]]
                            patterns/statement))

(>defn search-similar-questions
  "Search starting Conclusions (Questions in QA) and try to provide answers if there are any."
  [share-hash search-string]
  [:discussion/share-hash ::specs/non-blank-string :ret (s/coll-of ::specs/statement)]
  (generic-statement-search share-hash search-string
                            '[[?discussion :discussion/starting-statements ?statements]]
                            patterns/statement-with-children))

(>defn- request-summary
  "Updates an existing summary request and returns the updated version."
  [summary-id requester]
  [:db/id :summary/requester :ret ::specs/summary]
  (let [tx-result @(transact [[:db/add summary-id :summary/requested-at (Date.)]
                              [:db/add summary-id :summary/requester requester]])]
    (fast-pull summary-id patterns/summary (:db-after tx-result))))

(>defn summary
  "Return a summary if it exists for a discussion's share-hash."
  [share-hash]
  [:discussion/share-hash :ret (? ::specs/summary)]
  (query '[:find (pull ?summary summary-pattern) .
           :in $ ?share-hash summary-pattern
           :where [?discussion :discussion/share-hash ?share-hash]
           [?summary :summary/discussion ?discussion]]
         share-hash patterns/summary))

(>defn summary-request
  "Creates a new summary-request if there is none for the discussion. Otherwise, updates the request-time."
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
         patterns/summary-with-discussion))

(>defn update-summary
  [share-hash new-text]
  [:discussion/share-hash :summary/text :ret ::specs/summary]
  (when-let [summary (:db/id (summary share-hash))]
    (let [tx-result @(transact [{:db/id summary
                                 :summary/text new-text
                                 :summary/created-at (Date.)}])]
      (fast-pull summary patterns/summary-with-discussion (:db-after tx-result)))))

(defn history-for-statement
  "Takes a statement entity and returns the statement-history."
  [statement-id]
  (toolbelt/pull-key-up
   (loop [current statement-id
          history (list)]
     (let [full-statement (fast-pull current patterns/statement)]
       (if (:statement/parent full-statement)
         (recur (-> full-statement :statement/parent :db/id) (conj history full-statement))
         (conj history full-statement))))))

(>defn add-label
  "Adds a label to a statement. If label is already applied, nothing changes."
  [statement-id label]
  [:db/id :statement/label :ret ::specs/statement]
  (toolbelt/pull-key-up
   (if (shared-config/allowed-labels label)
     (->> @(transact [[:db/add statement-id :statement/labels label]])
          :db-after
          (fast-pull statement-id patterns/statement))
     (fast-pull statement-id patterns/statement))))

(>defn remove-label
  "Deletes a label if it is in the statement-set. Otherwise, nothing changes."
  [statement-id label]
  [:db/id :statement/label :ret ::specs/statement]
  (toolbelt/pull-key-up
   (->> @(transact [[:db/retract statement-id :statement/labels label]])
        :db-after
        (fast-pull statement-id patterns/statement))))

;; -----------------------------------------------------------------------------

(>defn new-statement-ids-for-user
  "Retrieve ids of new statements of a discussion for a user"
  [keycloak-id discussion-hash]
  [:user.registered/keycloak-id :discussion/share-hash :ret (s/coll-of :db/id)]
  (map :db/id (new-statements-for-user keycloak-id discussion-hash)))

(>defn- build-discussion-diff-list
  "Build a map of discussion hashes with new statements as values"
  [keycloak-id discussion-hashes]
  [:user.registered/keycloak-id (s/coll-of :discussion/share-hash) :ret ::specs/share-hash-statement-id-mapping]
  (reduce conj
          (map (fn [discussion-hash]
                 {discussion-hash (new-statement-ids-for-user
                                   keycloak-id discussion-hash)})
               discussion-hashes)))

(>defn new-statements-by-discussion-hash
  "Returns a map containing tuples with the share-hash and a list of all new statements.
   
   Example: `{\"ad508972-5e33-4b9b-b446-d5a33c81ab8d\" (17592186047296 17592186047298 17592186047318 17592186047324 17592186047326)}`"
  [{:user.registered/keys [keycloak-id visited-schnaqs]}]
  [::specs/registered-user :ret ::specs/share-hash-statement-id-mapping]
  (let [discussion-hashes (map :discussion/share-hash visited-schnaqs)]
    (into {}
          (filter
           (fn [[_ statements]] (seq statements))
           (build-discussion-diff-list keycloak-id discussion-hashes)))))

(>defn mark-all-statements-as-read!
  [keycloak-id]
  [:user.registered/keycloak-id :ret ::specs/share-hash-statement-id-mapping]
  (let [user (user-db/private-user-by-keycloak-id keycloak-id)
        unread (new-statements-by-discussion-hash user)]
    (user-db/update-visited-statements keycloak-id unread)
    unread))

(>defn mark-all-statements-of-discussion-as-read
  "Query all new statements for a user and mark them as read."
  [keycloak-id share-hash]
  [:user.registered/keycloak-id :discussion/share-hash :ret any?]
  (let [new-statements-with-share-hash (build-discussion-diff-list keycloak-id [share-hash])]
    (user-db/update-visited-statements keycloak-id new-statements-with-share-hash)))

;; -----------------------------------------------------------------------------

(>defn- build-schnaq-secrets-map
  "Creates a secrets map for a collection of statements.
  When there is no secret, the statement is skipped."
  [share-hashes]
  [(? (s/coll-of :db/id)) :ret (? map?)]
  (when share-hashes
    (into {}
          (query
           '[:find ?share-hash ?secret
             :in $ [?share-hash ...]
             :where [?schnaq :discussion/share-hash ?share-hash]
             [?schnaq :discussion/creation-secret ?secret]]
           share-hashes))))

(>defn update-schnaq-authors
  "Takes a dictionary of share-hashes mapped to creation secrets and sets the passed author
  as their author, if the secrets are correct."
  [secrets-map author-id]
  [(? map?) :db/id :ret any?]
  (let [validated-secrets-map (build-schnaq-secrets-map (keys secrets-map))
        [_ _ valid-secrets] (cdata/diff secrets-map validated-secrets-map)]
    (when valid-secrets
      @(transact
        (mapv #(vector :db/add [:discussion/share-hash %] :discussion/author author-id) (keys valid-secrets))))))
