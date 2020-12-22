(ns schnaq.meeting.database-test
  (:require [clojure.test :refer [deftest testing use-fixtures is are]]
            [schnaq.meeting.database :as db]
            [schnaq.test.toolbelt :as schnaq-toolbelt])
  (:import (java.time Instant)))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(defn- any-meeting-id
  []
  (db/add-meeting {:meeting/title "Bla"
                   :meeting/start-date (db/now)
                   :meeting/end-date (db/now)
                   :meeting/share-hash "aklsuzd98-234da-123d"
                   :meeting/author (db/add-user-if-not-exists "Wegi")}))

(deftest up-and-downvotes-test
  (testing "Tests whether setting up and downvotes works properly."
    (let [cat-or-dog (:db/id (first (db/all-discussions-by-title "Cat or Dog?")))
          some-statements (map #(-> % :argument/premises first :db/id)
                               (db/all-arguments-for-discussion cat-or-dog))
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

(deftest valid-statement-id-and-meeting?-test
  (testing "Test the function that checks whether a statement belongs to a certain meeting."
    (let [meeting (db/add-meeting {:meeting/title "test-meet"
                                   :meeting/description "whatever"
                                   :meeting/start-date (db/now)
                                   :meeting/end-date (db/now)
                                   :meeting/share-hash "Wegi-ist-der-schönste"
                                   :meeting/author (db/add-user-if-not-exists "Wegi")})
          agenda (db/add-agenda-point "Hi" "Beschreibung" meeting)
          discussion (:db/id (:agenda/discussion (db/fast-pull agenda)))
          christian-id (db/author-id-by-nickname "Christian")
          first-id (db/add-starting-statement! discussion christian-id "this is sparta")
          second-id (db/add-starting-statement! discussion christian-id "this is kreta")]
      (is (db/check-valid-statement-id-and-meeting first-id "Wegi-ist-der-schönste"))
      (is (db/check-valid-statement-id-and-meeting second-id "Wegi-ist-der-schönste")))))

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

(deftest add-meeting-test
  (testing "Test whether meetings are properly added"
    (let [minimal-meeting {:meeting/title "Bla"
                           :meeting/start-date (db/now)
                           :meeting/end-date (db/now)
                           :meeting/share-hash "aklsuzd98-234da-123d"
                           :meeting/author (db/add-user-if-not-exists "Wegi")}]
      (is (number? (db/add-meeting minimal-meeting)))
      (is (number? (db/add-meeting (assoc minimal-meeting :meeting/description "some description"))))
      (is (nil? (db/add-meeting (assoc minimal-meeting :meeting/description 123)))))))

(deftest add-agenda-point-test
  (testing "Check whether agendas are added correctly"
    (let [some-meeting (any-meeting-id)]
      (is (number? (db/add-agenda-point "Alles gut" "hier" some-meeting)))
      (is (nil? (db/add-agenda-point 123 nil some-meeting)))
      (is (nil? (db/add-agenda-point "Meeting-kaputt" nil "was ist das?")))
      (is (number? (db/add-agenda-point "Kaputte description wird ignoriert" 123 some-meeting))))))

(deftest add-user-test
  (testing "Check for correct user-addition"
    (is (number? (db/add-user "Gib ihm!")))
    (is (nil? (db/add-user :nono-string)))))

(deftest add-feedback-test
  (testing "Valid feedbacks should be stored."
    (let [feedback {:feedback/description "Very good stuff 👍 Would use again"
                    :feedback/contact-mail "christian@dialogo.io"
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

(deftest number-of-meetings-test
  (testing "Return the correct number of meetings"
    (is (= 3 (db/number-of-meetings)))
    (any-meeting-id)                                        ;; Adds any new meeting
    (is (= 4 (db/number-of-meetings)))
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

(deftest average-number-of-agendas-test
  (testing "Test whether the average number of agendas fits."
    (is (= 4/3 (db/average-number-of-agendas)))
    (any-meeting-id)
    (is (= 1 (db/average-number-of-agendas)))))

(deftest number-of-active-users-test
  (testing "Test whether the active users are returned correctly."
    (let [cat-or-dog-id (:db/id (first (db/all-discussions-by-title "Cat or Dog?")))]
      (is (= 4 (db/number-of-active-discussion-users)))
      (let [_ (db/add-user-if-not-exists "wooooggler")
            woggler-id (db/author-id-by-nickname "wooooggler")]
        (is (= 4 (db/number-of-active-discussion-users)))
        (@#'db/transact
          [(@#'db/prepare-new-argument cat-or-dog-id woggler-id "Alles doof" ["weil alles doof war"])]))
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

(deftest all-statements-for-graph-test
  (testing "Returns all statements belonging to a agenda, specially prepared for graph-building."
    (let [discussion-id (:db/id (first (db/all-discussions-by-title "Wetter Graph")))
          statements (db/all-statements-for-graph discussion-id)]
      (is (= 7 (count statements)))
      (is (= 1 (count (filter #(= "foo" (:label %)) statements)))))))

(deftest number-of-statements-for-discussion-test
  (testing "Is the number of agendas returned correct?"
    (let [meeting-hash "89eh32hoas-2983ud"
          meeting-hash-2 "graph-hash"
          cat-or-dog-discussion (:agenda/discussion
                                  (first (remove #(= (:agenda/title %) "Top 2")
                                                 (db/agendas-by-meeting-hash meeting-hash))))
          graph-discussion (:agenda/discussion (first (db/agendas-by-meeting-hash meeting-hash-2)))]
      (is (= 27 (db/number-of-statements-for-discussion (:db/id cat-or-dog-discussion))))
      (is (= 7 (db/number-of-statements-for-discussion (:db/id graph-discussion)))))))

(deftest pack-premises-test
  (testing "Test the creation of statement-entities from strings"
    (let [premises ["What a beautifull day" "Hello test"]
          author-id (db/author-id-by-nickname "Test-person")
          premise-entities (@#'db/pack-premises premises author-id)]
      (is (= [{:db/id "premise-What a beautifull day",
               :statement/author author-id,
               :statement/content (first premises),
               :statement/version 1}
              {:db/id "premise-Hello test",
               :statement/author author-id,
               :statement/content (second premises),
               :statement/version 1}]
             premise-entities)))))

(deftest prepare-new-argument-test
  (testing "Test the creation of a valid argument-entity from strings"
    (let [premises ["What a beautifull day" "Hello test"]
          conclusion "Wow look at this"
          author-id (db/author-id-by-nickname "Test-person")
          meeting-hash "graph-hash"
          discussion-id
          (->> meeting-hash
               db/agendas-by-meeting-hash
               first
               :agenda/discussion :db/id)
          with-id (@#'db/prepare-new-argument discussion-id author-id conclusion premises "temp-id-here")]
      (is (contains? with-id :argument/premises))
      (is (contains? with-id :argument/conclusion))
      (is (contains? with-id :argument/author))
      (is (contains? with-id :argument/version))
      (is (contains? with-id :argument/type))
      (is (contains? with-id :argument/discussions)))))

(deftest add-starting-statement!-test
  (testing "Test the creation of a valid argument-entity from strings"
    (let [statement "Wow look at this"
          author-id (db/author-id-by-nickname "Test-person")
          meeting-hash "graph-hash"
          graph-discussion (:agenda/discussion (first (db/agendas-by-meeting-hash meeting-hash)))
          _ (db/add-starting-statement! (:db/id graph-discussion) author-id statement)
          starting-statements (db/starting-statements (:db/id graph-discussion))]
      (testing "Must have three more statements than the vanilla set and one more starting conclusion"
        (is (= 8 (db/number-of-statements-for-discussion (:db/id graph-discussion))))
        (is (= 3 (count starting-statements)))))))

(deftest all-arguments-for-conclusion-test
  (testing "Get arguments, that have a certain conclusion"
    (let [simple-discussion (:agenda/discussion (first (db/agendas-by-meeting-hash "simple-hash")))
          starting-conclusion (first (db/starting-statements (:db/id simple-discussion)))
          simple-argument (first (db/all-arguments-for-conclusion (:db/id starting-conclusion)))]
      (is (= "Man denkt viel nach dabei" (-> simple-argument :argument/premises first :statement/content)))
      (is (= "Brainstorming ist total wichtig" (-> simple-argument :argument/conclusion :statement/content))))))

(deftest statements-undercutting-premise-test
  (testing "Get arguments, that are undercutting an argument with a certain premise"
    (let [simple-discussion (:agenda/discussion (first (db/agendas-by-meeting-hash "simple-hash")))
          starting-conclusion (first (db/starting-statements (:db/id simple-discussion)))
          simple-argument (first (db/all-arguments-for-conclusion (:db/id starting-conclusion)))
          premise-to-undercut-id (-> simple-argument :argument/premises first :db/id)
          desired-statement (first (db/statements-undercutting-premise premise-to-undercut-id))]
      (is (= "Brainstorm hat nichts mit aktiv denken zu tun" (:statement/content desired-statement))))))

(deftest attack-statement!-test
  (testing "Add a new attacking statement to a discussion"
    (let [simple-discussion (:agenda/discussion (first (db/agendas-by-meeting-hash "simple-hash")))
          author-id (db/author-id-by-nickname "Wegi")
          starting-conclusion (first (db/starting-statements (:db/id simple-discussion)))
          new-attack (db/attack-statement! (:db/id simple-discussion) author-id (:db/id starting-conclusion)
                                           "This is a new attack")]
      (is (= "This is a new attack" (-> new-attack :argument/premises first :statement/content)))
      (is (= "Brainstorming ist total wichtig" (-> new-attack :argument/conclusion :statement/content)))
      (is (= :argument.type/attack (:argument/type new-attack))))))

(deftest support-statement!-test
  (testing "Add a new supporting statement to a discussion"
    (let [simple-discussion (:agenda/discussion (first (db/agendas-by-meeting-hash "simple-hash")))
          author-id (db/author-id-by-nickname "Wegi")
          starting-conclusion (first (db/starting-statements (:db/id simple-discussion)))
          new-attack (db/support-statement! (:db/id simple-discussion) author-id (:db/id starting-conclusion)
                                            "This is a new support")]
      (is (= "This is a new support" (-> new-attack :argument/premises first :statement/content)))
      (is (= "Brainstorming ist total wichtig" (-> new-attack :argument/conclusion :statement/content)))
      (is (= :argument.type/support (:argument/type new-attack))))))

(deftest all-discussions-by-title-test
  (testing "Should return discussions if title matches at least one discussion."
    (is (empty? (db/all-discussions-by-title "")))
    (is (empty? (db/all-discussions-by-title "👾")))
    (is (seq (db/all-discussions-by-title "Cat or Dog?")))))

(deftest all-arguments-for-discussion-test
  (testing "Should return valid arguments for valid discussion."
    (let [cat-or-dog-id (:db/id (first (db/all-discussions-by-title "Cat or Dog?")))]
      (is (empty? (db/all-arguments-for-discussion -1)))
      (is (seq (db/all-arguments-for-discussion cat-or-dog-id)))
      (is (contains? #{:argument.type/undercut :argument.type/support :argument.type/attack}
                     (:argument/type (rand-nth (db/all-arguments-for-discussion cat-or-dog-id))))))))

(deftest statements-by-content-test
  (testing "Statements are identified by identical content."
    (is (= 1 (count (db/statements-by-content "dogs can act as watchdogs"))))
    (is (= 1 (count (db/statements-by-content "we have no use for a watchdog"))))
    (is (empty? (db/statements-by-content "foo-baar-ajshdjkahsjdkljsadklja")))))

(deftest starting-statements-test
  (testing "Should return all starting-statements from a discussion."
    (let [cat-or-dog-id (:db/id (first (db/all-discussions-by-title "Cat or Dog?")))
          simple-discussion (:db/id (first (db/all-discussions-by-title "Simple Discussion")))
          graph-discussion (:db/id (first (db/all-discussions-by-title "Wetter Graph")))]
      (are [result discussion] (= result (count (db/starting-statements discussion)))
                               3 cat-or-dog-id
                               1 simple-discussion
                               2 graph-discussion))))
