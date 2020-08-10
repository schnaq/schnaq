(ns meetly.meeting.database-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [meetly.test.toolbelt :as meetly-toolbelt]
            [dialog.discussion.database :as ddb]
            [meetly.meeting.database :as database]))

(use-fixtures :each meetly-toolbelt/init-db-test-fixture)

(deftest up-and-downvotes-test
  (testing "Tests whether setting up and downvotes works properly."
    (let [cat-or-dog (:db/id (first (ddb/all-discussions-by-title "Cat or Dog?")))
          some-statements (map #(-> % :argument/premises first :db/id)
                               (ddb/all-arguments-for-discussion cat-or-dog))
          author-1 "Test-1"
          author-2 "Test-2"]
      (database/add-user-if-not-exists author-1)
      (database/add-user-if-not-exists author-2)
      (database/upvote-statement! (first some-statements) author-1)
      (database/downvote-statement! (second some-statements) author-1)
      (database/upvote-statement! (first some-statements) author-2)
      (is (database/did-user-upvote-statement (first some-statements) author-1))
      (is (database/did-user-downvote-statement (second some-statements) author-1))
      (is (= 2 (database/upvotes-for-statement (first some-statements))))
      (is (= 1 (database/downvotes-for-statement (second some-statements))))
      (is (= 0 (database/downvotes-for-statement (first some-statements))))
      ;; No up- and downvote for the same statement by the same user!
      (database/downvote-statement! (first some-statements) author-1)
      (is (= 1 (database/upvotes-for-statement (first some-statements))))
      (is (= 1 (database/downvotes-for-statement (first some-statements))))
      ;; Remove the up and downvotes now
      (database/remove-downvote! (first some-statements) author-1)
      (database/remove-upvote! (first some-statements) author-2)
      (is (= 0 (database/upvotes-for-statement (first some-statements))))
      (is (= 0 (database/downvotes-for-statement (first some-statements)))))))

(deftest valid-statement-id-and-meeting?-test
  (testing "Test the function that checks whether a statement belongs to a certain meeting."
    (let [meeting (database/add-meeting {:meeting/title "test-meet"
                                         :meeting/description "whatever"
                                         :meeting/start-date (database/now)
                                         :meeting/end-date (database/now)
                                         :meeting/share-hash "Wegi-ist-der-schönste"
                                         :meeting/author (database/add-user-if-not-exists "Wegi")})
          discussion (database/add-agenda-point "Hi" "Beschreibung" meeting)
          _ (ddb/add-new-starting-argument! discussion "Christian" "this is sparta" ["foo" "bar" "baz"])
          argument (first (ddb/starting-arguments-by-discussion discussion))
          conclusion-id (:db/id (:argument/conclusion argument))
          premise-id (:db/id (first (:argument/premises argument)))]
      (is (database/check-valid-statement-id-and-meeting conclusion-id "Wegi-ist-der-schönste"))
      (is (database/check-valid-statement-id-and-meeting premise-id "Wegi-ist-der-schönste")))))

(deftest clean-db-vals-test
  (testing "Test whether nil values are properly cleaned from a map."
    (let [no-change-map {:foo :bar
                         :baz :bam}
          time-map {:bar (database/now)}]
      (is (= no-change-map (@#'database/clean-db-vals no-change-map)))
      (is (= 2 (count (@#'database/clean-db-vals (merge no-change-map {:unwished-for nil})))))
      (is (= {} (@#'database/clean-db-vals {})))
      (is (= {} (@#'database/clean-db-vals {:foo ""})))
      (is (= time-map (@#'database/clean-db-vals time-map))))))

(deftest add-meeting-test
  (testing "Test whether meetings are properly added"
    (let [minimal-meeting {:meeting/title "Bla"
                           :meeting/start-date (database/now)
                           :meeting/end-date (database/now)
                           :meeting/share-hash "aklsuzd98-234da-123d"
                           :meeting/author (database/add-user-if-not-exists "Wegi")}]
      (is (number? (database/add-meeting minimal-meeting)))
      (is (number? (database/add-meeting (assoc minimal-meeting :meeting/description "some description"))))
      (is (nil? (database/add-meeting (assoc minimal-meeting :meeting/description 123)))))))

(deftest add-agenda-point-test
  (testing "Check whether agendas are added correctly"
    (let [some-meeting (database/add-meeting {:meeting/title "Bla"
                                              :meeting/start-date (database/now)
                                              :meeting/end-date (database/now)
                                              :meeting/share-hash "aklsuzd98-234da-123d"
                                              :meeting/author (database/add-user-if-not-exists "Wegi")})]
      (is (number? (database/add-agenda-point "Alles gut" "hier" some-meeting)))
      (is (nil? (database/add-agenda-point 123 nil some-meeting)))
      (is (nil? (database/add-agenda-point "Meeting-kaputt" nil "was ist das?")))
      (is (number? (database/add-agenda-point "Kaputte description wird ignoriert" 123 some-meeting))))))

(deftest add-user-test
  (testing "Check for correct user-addition"
    (is (number? (database/add-user "Gib ihm!")))
    (is (nil? (database/add-user :nono-string)))))

(deftest add-feedback-test
  (testing "Valid feedbacks should be stored."
    (let [feedback {:feedback/description "Very good stuff 👍 Would use again"
                    :feedback/contact-mail "christian@dialogo.io"
                    :feedback/has-image? false}]
      (is (zero? (count (database/all-feedbacks))))
      (is (number? (database/add-feedback! feedback)))
      (is (= 1 (count (database/all-feedbacks)))))))

(deftest add-user-if-not-exists-test
  (testing "Test the function to add a new user if they do not exist."
    (let [new-user (database/add-user-if-not-exists "For Sure a new User that does Not exist")]
      (is (int? new-user))
      (is (= new-user (database/add-user-if-not-exists "FOR SURE a new User that does Not exist"))))))

(deftest user-by-nickname-test
  (testing "Tests whether the user is correctly found, disregarding case."
    (let [wegi (database/user-by-nickname "Wegi")]
      (is (int? wegi))
      (is (= wegi (database/user-by-nickname "WeGi")
             (database/user-by-nickname "wegi")
             (database/user-by-nickname "wegI"))))))

(deftest canonical-username-test
  (testing "Test whether the canonical username is returned."
    (is (= "Wegi" (database/canonical-username "WEGI")
           (database/canonical-username "WeGi")))
    (is (= "Der Schredder" (database/canonical-username "DER schredder")))))

(deftest add-user-if-not-exists-test
  (testing "Test the function to add a new user if they do not exist."
    (let [new-user (database/add-user-if-not-exists "For Sure a new User that does Not exist")]
      (is (int? new-user))
      (is (= new-user (database/add-user-if-not-exists "FOR SURE a new User that does Not exist"))))))

(deftest user-by-nickname-test
  (testing "Tests whether the user is correctly found, disregarding case."
    (let [wegi (database/user-by-nickname "Wegi")]
      (is (int? wegi))
      (is (= wegi (database/user-by-nickname "WeGi")
             (database/user-by-nickname "wegi")
             (database/user-by-nickname "wegI"))))))

(deftest canonical-username-test
  (testing "Test whether the canonical username is returned."
    (is (= "Wegi" (database/canonical-username "WEGI")
           (database/canonical-username "WeGi")))
    (is (= "Der Schredder" (database/canonical-username "DER schredder")))))


;; Tests for the analytics part

(deftest number-of-meetings-test
  (testing "Return the correct number of meetings"
    (is number? (database/number-of-meetings))
    (is (zero? (database/number-of-meetings (database/now))))))