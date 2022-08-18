(ns schnaq.database.visited-statement-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.user :as user-db]
            [schnaq.processors :as processors]
            [schnaq.test-data :refer [kangaroo christian]]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(def ^:private kangaroo-keycloak-id
  (:user.registered/keycloak-id kangaroo))

(def ^:private christian-keycloak-id
  (:user.registered/keycloak-id christian))

(defn- create-discussion [title share-hash edit-hash user-id]
  {:discussion/title title
   :discussion/share-hash share-hash
   :discussion/edit-hash edit-hash
   :discussion/author user-id})

(defn- find-statement-in-list [statement-id statement-list]
  (some #(when (= statement-id (:db/id %)) %) statement-list))

(deftest add-visited-statements-test
  (testing "Test if visited statements are correctly added"
    (let [keycloak-id kangaroo-keycloak-id
          user-id (:db/id (user-db/private-user-by-keycloak-id kangaroo-keycloak-id))
          share-hash "cat-dog-hash"
          ;; add starting statements
          content-1 "Startargument 1"
          content-2 "Startargument 2"
          statement-1 (:db/id (discussion-db/add-starting-statement! share-hash user-id content-1
                                                                     :registered-user? true))
          statement-2 (:db/id (discussion-db/add-starting-statement! share-hash user-id content-2
                                                                     :registered-user? true))
          statements #{statement-1 statement-2}]
      (user-db/create-visited-statements-for-discussion keycloak-id share-hash statements)
        ;; check if seen statements can be retrieved
      (is (user-db/seen-statements-entity keycloak-id share-hash))
        ;; check if content is correct
      (let [queried-visited (:seen-statements/visited-statements
                             (fast-pull (user-db/seen-statements-entity keycloak-id share-hash)
                                        patterns/seen-statements))]
        (is (= (count statements) (count queried-visited)))
        (is (some #(= statement-1 (:db/id %)) queried-visited))
        (is (some #(= statement-2 (:db/id %)) queried-visited))))))

(deftest update-new-posts-test
  (testing "Test updating posts with seen info"
    (let [user-id (-> kangaroo-keycloak-id
                      user-db/private-user-by-keycloak-id
                      :db/id)
          share-hash "cat-dog-hash"
          ;; add starting statements
          content-1 "Gesehen"
          content-2 "Auch gesehen"
          content-3 "Ich bin auch gesehen worden!"
          content-new "Noch nicht gesehen"
          statement-1 (:db/id (discussion-db/add-starting-statement! share-hash user-id content-1
                                                                     :registered-user? true))
          statement-2 (:db/id (discussion-db/add-starting-statement! share-hash user-id content-2
                                                                     :registered-user? true))
          statement-3 (:db/id (discussion-db/add-starting-statement! share-hash user-id content-3
                                                                     :registered-user? true))
          statement-new (:db/id (discussion-db/add-starting-statement! share-hash user-id content-new
                                                                       :registered-user? true))
          seen-statements #{statement-1 statement-2 statement-3}
          ;; pull all statements
          all-statements (mapv #(fast-pull % patterns/statement)
                               [statement-1 statement-2 statement-3 statement-new])
          ;; add seen statements for user-2
          _ (user-db/create-visited-statements-for-discussion christian-keycloak-id share-hash seen-statements)
          updated-statements (processors/with-new-post-info all-statements share-hash christian-keycloak-id)
          updated-statement-1 (find-statement-in-list statement-1 updated-statements)
          updated-statement-2 (find-statement-in-list statement-2 updated-statements)
          updated-statement-3 (find-statement-in-list statement-3 updated-statements)
          updated-statement-new (find-statement-in-list statement-new updated-statements)]
      (is (= (count seen-statements) (count seen-statements)))
      (is (false? (:meta/new? updated-statement-1)))
      (is (false? (:meta/new? updated-statement-2)))
      (is (false? (:meta/new? updated-statement-3)))
      (is (true? (:meta/new? updated-statement-new))))))

(defn- add-dead-parrot-sketch!
  []
  (let [user (user-db/private-user-by-keycloak-id kangaroo-keycloak-id)
        user-id (:db/id user)
        ;; add discussion
        discussion-title "Have you seen this discussion?"
        share-hash "share-hash-1"
        edit-hash "secret-hash-1"
        discussion-id (discussion-db/new-discussion (create-discussion discussion-title share-hash edit-hash user-id))
        ;; add starting statements
        content-1 "'Ello, I wish to register a complaint. 'Ello, Miss?"
        content-2 "What do you mean miss?"
        content-3 "I'm sorry, I have a cold. I wish to make a complaint!"
        content-new-1 "We're closin' for lunch."
        content-new-2 "Never mind that, my lad. I wish to complain about this parrot what I purchased not half an hour ago from this very boutique."
        statement-1 (:db/id (discussion-db/add-starting-statement! share-hash user-id content-1
                                                                   :registered-user? true))
        statement-2 (:db/id (discussion-db/add-starting-statement! share-hash user-id content-2
                                                                   :registered-user? true))
        statement-3 (:db/id (discussion-db/add-starting-statement! share-hash user-id content-3
                                                                   :registered-user? true))
        statement-4 (:db/id (discussion-db/add-starting-statement! share-hash user-id content-new-1
                                                                   :registered-user? true))
        statement-5 (:db/id (discussion-db/add-starting-statement! share-hash user-id content-new-2
                                                                   :registered-user? true))
        ;; pull all statements
        all-statements (mapv #(fast-pull % patterns/statement)
                             [statement-1 statement-2 statement-3 statement-4 statement-5])
        ;; add visited schnaqs
        _ (user-db/update-visited-schnaqs user [discussion-id])]
    {:discussion-hash share-hash
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
          (add-dead-parrot-sketch!)
          ;; add seen statements
          seen-statements #{statement-1 statement-2 statement-3}
          _ (user-db/create-visited-statements-for-discussion christian-keycloak-id discussion-hash seen-statements)
          new-statements (#'discussion-db/new-statements-for-user christian-keycloak-id discussion-hash)
          expected-new-statement-count (- (count all-statements) (count seen-statements))]
      (is (= expected-new-statement-count (count new-statements)))
      (is (find-statement-in-list statement-4 new-statements))
      (is (find-statement-in-list statement-5 new-statements))
      (is (nil? (find-statement-in-list statement-1 new-statements)))
      (is (nil? (find-statement-in-list statement-2 new-statements)))
      (is (nil? (find-statement-in-list statement-3 new-statements))))))

(deftest mark-all-as-read-test
  (testing "Test if mark-all-as-read causes an empty new-statement-ids-for-user result"
    (let [{:keys [_user _keycloak-id discussion-hash discussion-id all-statements]}
          (add-dead-parrot-sketch!)
          keycloak-id christian-keycloak-id
          user (user-db/private-user-by-keycloak-id keycloak-id)
          _ (user-db/update-visited-schnaqs user [discussion-id])
          known-before (user-db/known-statement-ids keycloak-id discussion-hash)
          marked-as-read (discussion-db/mark-all-statements-as-read! keycloak-id)
          new-statements-after-mark-as-read (discussion-db/new-statement-ids-for-user
                                             keycloak-id
                                             discussion-hash)]
      (is (empty? known-before))
      (is (= (count all-statements) (count (get marked-as-read discussion-hash)))
          "Number of known statements should be the same as seen-statements from discussion")
      (is (zero? (count new-statements-after-mark-as-read))))))
