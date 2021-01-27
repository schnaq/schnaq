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
      (is (associative? (db/delete-discussion sample-discussion))))
    (testing "Deleting a public discussion, should decrease the count."
      (let [new-meeting-id (main-db/add-meeting new-public-meeting)]
        (main-db/add-agenda-point "Some-title" "Some-description" new-meeting-id
                             0 true))
      (is (= (inc discussion-count) (count (main-db/public-meetings))))
      (db/delete-discussion new-discussion-hash)
      (is (= discussion-count (count (main-db/public-meetings)))))))