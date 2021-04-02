(ns schnaq.database.user-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.user :as db]
            [schnaq.meeting.database :refer [fast-pull]]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest add-user-test
  (testing "Check for correct user-addition"
    (is (number? (db/add-user "Gib ihm!")))
    (is (nil? (db/add-user :nono-string)))))

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

(deftest add-user-if-not-exists-test
  (testing "Test the function to add a new user if they do not exist."
    (let [new-user (db/add-user-if-not-exists "For Sure a new User that does Not exist")]
      (is (int? new-user))
      (is (= new-user (db/add-user-if-not-exists "FOR SURE a new User that does Not exist"))))))

(deftest change-user-name
  (testing "Test update user name"
    (let [id "test-id-abcdefg"
          user-name "Tester"
          name-new "New Tester"
          user (db/register-new-user {:id id :preferred_username user-name})
          updated-user (db/update-display-name id name-new)
          current-name (:user.registered/display-name user)
          updated-name (:user.registered/display-name updated-user)]
      (is (not (= current-name updated-name)))
      (is (= user-name current-name))
      (is (= name-new updated-name)))))

(deftest update-groups-test
  (testing "Test, whether the user has correct groups"
    (let [test-user-id "59456d4a-6950-47e8-88d8-a1a6a8de9276"
          group-pattern [:user.registered/keycloak-id
                         :user.registered/groups]
          unmodified-test-user-groups
          (:user.registered/groups (fast-pull [:user.registered/keycloak-id test-user-id] group-pattern))]
      (is (seq unmodified-test-user-groups))
      (is (some #(= "test-group" %) unmodified-test-user-groups))
      (is (not (some #(= "schnaqqifantenparty" %) unmodified-test-user-groups)))
      (is (not (some #(= "new-test-group" %) unmodified-test-user-groups)))
      (let [new-groups ["schnaqqifantenparty" "new-test-group"]
            _ (db/update-groups test-user-id new-groups)
            updated-groups
            (:user.registered/groups (fast-pull [:user.registered/keycloak-id test-user-id] group-pattern))]
        (is (seq updated-groups))
        (is (not (some #(= "test-group" %) updated-groups)))
        (is (some #(= "schnaqqifantenparty" %) updated-groups))
        (is (some #(= "new-test-group" %) updated-groups))))))
