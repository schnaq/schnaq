(ns schnaq.meeting.database-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.meeting.database :as db]
            [schnaq.test.toolbelt :as schnaq-toolbelt])
  (:import (java.time Instant)))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

#_(def ^:private any-meeting-share-hash "aklsuzd98-234da-123d")
;; temporarily disable during refactor
#_(defn- any-discussion
  []
  (discussion-db/new-discussion {:discussion/title "Bla"
                                 :discussion/share-hash any-meeting-share-hash
                                 :discussion/edit-hash (str any-meeting-share-hash "-secret")
                                 :discussion/author (db/add-user-if-not-exists "Wegi")}
                                true))

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


;; Tests for the analytics part
;; todo temporarily disable during refactor
#_(deftest number-of-meetings-test
  (testing "Return the correct number of meetings"
    (is (= 5 (db/number-of-meetings)))
    ;; Adds any new discussion
    (any-discussion)
    (is (= 6 (db/number-of-meetings)))
    (is (zero? (db/number-of-meetings (Instant/now))))))

(deftest number-of-usernames-test
  (testing "Return the correct number of usernames"
    ;; There are at least the 4 users from the test-set
    (is (= 6 (db/number-of-usernames)))
    (db/add-user-if-not-exists "Some-Testdude")
    (is (= 7 (db/number-of-usernames)))
    (is (zero? (db/number-of-meetings (Instant/now))))))

(deftest number-of-statements-test
  (testing "Return the correct number of statements."
    (is (= 38 (db/number-of-statements)))
    (is (zero? (db/number-of-statements (Instant/now))))))

;; todo temporarily disable during refactor
#_(deftest average-number-of-agendas-test
  (testing "Test whether the average number of agendas fits."
    (is (= 6/5 (db/average-number-of-agendas)))
    (any-discussion)
    (is (= 1 (db/average-number-of-agendas)))))

(deftest number-of-active-users-test
  (testing "Test whether the active users are returned correctly."
    (let [cat-or-dog-id (:db/id (first (discussion-db/all-discussions-by-title "Cat or Dog?")))]
      (is (= 4 (db/number-of-active-discussion-users)))
      (let [_ (db/add-user-if-not-exists "wooooggler")
            woggler-id (db/user-by-nickname "wooooggler")]
        (is (= 4 (db/number-of-active-discussion-users)))
        (@#'db/transact
          [(discussion-db/prepare-new-argument cat-or-dog-id woggler-id "Alles doof"
                                               ["weil alles doof war"])]))
      (is (= 5 (db/number-of-active-discussion-users))))))

(deftest statement-length-stats-test
  (testing "Testing the function that returns lengths of statements statistics"
    (let [stats (db/statement-length-stats)]
      (is (< (:min stats) (:max stats)))
      (is (< (:min stats) (:median stats)))
      (is (> (:max stats) (:median stats)))
      (is (> (:max stats) (:average stats)))
      (is float? (:average stats)))))

(deftest argument-type-stats-test
  (testing "Statistics about argument types should be working."
    (let [stats (db/argument-type-stats)]
      (is (= 7 (:attacks stats)))
      (is (= 15 (:supports stats)))
      (is (= 9 (:undercuts stats))))))

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
