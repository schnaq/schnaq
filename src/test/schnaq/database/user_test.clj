(ns schnaq.database.user-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.user :as db]
            [schnaq.test-data :refer [alex kangaroo]]
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

(deftest add-user-if-not-exists-test
  (testing "Test the function to add a new user if they do not exist."
    (let [new-user (db/add-user-if-not-exists "For Sure a new User that does Not exist")]
      (is (int? new-user))
      (is (= new-user (db/add-user-if-not-exists "FOR SURE a new User that does Not exist"))))))

(deftest change-user-name-test
  (testing "Test update user name"
    (let [id "test-id-abcdefg"
          user-name "Tester"
          name-new "New Tester"
          [new-user? user] (db/register-new-user {:sub id :preferred_username "Tester"} [] [])
          updated-user (db/update-display-name id name-new)
          current-name (:user.registered/display-name user)
          updated-name (:user.registered/display-name updated-user)]
      (is (not (= current-name updated-name)))
      (is new-user?)
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
      (let [new-groups ["schnaqqifantenparty" "new-test-group" "test-group"]
            _ (db/update-groups test-user-id new-groups)
            updated-groups
            (:user.registered/groups (fast-pull [:user.registered/keycloak-id test-user-id] group-pattern))]
        (is (seq updated-groups))
        (is (some #(= "test-group" %) updated-groups))
        (is (some #(= "schnaqqifantenparty" %) updated-groups))
        (is (some #(= "new-test-group" %) updated-groups))))))

(deftest members-of-group-test
  (testing "Are the members pulled correctly?"
    (is (empty? (db/members-of-group "some-group-thats-not-there")))
    (is (some #(= "A. Schneider" (:user.registered/display-name %)) (db/members-of-group "test-group")))))

(deftest users-by-notification-interval-test
  (testing "Load users by their notification interval setting."
    (is (= 3 (count (db/users-by-notification-interval :notification-mail-interval/daily))))
    (is (= 1 (count (db/users-by-notification-interval :notification-mail-interval/weekly))))
    (is (= 0 (count (db/users-by-notification-interval :notification-mail-interval/never))))))

(deftest subscribed-share-hashes-test
  (testing "Retrieve share-hashes by users dependent on their notification settings."
    (is (= #{"cat-dog-hash" "simple-hash"}
           (db/subscribed-share-hashes :notification-mail-interval/daily)))
    (is (= #{"cat-dog-hash"}
           (db/subscribed-share-hashes :notification-mail-interval/weekly)))
    (is (= #{} (db/subscribed-share-hashes :notification-mail-interval/never)))))

;; -----------------------------------------------------------------------------
;; Subscriptions

(deftest subscribe-pro-tier-test
  (let [kangaroo-keycloak-id "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        stripe-subscription-id "razupaltuff"
        stripe-customer-id "cus_kangaroo"
        pro-kangaroo (db/subscribe-pro-tier kangaroo-keycloak-id stripe-subscription-id stripe-customer-id)]
    (testing "User subscribes to pro-tier."
      (is (= :user.registered.subscription.type/pro (:user.registered.subscription/type pro-kangaroo)))
      (is (= stripe-subscription-id (:user.registered.subscription/stripe-id pro-kangaroo)))
      (is (= stripe-customer-id (:user.registered.subscription/stripe-customer-id pro-kangaroo))))
    (testing "User unsubscribes from pro tier."
      (let [no-pro-kangaroo (db/unsubscribe-pro-tier kangaroo-keycloak-id)]
        (is (nil? (:user.registered.subscription/type no-pro-kangaroo)))
        (is (nil? (:user.registered.subscription/stripe-id no-pro-kangaroo)))
        (is (nil? (:user.registered.subscription/stripe-customer-id no-pro-kangaroo)))))))

(deftest pro-subscription?-test
  (testing "Check pro-status in database.")
  (let [kangaroo-keycloak-id "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        stripe-subscription-id "razupaltuff"
        stripe-customer-id "cus_kangaroo"
        _ (db/subscribe-pro-tier kangaroo-keycloak-id stripe-subscription-id stripe-customer-id)]
    (is (db/pro-subscription? kangaroo-keycloak-id))))

;; -----------------------------------------------------------------------------
;; Visited schnaqs

(deftest remove-visited-schnaq-test
  (testing "Remove a set of visited schnaqs from a user"
    (let [user-keycloak-id (:user.registered/keycloak-id alex)]
      (db/remove-visited-schnaq user-keycloak-id "cat-dog-hash")
      (is (zero? (count (:user.registered/visited-schnaqs
                         (db/private-user-by-keycloak-id user-keycloak-id))))))))

;; -----------------------------------------------------------------------------
;; Archived schnaqs

(deftest archive-schnaq-test
  (testing "Archive a schnaq for a user."
    (let [keycloak-id (:user.registered/keycloak-id alex)]
      (db/archive-schnaq keycloak-id "cat-dog-hash")
      (is (= 1 (count (:user.registered/archived-schnaqs
                       (db/private-user-by-keycloak-id keycloak-id))))))))

(deftest unarchive-schnaq-test
  (testing "Unarchive a schnaq for a user."
    (let [keycloak-id (:user.registered/keycloak-id kangaroo)]
      (db/unarchive-schnaq keycloak-id "cat-dog-hash")
      (is (zero? (count (:user.registered/archived-schnaqs
                         (db/private-user-by-keycloak-id keycloak-id))))))))
