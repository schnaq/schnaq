(ns schnaq.database.discussion-test
  (:require [clojure.test :refer [deftest testing use-fixtures is are]]
            [schnaq.database.discussion :as db]
            [schnaq.database.discussion-test-data :as test-data]
            [schnaq.meeting.database :as main-db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each
              schnaq-toolbelt/init-test-delete-db-fixture
              #(schnaq-toolbelt/init-test-delete-db-fixture % test-data/public-discussions))
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest delete-discussion-test
  (let [sample-discussion "simple-hash"
        discussion-count (count (db/public-discussions))
        new-discussion-hash "ajskdhajksdh"
        author (main-db/add-user-if-not-exists "Wegi")
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
                            :discussion/edit-hash "secret-never-guessed"
                            :discussion/author (main-db/add-user-if-not-exists "Wegi")}]
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
                        :discussion/author (main-db/add-user-if-not-exists "Wegi")}
                       true)
    (is (= 2 (count (db/public-discussions))))
    (db/new-discussion {:discussion/title "tester private"
                        :discussion/share-hash "newaaaasdasdwwwasd"
                        :discussion/edit-hash "secret-yeah"
                        :discussion/author (main-db/add-user-if-not-exists "Wegi")}
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
                                           :discussion/author (main-db/add-user-if-not-exists "Wegi")}
                                          true)
          christian-id (main-db/user-by-nickname "Christian")
          first-id (db/add-starting-statement! share-hash christian-id "this is sparta")
          second-id (db/add-starting-statement! share-hash christian-id "this is kreta")]
      (is (db/check-valid-statement-id-for-discussion first-id "Wegi-ist-der-schönste"))
      (is (db/check-valid-statement-id-for-discussion second-id "Wegi-ist-der-schönste")))))

(deftest all-premises-for-conclusion
  (testing "Get arguments (with meta-information), that have a certain conclusion"
    (let [share-hash "simple-hash"
          starting-conclusion (first (db/starting-statements share-hash))
          meta-premise (first (db/all-premises-for-conclusion (:db/id starting-conclusion)))]
      (is (= "Man denkt viel nach dabei" (:statement/content meta-premise)))
      (is (= :argument.type/support (:meta/argument-type meta-premise))))))
