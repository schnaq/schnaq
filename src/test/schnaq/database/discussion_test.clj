(ns schnaq.database.discussion-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as db]
            [schnaq.meeting.database :as main-db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest delete-discussion-test
  (let [sample-discussion "simple-hash"
        discussion-count (count (main-db/public-meetings))
        new-discussion-hash "ajskdhajksdh"
        new-public-meeting {:meeting/title "Bla"
                            :meeting/start-date (main-db/now)
                            :meeting/end-date (main-db/now)
                            :meeting/share-hash new-discussion-hash
                            :meeting/author (main-db/add-user-if-not-exists "Wegi")}]
    (testing "When deleting wrong discussion, throw error."
      (is (nil? (db/delete-discussion "nonsense-8u89jh89z79h88##")))
      (is (string? (db/delete-discussion sample-discussion))))
    (testing "Deleting a public discussion, should decrease the count."
      (let [new-meeting-id (main-db/add-meeting new-public-meeting)]
        (main-db/add-agenda-point "Some-title" "Some-description" new-meeting-id
                                  0 true))
      (is (= (inc discussion-count) (count (main-db/public-meetings))))
      (db/delete-discussion new-discussion-hash)
      (is (= discussion-count (count (main-db/public-meetings)))))))

(deftest support-statement!-test
  (testing "Add a new supporting statement to a discussion"
    (let [share-hash "simple-hash"
          author-id (main-db/author-id-by-nickname "Wegi")
          starting-conclusion (first (main-db/starting-statements share-hash))
          new-attack (db/support-statement! share-hash author-id (:db/id starting-conclusion)
                                            "This is a new support")]
      (is (= "This is a new support" (-> new-attack :argument/premises first :statement/content)))
      (is (= "Brainstorming ist total wichtig" (-> new-attack :argument/conclusion :statement/content)))
      (is (= :argument.type/support (:argument/type new-attack))))))

(deftest attack-statement!-test
  (testing "Add a new attacking statement to a discussion"
    (let [share-hash "simple-hash"
          author-id (main-db/author-id-by-nickname "Wegi")
          starting-conclusion (first (main-db/starting-statements share-hash))
          new-attack (db/attack-statement! share-hash author-id (:db/id starting-conclusion)
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