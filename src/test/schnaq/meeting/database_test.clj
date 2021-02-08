(ns schnaq.meeting.database-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.meeting.database :as db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest up-and-downvotes-test
  (testing "Tests whether setting up and downvotes works properly."
    (let [share-hash "cat-dog-hash"
          some-statements (map #(-> % :argument/premises first :db/id)
                               (discussion-db/all-arguments-for-discussion share-hash))
          author-1 "Test-1"
          author-2 "Test-2"]
      (db/add-user-if-not-exists author-1)
      (db/add-user-if-not-exists author-2)
      (db/upvote-statement! (first some-statements) author-1)
      (db/downvote-statement! (second some-statements) author-1)
      (db/upvote-statement! (first some-statements) author-2)
      (is (db/did-user-upvote-statement (first some-statements) author-1))
      (is (db/did-user-downvote-statement (second some-statements) author-1))
      (is (= 2 (db/upvotes-for-statement (first some-statements))))
      (is (= 1 (db/downvotes-for-statement (second some-statements))))
      (is (zero? (db/downvotes-for-statement (first some-statements))))
      ;; No up- and downvote for the same statement by the same user!
      (db/downvote-statement! (first some-statements) author-1)
      (is (= 1 (db/upvotes-for-statement (first some-statements))))
      (is (= 1 (db/downvotes-for-statement (first some-statements))))
      ;; Remove the up and downvotes now
      (db/remove-downvote! (first some-statements) author-1)
      (db/remove-upvote! (first some-statements) author-2)
      (is (zero? (db/upvotes-for-statement (first some-statements))))
      (is (zero? (db/downvotes-for-statement (first some-statements)))))))

(deftest clean-db-vals-test
  (testing "Test whether nil values are properly cleaned from a map."
    (let [no-change-map {:foo :bar
                         :baz :bam}
          time-map {:bar (db/now)}]
      (is (= no-change-map (@#'db/clean-db-vals no-change-map)))
      (is (= 2 (count (@#'db/clean-db-vals (merge no-change-map {:unwished-for nil})))))
      (is (= {} (@#'db/clean-db-vals {})))
      (is (= {} (@#'db/clean-db-vals {:foo ""})))
      (is (= time-map (@#'db/clean-db-vals time-map))))))

(deftest add-user-test
  (testing "Check for correct user-addition"
    (is (number? (db/add-user "Gib ihm!")))
    (is (nil? (db/add-user :nono-string)))))

(deftest add-feedback-test
  (testing "Valid feedbacks should be stored."
    (let [feedback {:feedback/description "Very good stuff 👍 Would use again"
                    :feedback/contact-mail "christian@schnaq.com"
                    :feedback/has-image? false}]
      (is (zero? (count (db/all-feedbacks))))
      (is (number? (db/add-feedback! feedback)))
      (is (= 1 (count (db/all-feedbacks)))))))

(deftest add-user-if-not-exists-test
  (testing "Test the function to add a new user if they do not exist."
    (let [new-user (db/add-user-if-not-exists "For Sure a new User that does Not exist")]
      (is (int? new-user))
      (is (= new-user (db/add-user-if-not-exists "FOR SURE a new User that does Not exist"))))))

(deftest user-by-nickname-test
  (testing "Tests whether the user is correctly found, disregarding case."
    (let [wegi (db/user-by-nickname "Wegi")]
      (is (int? wegi))
      (is (= wegi (db/user-by-nickname "WeGi")
             (db/user-by-nickname "wegi")
             (db/user-by-nickname "wegI"))))))

(deftest canonical-username-test
  (testing "Test whether the canonical username is returned."
    (is (= "Wegi" (db/canonical-username "WEGI")
           (db/canonical-username "WeGi")))
    (is (= "Der Schredder" (db/canonical-username "DER schredder")))))

(deftest all-arguments-for-conclusion-test
  (testing "Get arguments, that have a certain conclusion"
    (let [share-hash "simple-hash"
          starting-conclusion (first (discussion-db/starting-statements share-hash))
          simple-argument (first (discussion-db/all-arguments-for-conclusion (:db/id starting-conclusion)))]
      (is (= "Man denkt viel nach dabei" (-> simple-argument :argument/premises first :statement/content)))
      (is (= "Brainstorming ist total wichtig" (-> simple-argument :argument/conclusion :statement/content))))))

(deftest all-discussions-by-title-test
  (testing "Should return discussions if title matches at least one discussion."
    (is (empty? (discussion-db/all-discussions-by-title "")))
    (is (empty? (discussion-db/all-discussions-by-title "👾")))
    (is (seq (discussion-db/all-discussions-by-title "Cat or Dog?")))))
