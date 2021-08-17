(ns schnaq.database.visited-statement-tests
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.api.discussion :as discussion-api]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.user :as user-db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(defn add-test-user [id name]
  (second (user-db/register-new-user {:id id :preferred_username name} [] [])))

(defn create-discussion [name share-hash edit-hash user]
  {:discussion/title name
   :discussion/share-hash share-hash
   :discussion/edit-hash edit-hash
   :discussion/author user})

(deftest add-visited-statements-test
  (testing "Test if visited statements are correctly added"
    (let [;; add user
          keycloak-user-id "user-1-id"
          user-name "User Name 1"
          user (add-test-user keycloak-user-id user-name)
          user-id (:db/id user)
          ;; add discussion
          discussion-name "Have you seen this discussion?"
          share-hash "share-hash-1"
          edit-hash "secret-hash-1"
          _ (discussion-db/new-discussion (create-discussion discussion-name share-hash edit-hash user) true)
          ;; add starting statements
          content-1 "Startargument 1"
          content-2 "Startargument 2"
          statement-1 (discussion-db/add-starting-statement! share-hash user-id content-1 true)
          statement-2 (discussion-db/add-starting-statement! share-hash user-id content-2 true)
          statements #{statement-1 statement-2}]
      (testing (str "Add visited statements to user [" user-name "] and discussion [" discussion-name "]")
        (user-db/create-visited-statements-for-discussion keycloak-user-id share-hash statements)
        ;; check if seen statements can be retrieved
        (is (user-db/seen-statements-id keycloak-user-id share-hash))
        ;; check if content is correct
        (let [queried-visited (:seen-statements/visited-statements
                                (fast-pull (user-db/seen-statements-id keycloak-user-id share-hash)
                                           user-db/seen-statements-pattern))]
          (is (= (count statements) (count queried-visited)))
          (is (some #(= statement-1 (:db/id %)) queried-visited))
          (is (some #(= statement-2 (:db/id %)) queried-visited)))))))

(defn find-statement-in-list [statement-id statement-list]
  (some #(when (= statement-id (:db/id %)) %) statement-list))

(deftest update-new-posts-test
  (testing "Test updating posts with seen info"
    (let [;; add user
          keycloak-user-id "user-2-id"
          user-name "User Name 2"
          user (add-test-user keycloak-user-id user-name)
          user-id (:db/id user)
          ;; add discussion
          discussion-name "Have you seen this discussion, as well?"
          share-hash "share-hash-2"
          edit-hash "secret-hash-2"
          _ (discussion-db/new-discussion (create-discussion discussion-name share-hash edit-hash user) true)
          ;; add starting statements
          content-1 "Gesehen"
          content-2 "Auch gesehen"
          content-3 "Ich bin anders!"
          content-new "Noch nicht gesehen"
          statement-1 (discussion-db/add-starting-statement! share-hash user-id content-1 true)
          statement-2 (discussion-db/add-starting-statement! share-hash user-id content-2 true)
          statement-3 (discussion-db/add-starting-statement! share-hash user-id content-3 true)
          statement-new (discussion-db/add-starting-statement! share-hash user-id content-new true)
          seen-statements #{statement-1 statement-2 statement-3}
          ;; pull all statements
          all-statements (mapv #(fast-pull % discussion-db/statement-pattern)
                               [statement-1 statement-2 statement-3 statement-new])
          ;; add seen statements
          _ (user-db/create-visited-statements-for-discussion keycloak-user-id share-hash seen-statements)
          _ (:seen-statements/visited-statements
              (fast-pull (user-db/seen-statements-id keycloak-user-id share-hash)
                         user-db/seen-statements-pattern))
          updated-statements (@#'discussion-api/update-new-posts all-statements share-hash keycloak-user-id)
          updated-statement-1 (find-statement-in-list statement-1 updated-statements)
          updated-statement-2 (find-statement-in-list statement-2 updated-statements)
          updated-statement-3 (find-statement-in-list statement-3 updated-statements)
          updated-statement-new (find-statement-in-list statement-new updated-statements)]
      (is (= (count seen-statements) (count seen-statements)))
      (is (false? (:meta/new updated-statement-1)))
      (is (false? (:meta/new updated-statement-2)))
      (is (false? (:meta/new updated-statement-3)))
      (is (true? (:meta/new updated-statement-new))))))