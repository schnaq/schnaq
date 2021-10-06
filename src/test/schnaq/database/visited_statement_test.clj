(ns schnaq.database.visited-statement-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.user :as user-db]
            [schnaq.processors :as processors]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(defn- add-test-user [id name]
  (second (user-db/register-new-user {:sub id :preferred_username name} [] [])))

(defn- create-discussion [title share-hash edit-hash user]
  {:discussion/title title
   :discussion/share-hash share-hash
   :discussion/edit-hash edit-hash
   :discussion/author user})

(defn- find-statement-in-list [statement-id statement-list]
  (some #(when (= statement-id (:db/id %)) %) statement-list))

(deftest add-visited-statements-test
  (testing "Test if visited statements are correctly added"
    (let [;; add user
          keycloak-user-id "user-1-id"
          user-name "User Name 1"
          user (add-test-user keycloak-user-id user-name)
          user-id (:db/id user)
          ;; add discussion
          discussion-title "Have you seen this discussion?"
          share-hash "share-hash-1"
          edit-hash "secret-hash-1"
          _ (discussion-db/new-discussion (create-discussion discussion-title share-hash edit-hash user))
          ;; add starting statements
          content-1 "Startargument 1"
          content-2 "Startargument 2"
          statement-1 (discussion-db/add-starting-statement! share-hash user-id content-1 true)
          statement-2 (discussion-db/add-starting-statement! share-hash user-id content-2 true)
          statements #{statement-1 statement-2}]
      (testing (str "Add visited statements to user [" user-name "] and discussion [" discussion-title "]")
        (user-db/create-visited-statements-for-discussion keycloak-user-id share-hash statements)
        ;; check if seen statements can be retrieved
        (is (user-db/seen-statements-entity keycloak-user-id share-hash))
        ;; check if content is correct
        (let [queried-visited (:seen-statements/visited-statements
                                (fast-pull (user-db/seen-statements-entity keycloak-user-id share-hash)
                                           patterns/seen-statements))]
          (is (= (count statements) (count queried-visited)))
          (is (some #(= statement-1 (:db/id %)) queried-visited))
          (is (some #(= statement-2 (:db/id %)) queried-visited)))))))

(deftest update-new-posts-test
  (testing "Test updating posts with seen info"
    (let [;; add user
          keycloak-user-id-1 "user-A-id"
          user-name-1 "User Name A"
          user-1 (add-test-user keycloak-user-id-1 user-name-1)
          user-id-1 (:db/id user-1)
          ;; add second user
          keycloak-user-id-2 "user-B-id"
          user-name-2 "User Name B"
          _user-2 (add-test-user keycloak-user-id-2 user-name-2)
          ;; add discussion
          discussion-title "Have you seen this discussion, as well?"
          share-hash "share-hash-2"
          edit-hash "secret-hash-2"
          _ (discussion-db/new-discussion (create-discussion discussion-title share-hash edit-hash user-1))
          ;; add starting statements
          content-1 "Gesehen"
          content-2 "Auch gesehen"
          content-3 "Ich bin auch gesehen worden!"
          content-new "Noch nicht gesehen"
          statement-1 (discussion-db/add-starting-statement! share-hash user-id-1 content-1 true)
          statement-2 (discussion-db/add-starting-statement! share-hash user-id-1 content-2 true)
          statement-3 (discussion-db/add-starting-statement! share-hash user-id-1 content-3 true)
          statement-new (discussion-db/add-starting-statement! share-hash user-id-1 content-new true)
          seen-statements #{statement-1 statement-2 statement-3}
          ;; pull all statements
          all-statements (mapv #(fast-pull % patterns/statement)
                               [statement-1 statement-2 statement-3 statement-new])
          ;; add seen statements for user-2
          _ (user-db/create-visited-statements-for-discussion keycloak-user-id-2 share-hash seen-statements)
          updated-statements (processors/with-new-post-info all-statements share-hash keycloak-user-id-2)
          updated-statement-1 (find-statement-in-list statement-1 updated-statements)
          updated-statement-2 (find-statement-in-list statement-2 updated-statements)
          updated-statement-3 (find-statement-in-list statement-3 updated-statements)
          updated-statement-new (find-statement-in-list statement-new updated-statements)]
      (is (= (count seen-statements) (count seen-statements)))
      (is (false? (:meta/new? updated-statement-1)))
      (is (false? (:meta/new? updated-statement-2)))
      (is (false? (:meta/new? updated-statement-3)))
      (is (true? (:meta/new? updated-statement-new))))))

(defn- add-dead-parrot-sketch
  [user-name]
  (let [;; add user
        keycloak-user-id (str "keycloak-id-" user-name)
        user (add-test-user keycloak-user-id user-name)
        user-id (:db/id user)
        ;; add discussion
        discussion-title "A customer enters a pet shop."
        share-hash "share-hash-3"
        edit-hash "secret-hash-3"
        discussion-id (discussion-db/new-discussion (create-discussion discussion-title share-hash edit-hash user))
        ;; add starting statements
        content-1 "'Ello, I wish to register a complaint. 'Ello, Miss?"
        content-2 "What do you mean miss?"
        content-3 "I'm sorry, I have a cold. I wish to make a complaint!"
        content-new-1 "We're closin' for lunch."
        content-new-2 "Never mind that, my lad. I wish to complain about this parrot what I purchased not half an hour ago from this very boutique."
        statement-1 (discussion-db/add-starting-statement! share-hash user-id content-1 true)
        statement-2 (discussion-db/add-starting-statement! share-hash user-id content-2 true)
        statement-3 (discussion-db/add-starting-statement! share-hash user-id content-3 true)
        statement-4 (discussion-db/add-starting-statement! share-hash user-id content-new-1 true)
        statement-5 (discussion-db/add-starting-statement! share-hash user-id content-new-2 true)
        ;; pull all statements
        all-statements (mapv #(fast-pull % patterns/statement)
                             [statement-1 statement-2 statement-3 statement-4 statement-5])
        ;; add visited schnaqs
        _ (user-db/update-visited-schnaqs keycloak-user-id [discussion-id])]
    {:user user
     :keycloak-id keycloak-user-id
     :discussion-hash share-hash
     :discussion-id discussion-id
     :all-statements all-statements
     :statement-1 statement-1
     :statement-2 statement-2
     :statement-3 statement-3
     :statement-4 statement-4
     :statement-5 statement-5}))

(deftest get-new-statements-tests
  (testing "Test get new statements for user"
    (let [{:keys [discussion-hash all-statements statement-1 statement-2 statement-3 statement-4 statement-5]}
          (add-dead-parrot-sketch "Michael Palin")
          ;; add user
          keycloak-id "user-2-id"
          _user (add-test-user keycloak-id "User Name 2")
          ;; add seen statements
          seen-statements #{statement-1 statement-2 statement-3}
          _ (user-db/create-visited-statements-for-discussion keycloak-id discussion-hash seen-statements)
          new-statements (#'discussion-db/new-statements-for-user keycloak-id discussion-hash)
          expected-new-statement-count (- (count all-statements) (count seen-statements))]
      (is (= expected-new-statement-count (count new-statements)))
      (is (find-statement-in-list statement-4 new-statements))
      (is (find-statement-in-list statement-5 new-statements))
      (is (nil? (find-statement-in-list statement-1 new-statements)))
      (is (nil? (find-statement-in-list statement-2 new-statements)))
      (is (nil? (find-statement-in-list statement-3 new-statements))))))

(deftest test-mark-all-as-read
  (testing "Test if mark-all-as-read causes an empty new-statement-ids-for-user result"
    (let [{:keys [_user _keycloak-id discussion-hash discussion-id all-statements]}
          (add-dead-parrot-sketch "John-Cleese")
          keycloak-id "new-user-keycloak-id"
          _new-user (add-test-user keycloak-id "Neuer Nutzer")
          _ (user-db/update-visited-schnaqs keycloak-id [discussion-id])
          known-before (user-db/known-statement-ids keycloak-id discussion-hash)
          marked-as-read (discussion-db/mark-all-statements-as-read! keycloak-id)
          new-statements-after-mark-as-read (discussion-db/new-statement-ids-for-user
                                              keycloak-id
                                              discussion-hash)]
      (is (empty? known-before))
      (is (= (count all-statements) (count (get marked-as-read discussion-hash)))
          "Number of known statements should be the same as seen-statements from discussion")
      (is (zero? (count new-statements-after-mark-as-read))))))

