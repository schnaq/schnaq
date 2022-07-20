(ns schnaq.database.discussion-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest testing use-fixtures is are]]
            [schnaq.database.discussion :as db]
            [schnaq.database.main :refer [fast-pull] :as main-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.test-data :as test-data]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest delete-discussion-test
  (let [sample-discussion "simple-hash"
        new-discussion-hash "ajskdhajksdh"
        author (user-db/add-user-if-not-exists "Wegi")
        new-discussion {:discussion/title "Bla"
                        :discussion/share-hash new-discussion-hash
                        :discussion/edit-hash "secret-whatever"
                        :discussion/author author}
        filter-deleted (fn [discussions]
                         (filter #(not (some #{:discussion.state/deleted} (:discussion/states %))) discussions))
        discussion-count (count (filter-deleted (db/all-discussions)))]
    (testing "When deleting wrong discussion, throw error."
      (is (nil? (db/delete-discussion "nonsense-8u89jh89z79h88##")))
      (is (string? (db/delete-discussion sample-discussion))))
    (testing "Deleting a discussion, should decrease the count."
      (db/new-discussion new-discussion)
      (is (= discussion-count (count (filter-deleted (db/all-discussions)))))
      (db/delete-discussion new-discussion-hash)
      (is (= (dec discussion-count) (count (filter-deleted (db/all-discussions))))))))

(deftest support-statement!-test
  (testing "Add a new supporting statement to a discussion"
    (let [share-hash "simple-hash"
          user-id (user-db/user-by-nickname "Wegi")
          starting-conclusion (first (db/starting-statements share-hash))
          new-support (db/react-to-statement! share-hash user-id (:db/id starting-conclusion)
                                              "This is a new support" :statement.type/support
                                              :registered-user? true)
          another-new-reaction (db/react-to-statement! share-hash user-id (:db/id new-support)
                                                       "this is a secret support" :statement.type/support)]
      (is (= "This is a new support" (:statement/content new-support)))
      (is (= (:db/id starting-conclusion) (:db/id (:statement/parent new-support))))
      (is (= :statement.type/support (:statement/type new-support)))
      (is (= "this is a secret support" (:statement/content another-new-reaction)))
      (is (string? (:statement/creation-secret another-new-reaction))))))

(deftest attack-statement!-test
  (testing "Add a new attacking statement to a discussion"
    (let [share-hash "simple-hash"
          user-id (user-db/user-by-nickname "Wegi")
          starting-conclusion (first (db/starting-statements share-hash))
          new-attack (db/react-to-statement! share-hash user-id (:db/id starting-conclusion)
                                             "This is a new attack" :statement.type/attack
                                             :registered-user? true)]
      (is (= "This is a new attack" (:statement/content new-attack)))
      (is (= (:db/id starting-conclusion) (:db/id (:statement/parent new-attack))))
      (is (= :statement.type/attack (:statement/type new-attack)))
      (is (not (:statement/locked? new-attack))))))

(deftest locked-statement!-test
  (testing "Add a new locked statement to a discussion"
    (let [share-hash "simple-hash"
          user-id (user-db/user-by-nickname "Wegi")
          registered-user-id (:db/id (first (user-db/all-registered-users)))
          starting-conclusion (first (db/starting-statements share-hash))
          new-non-locked-attack (db/react-to-statement! share-hash user-id (:db/id starting-conclusion)
                                                        "This is a new unlocked attack" :statement.type/attack
                                                        :locked? true)
          new-locked-attack (db/react-to-statement! share-hash registered-user-id (:db/id starting-conclusion)
                                                    "This is a new locked attack" :statement.type/attack
                                                    :registered-user? true
                                                    :locked? true)]
      (is (not (:statement/locked? new-non-locked-attack)))
      (is (:statement/locked? new-locked-attack)))))

(deftest statements-by-content-test
  (testing "Statements are identified by identical content."
    (is (= 1 (count (db/statements-by-content "dogs can act as watchdogs"))))
    (is (= 1 (count (db/statements-by-content "we should get a cat"))))
    (is (empty? (db/statements-by-content "foo-baar-ajshdjkahsjdkljsadklja")))))

(deftest add-starting-statement!-test
  (testing "Test the creation of a valid statement-entity from strings"
    (let [statement "Wow look at this"
          user-id (user-db/add-user-if-not-exists "Test-person")
          meeting-hash "graph-hash"
          new-starting (db/add-starting-statement! meeting-hash user-id statement)
          starting-statements (db/starting-statements meeting-hash)]
      (testing "Must have three more statements than the vanilla set and one more starting conclusion"
        (is (= 3 (count starting-statements))))
      (testing "Must be unlocked"
        (is (not (:statement/locked? new-starting)))))))

(deftest add-locked-starting-statement-test
  (testing "Test the creation of a valid locked statement-entity from strings"
    (let [statement "Wow look at this"
          user-id (user-db/add-user-if-not-exists "Test-person")
          registered-user-id (:db/id (first (user-db/all-registered-users)))
          meeting-hash "graph-hash"
          new-unlocked-starting (db/add-starting-statement! meeting-hash user-id statement
                                                            :locked? true)
          new-locked-starting (db/add-starting-statement! meeting-hash registered-user-id statement
                                                          :registered-user? true
                                                          :locked? true)]
      (testing "Must be locked if done by registered user"
        (is (not (:statement/locked? new-unlocked-starting)))
        (is (:statement/locked? new-locked-starting))))))

(deftest starting-statements-test
  (testing "Should return all starting-statements from a discussion."
    (let [cat-dog-hash "cat-dog-hash"
          simple-hash "simple-hash"
          graph-hash "graph-hash"]
      (are [result discussion] (= result (count (db/starting-statements discussion)))
        3 cat-dog-hash
        1 simple-hash
        2 graph-hash))))

(deftest new-discussion-test
  (let [minimal-discussion {:discussion/title "Whatevs"
                            :discussion/share-hash "oooooh"
                            :discussion/edit-hash "secret-never-guessed"
                            :discussion/author (user-db/add-user-if-not-exists "Wegi")}]
    (testing "Whether a correct id is returned when valid discussions are transacted."
      (is (number? (db/new-discussion minimal-discussion)))
      (is (number? (db/new-discussion (assoc minimal-discussion
                                             :discussion/description nil
                                             :discussion/header-image-url "")))))
    (testing "Transacting something non-essential should return nil"
      (is (nil? (db/new-discussion (dissoc minimal-discussion :discussion/title)))))))

(deftest all-statements-for-graph-test
  (testing "Returns all statements belonging to a agenda, specially prepared for graph-building."
    (let [graph-hash "graph-hash"
          statements (db/all-statements-for-graph graph-hash)]
      (is (= 7 (count statements)))
      (is (= 1 (count (filter #(= "foo" (:label %)) statements)))))))

(deftest discussion-deleted?-test
  (testing "Test whether deleted discussions are correctly recognized."
    (is (db/discussion-deleted? "public-share-hash-deleted"))
    (is (not (db/discussion-deleted? "public-share-hash")))))

(deftest valid-statement-id-and-meeting?-test
  (testing "Test the function that checks whether a statement belongs to a certain meeting."
    (let [share-hash "Wegi-ist-der-schönste"
          _ (db/new-discussion {:discussion/title "test-meet"
                                :discussion/share-hash share-hash
                                :discussion/edit-hash (str "secret-" share-hash)
                                :discussion/author (user-db/add-user-if-not-exists "Wegi")})
          christian-id (user-db/user-by-nickname "Christian")
          first-id (:db/id (db/add-starting-statement! share-hash christian-id "this is sparta"))
          second-id (:db/id (db/add-starting-statement! share-hash christian-id "this is kreta"))]
      (is (db/check-valid-statement-id-for-discussion first-id "Wegi-ist-der-schönste"))
      (is (db/check-valid-statement-id-for-discussion second-id "Wegi-ist-der-schönste")))))

(deftest children-for-statement-test
  (testing "Get statements (with meta-information), that have a certain parent."
    (let [share-hash "simple-hash"
          starting-conclusion (first (db/starting-statements share-hash))
          meta-premise (first (db/children-for-statement (:db/id starting-conclusion)))]
      (is (= "Man denkt viel nach dabei" (:statement/content meta-premise)))
      (is (= :statement.type/support (:statement/type meta-premise))))))

(deftest discussions-by-share-hashes-test
  (let [new-discussion-hash "hello-i-am-new-here"
        author (user-db/add-user-if-not-exists "Christian")
        new-public-discussion {:discussion/title "Bla"
                               :discussion/share-hash new-discussion-hash
                               :discussion/edit-hash ":shrug:"
                               :discussion/author author}
        _ (db/new-discussion new-public-discussion)]
    (testing "Valid discussions should be returned."
      (are [valid share-hashes]
           (= valid (count (db/discussions-by-share-hashes share-hashes)))
        0 []
        0 ["razupaltuff"]
        1 ["simple-hash"]
        1 ["simple-hash" "razupaltuff"]
        1 ["simple-hash" "public-share-hash-deleted"]
        2 ["simple-hash" new-discussion-hash]
        2 ["simple-hash" new-discussion-hash "public-share-hash-deleted"]
        2 ["simple-hash" new-discussion-hash "public-share-hash-deleted" "razupaltuff"]))))

(deftest change-pro-con-button-test
  (let [share-hash "the-hash"
        author (user-db/add-user-if-not-exists "Mike")
        new-public-discussion {:discussion/title "Lord"
                               :discussion/share-hash share-hash
                               :discussion/edit-hash "secret-whatever"
                               :discussion/author author}
        _ (db/new-discussion new-public-discussion)
        schnaq-before (db/discussion-by-share-hash share-hash)
        _ (db/set-disable-pro-con share-hash true)
        schnaq-after (db/discussion-by-share-hash share-hash)
        _ (db/set-disable-pro-con share-hash false)
        schnaq-after-2 (db/discussion-by-share-hash share-hash)
        disabled-pro-con? #(nil? (some #{:discussion.state/disable-pro-con} (:discussion/states %)))]
    (testing "Testing change pro-con-button tag"
      (is (disabled-pro-con? schnaq-before)
          "schnaq should not contain disable button tag per default")
      (is (not (disabled-pro-con? schnaq-after))
          "schnaq should include disable button tag after setting it")
      (is (disabled-pro-con? schnaq-after-2)
          "schnaq should no longer include disable button tag after removing it"))))

(deftest change-statement-text-test
  (testing "Test whether editing statement-content works correctly."
    (let [cat-dog-discussion (first (db/all-discussions-by-title "Cat or Dog?"))
          initial-content "Unmodified-statement"
          modified-content "Whats up in dis here house?"
          modified-type :statement.type/neutral
          new-user (user-db/add-user-if-not-exists "Wugiperson")
          statement (db/add-starting-statement! (:discussion/share-hash cat-dog-discussion)
                                                new-user initial-content)]
      (is (= initial-content (:statement/content statement)))
      (let [modified-statement (db/change-statement-text-and-type statement modified-type modified-content)]
        (is (= modified-content (:statement/content modified-statement)))
        (is (nil? (:statement/type modified-statement)))
        (is (s/valid? ::specs/statement modified-statement))))))

(deftest update-authors-from-secrets-test
  (testing "Change of author, when a registered user claims the statement."
    (let [statement (first (db/starting-statements "simple-hash"))
          original-author (user-db/user-by-nickname "Christian")
          registered-user (fast-pull [:user.registered/keycloak-id "59456d4a-6950-47e8-88d8-a1a6a8de9276"])]
      ;; Using the wrong secret should do nothing
      (db/update-authors-from-secrets {(:db/id statement) "wrong-secret"} (:db/id registered-user))
      (is (= original-author (-> (first (db/starting-statements "simple-hash")) :statement/author :db/id)))
      ;; Now update the author
      (db/update-authors-from-secrets {(:db/id statement) "secret-creation-secret"} (:db/id registered-user))
      (is (= (:db/id registered-user) (-> (first (db/starting-statements "simple-hash")) :statement/author :db/id))))))

(deftest search-statements-test
  (testing "Statements with corresponding content should be found when share-hash is known."
    (let [share-hash "cat-dog-hash"]
      (is (= 10 (count (db/search-statements share-hash "cats"))))
      (is (= 7 (count (db/search-statements share-hash "dogs"))))
      (is (= 1 (count (db/search-statements share-hash "both")))))))

(deftest summary-request-test
  (testing "Create a new summary when requested, otherwise update the requested at tag."
    (let [share-hash "cat-dog-hash"
          keycloak-user-id "59456d4a-6950-47e8-88d8-a1a6a8de9276"
          new-summary (db/summary-request share-hash keycloak-user-id)]
      (is (map? new-summary))
      (let [updated-summary (db/summary-request share-hash keycloak-user-id)]
        (is (= 1
               (compare (:summary/requested-at updated-summary)
                        (:summary/requested-at new-summary))))))))

(deftest history-for-statement-test
  (testing "Whether history works linearly for any statement."
    (let [historyless-content "Brainstorming ist total wichtig"
          history-1 (db/history-for-statement (:db/id (first (db/statements-by-content historyless-content))))
          content-with-history "Denken sorgt nur für Kopfschmerzen. Lieber den Donaldo machen!"
          history-3 (db/history-for-statement (:db/id (first (db/statements-by-content content-with-history))))]
      (is (= 1 (count history-1)))
      (is (= historyless-content (:statement/content (first history-1))))
      (is (= 3 (count history-3)))
      (is (= content-with-history (:statement/content (last history-3))))
      (is (= historyless-content (:statement/content (first history-3)))))))

(deftest delete-statement!-test
  (testing "The correct behaviour of statement deletion"
    (let [root (:db/id (first (db/statements-by-content "Brainstorming ist total wichtig")))
          child (:db/id (first (db/statements-by-content "Man denkt viel nach dabei")))
          grandchild (:db/id (first (db/statements-by-content "Denken sorgt nur für Kopfschmerzen. Lieber den Donaldo machen!")))]
      (db/delete-statement! root)
      (is (:statement/deleted? (fast-pull root [:statement/deleted?])))
      (db/delete-statement! grandchild)
      (is (nil? (fast-pull grandchild [:statement/content])))
      (db/delete-statement! child)
      (is (nil? (fast-pull child [:statement/content])))
      (is (nil? (fast-pull root [:statement/content]))))))

(deftest add-label-test
  (testing "Tests whether labels are added correctly."
    (let [statement-id (:db/id (first (db/statements-by-content "Brainstorming ist total wichtig")))]
      (is (= [":comment"] (:statement/labels (db/add-label statement-id ":comment"))))
      ;; test for forbidden label
      (is (= [":comment"] (:statement/labels (db/add-label statement-id "anything goes here")))))))

(deftest remove-label-test
  (testing "Correctly remove labels from a statement."
    (let [statement-id (:db/id (first (db/statements-by-content "Brainstorming ist total wichtig")))]
      (db/add-label statement-id ":comment")
      (db/add-label statement-id ":check")
      (is (= [":check"] (:statement/labels (db/remove-label statement-id ":comment"))))
      (is (= [":check"] (:statement/labels (db/remove-label statement-id "anything-else")))))))

(deftest search-similar-questions-test
  (testing "Test whether the similar questions search works as intended."
    (let [share-hash "cat-dog-hash"]
      ;; Searching for "we" should give 3 first level statements. Permutations should give nothing.
      (is (= 3 (count (db/search-similar-questions share-hash "we"))))
      (is (empty? (db/search-similar-questions share-hash "ew")))
      ;; dog and dok should give similar results. Case should also not matter so DOG is the same
      (is (= 2
             (count (db/search-similar-questions share-hash "dog"))
             (count (db/search-similar-questions share-hash "dok"))
             (count (db/search-similar-questions share-hash "DOG"))
             (count (db/search-similar-questions share-hash "DOK"))))
      (is (empty? (db/search-similar-questions share-hash "dgo")))
      ;; Five or more chars should allow for two errors. Should itself also matches would, which gives it one more hit,
      ;; than the misspelled versions
      (is (= 3 (count (db/search-similar-questions share-hash "should"))))
      (is (= 2
             (count (db/search-similar-questions share-hash "schoult"))
             (count (db/search-similar-questions share-hash "shald"))))
      (is (empty? (db/search-similar-questions share-hash "scholt"))))))

(deftest new-statement-ids-for-user-test
  (let [test-user (user-db/private-user-by-keycloak-id "59456d4a-6950-47e8-88d8-a1a6a8de9276")]
    (testing "test-user gets a list of new statements."
      (let [new-statements-in-discussion (db/new-statement-ids-for-user (:user.registered/keycloak-id test-user) "cat-dog-hash")]
        (is (pos-int? (count new-statements-in-discussion)))
        (is (s/valid? (s/coll-of :db/id) new-statements-in-discussion))))
    (testing "User who is not part of a discussion, gets no list of new statements."
      (let [new-statements-in-discussion (db/new-statement-ids-for-user (:user.registered/keycloak-id test-user) "definitely not a valid hash")]
        (is (zero? (count new-statements-in-discussion)))))))

(deftest new-statements-by-discussion-hash-test
  (let [test-user (user-db/private-user-by-keycloak-id "59456d4a-6950-47e8-88d8-a1a6a8de9276")
        share-hash "cat-dog-hash"
        anonymous-user-id (user-db/add-user-if-not-exists "Anonymous")
        new-statements-before (get (db/new-statements-by-discussion-hash test-user) share-hash)
        _ (db/add-starting-statement! "cat-dog-hash" anonymous-user-id "New Post!")
        new-statements-after (get (db/new-statements-by-discussion-hash test-user) share-hash)]
    (testing "Adding a new statement from a different user results in an unseen statement for the test-user."
      (is (not= new-statements-after new-statements-before))
      (is (> (count new-statements-after) (count new-statements-before))))))

(deftest mark-all-statements-of-discussion-as-read-test
  (let [test-user (user-db/private-user-by-keycloak-id "59456d4a-6950-47e8-88d8-a1a6a8de9276")
        share-hash "cat-dog-hash"
        new-statements-in-discussion (get (db/new-statements-by-discussion-hash test-user) share-hash)
        _ (db/mark-all-statements-of-discussion-as-read "59456d4a-6950-47e8-88d8-a1a6a8de9276" share-hash)
        statements-in-discussion-after-clearing (get (db/new-statements-by-discussion-hash test-user) share-hash)]
    (testing "Before clearing unseen statements, there should be unseen statements in the collection."
      (is (not (zero? (count new-statements-in-discussion))))
      (is (nil? statements-in-discussion-after-clearing)))))

(deftest new-statements-within-time-slot-test
  (testing "Between now and now are no new statements created."
    (is (zero? (count (db/new-statements-within-time-slot "cat-dog-hash" (main-db/now))))))
  (testing "After a moment, the new statements are stored."
    (is (= 18 (count (db/new-statements-within-time-slot "cat-dog-hash" (main-db/minutes-ago 1)))))))

(deftest discussions-with-new-statements-test
  (let [discussion (db/discussion-by-share-hash "cat-dog-hash")]
    (testing "No new statements should be added in a zero time-slot."
      (is (zero? (count (db/discussions-with-new-statements [discussion] (main-db/now))))))
    (testing "In the last minute, 18 new statements were added to the cat-dog discussion."
      (is (= 18 (:total (:new-statements (first (db/discussions-with-new-statements [discussion] (main-db/minutes-ago 1))))))))))

(deftest all-statements-from-user-test
  (testing "Return statements for users if user has created some statements."
    (is (= 1 (count (db/all-statements-from-user (:user.registered/keycloak-id test-data/kangaroo)))))
    (is (zero? (count (db/all-statements-from-user (:user.registered/keycloak-id test-data/alex)))))))

(deftest discussions-from-user-test
  (testing "If user has created discussions, return them."
    (is (= 1 (count (db/discussions-from-user (:user.registered/keycloak-id test-data/alex)))))
    (is (zero? (count (db/discussions-from-user (:user.registered/keycloak-id test-data/kangaroo)))))))

(deftest children-from-statements-test
  (testing "Find all children for a set of statements."
    (let [startings (db/starting-statements "cat-dog-hash")
          children (db/children-from-statements startings)]
      (is (= 6 (count children)))
      (is (= 6 (count (filter :statement/content children)))))))
