(ns schnaq.database.discussion-test
  (:require [clojure.test :refer [deftest testing use-fixtures is are]]
            [schnaq.database.discussion :as db]
            [schnaq.meeting.database :as main-db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest delete-discussion-test
  (let [sample-discussion "simple-hash"
        discussion-count (count (main-db/public-meetings))
        new-discussion-hash "ajskdhajksdh"
        author (main-db/add-user-if-not-exists "Wegi")
        new-public-meeting {:meeting/title "Bla"
                            :meeting/start-date (main-db/now)
                            :meeting/end-date (main-db/now)
                            :meeting/share-hash new-discussion-hash
                            :meeting/author author}]
    (testing "When deleting wrong discussion, throw error."
      (is (nil? (db/delete-discussion "nonsense-8u89jh89z79h88##")))
      (is (string? (db/delete-discussion sample-discussion))))
    (testing "Deleting a public discussion, should decrease the count."
      (let [new-meeting-id (main-db/add-meeting new-public-meeting)]
        (main-db/add-agenda-point "Some-title" "Some-description" new-meeting-id
                                  0 true sample-discussion "edit-hash" author))
      (is (= (inc discussion-count) (count (main-db/public-meetings))))
      (db/delete-discussion new-discussion-hash)
      (is (= discussion-count (count (main-db/public-meetings)))))))

(deftest support-statement!-test
  (testing "Add a new supporting statement to a discussion"
    (let [share-hash "simple-hash"
          user-id (main-db/user-by-nickname "Wegi")
          starting-conclusion (first (db/starting-statements share-hash))
          new-attack (db/support-statement! share-hash user-id (:db/id starting-conclusion)
                                            "This is a new support")]
      (is (= "This is a new support" (-> new-attack :argument/premises first :statement/content)))
      (is (= "Brainstorming ist total wichtig" (-> new-attack :argument/conclusion :statement/content)))
      (is (= :argument.type/support (:argument/type new-attack))))))

(deftest attack-statement!-test
  (testing "Add a new attacking statement to a discussion"
    (let [share-hash "simple-hash"
          user-id (main-db/user-by-nickname "Wegi")
          starting-conclusion (first (db/starting-statements share-hash))
          new-attack (db/attack-statement! share-hash user-id (:db/id starting-conclusion)
                                           "This is a new attack")]
      (is (= "This is a new attack" (-> new-attack :argument/premises first :statement/content)))
      (is (= "Brainstorming ist total wichtig" (-> new-attack :argument/conclusion :statement/content)))
      (is (= :argument.type/attack (:argument/type new-attack))))))

(deftest statements-by-content-test
  (testing "Statements are identified by identical content."
    (is (= 1 (count (db/statements-by-content "dogs can act as watchdogs"))))
    (is (= 1 (count (db/statements-by-content "we have no use for a watchdog"))))
    (is (empty? (db/statements-by-content "foo-baar-ajshdjkahsjdkljsadklja")))))

(deftest all-arguments-for-discussion-test
  (testing "Should return valid arguments for valid discussion."
    (let [share-hash "cat-dog-hash"]
      (is (empty? (db/all-arguments-for-discussion "non-existing-hash-1923hwudahsi")))
      (is (seq (db/all-arguments-for-discussion share-hash)))
      (is (contains? #{:argument.type/undercut :argument.type/support :argument.type/attack}
                     (:argument/type (rand-nth (db/all-arguments-for-discussion share-hash))))))))

(deftest statements-undercutting-premise-test
  (testing "Get arguments, that are undercutting an argument with a certain premise"
    (let [share-hash "simple-hash"
          starting-conclusion (first (db/starting-statements share-hash))
          simple-argument (first (db/all-arguments-for-conclusion (:db/id starting-conclusion)))
          premise-to-undercut-id (-> simple-argument :argument/premises first :db/id)
          desired-statement (first (db/statements-undercutting-premise premise-to-undercut-id))]
      (is (= "Brainstorm hat nichts mit aktiv denken zu tun" (:statement/content desired-statement))))))

(deftest add-starting-statement!-test
  (testing "Test the creation of a valid argument-entity from strings"
    (let [statement "Wow look at this"
          user-id (main-db/user-by-nickname "Test-person")
          meeting-hash "graph-hash"
          _ (db/add-starting-statement! meeting-hash user-id statement)
          starting-statements (db/starting-statements meeting-hash)]
      (testing "Must have three more statements than the vanilla set and one more starting conclusion"
        (is (= 3 (count starting-statements)))))))

(deftest prepare-new-argument-test
  (testing "Test the creation of a valid argument-entity from strings"
    (let [premises ["What a beautifull day" "Hello test"]
          conclusion "Wow look at this"
          user-id (main-db/user-by-nickname "Test-person")
          meeting-hash "graph-hash"
          discussion-id (:db/id (db/discussion-by-share-hash meeting-hash))
          with-id (db/prepare-new-argument discussion-id user-id conclusion premises "temp-id-here")]
      (is (contains? with-id :argument/premises))
      (is (contains? with-id :argument/conclusion))
      (is (contains? with-id :argument/author))
      (is (contains? with-id :argument/version))
      (is (contains? with-id :argument/type))
      (is (contains? with-id :argument/discussions)))))

(deftest pack-premises-test
  (testing "Test the creation of statement-entities from strings"
    (let [premises ["What a beautifull day" "Hello test"]
          user-id (main-db/user-by-nickname "Test-person")
          premise-entities (@#'db/pack-premises premises user-id)]
      (is (= [{:db/id "premise-What a beautifull day",
               :statement/author user-id,
               :statement/content (first premises),
               :statement/version 1}
              {:db/id "premise-Hello test",
               :statement/author user-id,
               :statement/content (second premises),
               :statement/version 1}]
             premise-entities)))))

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
                            :discussion/edit-hash "secret-never-guessed"}]
    (testing "Whether a correct id is returned when valid discussions are transacted."
      (is (number? (db/new-discussion minimal-discussion true)))
      (is (number? (db/new-discussion (assoc minimal-discussion
                                        :discussion/description nil
                                        :discussion/header-image-url "")
                                      false))))
    (testing "Transacting something non-essential should return nil"
      (is (nil? (db/new-discussion (dissoc minimal-discussion :discussion/title) false))))))
