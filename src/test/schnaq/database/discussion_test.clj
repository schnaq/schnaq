(ns schnaq.database.discussion-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest testing use-fixtures is are]]
            [schnaq.database.discussion :as db]
            [schnaq.database.discussion-test-data :as test-data]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each
              schnaq-toolbelt/init-test-delete-db-fixture
              #(schnaq-toolbelt/init-test-delete-db-fixture % test-data/public-discussions))
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest delete-discussion-test
  (let [sample-discussion "simple-hash"
        discussion-count (count (db/public-discussions))
        new-discussion-hash "ajskdhajksdh"
        author (user-db/add-user-if-not-exists "Wegi")
        new-public-discussion {:discussion/title "Bla"
                               :discussion/share-hash new-discussion-hash
                               :discussion/edit-hash "secret-whatever"
                               :discussion/author author}]
    (testing "When deleting wrong discussion, throw error."
      (is (nil? (db/delete-discussion "nonsense-8u89jh89z79h88##")))
      (is (string? (db/delete-discussion sample-discussion))))
    (testing "Deleting a public discussion, should decrease the count."
      (db/new-discussion new-public-discussion true)
      (is (= (inc discussion-count) (count (db/public-discussions))))
      (db/delete-discussion new-discussion-hash)
      (is (= discussion-count (count (db/public-discussions)))))))

(deftest support-statement!-test
  (testing "Add a new supporting statement to a discussion"
    (let [share-hash "simple-hash"
          user-id (user-db/user-by-nickname "Wegi")
          starting-conclusion (first (db/starting-statements share-hash))
          new-support (db/react-to-statement! share-hash user-id (:db/id starting-conclusion)
                                              "This is a new support" :statement.type/support true)
          another-new-reaction (db/react-to-statement! share-hash user-id (:db/id new-support)
                                                       "this is a secret support" :statement.type/support false)]
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
                                             "This is a new attack" :statement.type/attack true)]
      (is (= "This is a new attack" (:statement/content new-attack)))
      (is (= (:db/id starting-conclusion) (:db/id (:statement/parent new-attack))))
      (is (= :statement.type/attack (:statement/type new-attack))))))

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
          _ (db/add-starting-statement! meeting-hash user-id statement false)
          starting-statements (db/starting-statements meeting-hash)]
      (testing "Must have three more statements than the vanilla set and one more starting conclusion"
        (is (= 3 (count starting-statements)))))))

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
      (is (number? (db/new-discussion minimal-discussion true)))
      (is (number? (db/new-discussion (assoc minimal-discussion
                                        :discussion/description nil
                                        :discussion/header-image-url "")
                                      false))))
    (testing "Transacting something non-essential should return nil"
      (is (nil? (db/new-discussion (dissoc minimal-discussion :discussion/title) false))))))

(deftest public-discussions-test
  (testing "Should return all discussions that are marked as public."
    (is (= 1 (count (db/public-discussions))))
    (db/new-discussion {:discussion/title "tester"
                        :discussion/share-hash "newwwwasd"
                        :discussion/edit-hash "secret-yeah"
                        :discussion/author (user-db/add-user-if-not-exists "Wegi")}
                       true)
    (is (= 2 (count (db/public-discussions))))
    (db/new-discussion {:discussion/title "tester private"
                        :discussion/share-hash "newaaaasdasdwwwasd"
                        :discussion/edit-hash "secret-yeah"
                        :discussion/author (user-db/add-user-if-not-exists "Wegi")}
                       false)
    (is (= 2 (count (db/public-discussions))))))

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
                                :discussion/author (user-db/add-user-if-not-exists "Wegi")}
                               true)
          christian-id (user-db/user-by-nickname "Christian")
          first-id (db/add-starting-statement! share-hash christian-id "this is sparta" false)
          second-id (db/add-starting-statement! share-hash christian-id "this is kreta" false)]
      (is (db/check-valid-statement-id-for-discussion first-id "Wegi-ist-der-schönste"))
      (is (db/check-valid-statement-id-for-discussion second-id "Wegi-ist-der-schönste")))))

(deftest children-for-statement-test
  (testing "Get statements (with meta-information), that have a certain parent."
    (let [share-hash "simple-hash"
          starting-conclusion (first (db/starting-statements share-hash))
          meta-premise (first (db/children-for-statement (:db/id starting-conclusion)))]
      (is (= "Man denkt viel nach dabei" (:statement/content meta-premise)))
      (is (= :statement.type/support (:statement/type meta-premise))))))

(deftest valid-discussions-by-hashes-test
  (let [new-discussion-hash "hello-i-am-new-here"
        author (user-db/add-user-if-not-exists "Christian")
        new-public-discussion {:discussion/title "Bla"
                               :discussion/share-hash new-discussion-hash
                               :discussion/edit-hash ":shrug:"
                               :discussion/author author}
        _ (db/new-discussion new-public-discussion true)]
    (testing "Valid discussions should be returned."
      (are [valid share-hashes]
        (= valid (count (db/valid-discussions-by-hashes share-hashes)))
        0 []
        0 ["razupaltuff"]
        1 ["public-share-hash"]
        1 ["public-share-hash" "razupaltuff"]
        1 ["public-share-hash" "public-share-hash-deleted"]
        2 ["public-share-hash" new-discussion-hash]
        2 ["public-share-hash" new-discussion-hash "public-share-hash-deleted"]
        2 ["public-share-hash" new-discussion-hash "public-share-hash-deleted" "razupaltuff"]))))

(deftest change-pro-con-button-test
  (let [share-hash "the-hash"
        author (user-db/add-user-if-not-exists "Mike")
        new-public-discussion {:discussion/title "Lord"
                               :discussion/share-hash share-hash
                               :discussion/edit-hash "secret-whatever"
                               :discussion/author author}
        _ (db/new-discussion new-public-discussion true)
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
          new-statement-id (db/add-starting-statement! (:discussion/share-hash cat-dog-discussion)
                                                       new-user initial-content false)
          statement (fast-pull new-statement-id db/statement-pattern)]
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

(deftest search-schnaq-test
  (testing "Statements with corresponding content should be found when share-hash is known."
    (let [share-hash "cat-dog-hash"]
      (is (= 6 (count (db/search-statements share-hash "cats"))))
      (is (= 2 (count (db/search-statements share-hash "dogs"))))
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
      ;; test for fobidden label
      (is (= [":comment"] (:statement/labels (db/add-label statement-id "anything goes here")))))))

(deftest remove-label-test
  (testing "Correctly remove labels from a statement."
    (let [statement-id (:db/id (first (db/statements-by-content "Brainstorming ist total wichtig")))]
      (db/add-label statement-id ":comment")
      (db/add-label statement-id ":check")
      (is (= [":check"] (:statement/labels (db/remove-label statement-id ":comment"))))
      (is (= [":check"] (:statement/labels (db/remove-label statement-id "anything-else")))))))
