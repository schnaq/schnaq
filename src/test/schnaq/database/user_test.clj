(ns schnaq.database.user-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as db]
            [schnaq.test-data :refer [alex christian kangaroo]]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(def ^:private kangaroo-keycloak-id (:user.registered/keycloak-id kangaroo))
(def ^:private alex-keycloak-id (:user.registered/keycloak-id alex))

(deftest add-user-test
  (testing "Check for correct user-addition"
    (is (s/valid? ::specs/any-user (db/add-user "Gib ihm!")))))

(deftest user-by-nickname-test
  (testing "Tests whether the user is correctly found, disregarding case."
    (let [wegi (db/user-by-nickname "Wegi")]
      (is (= wegi (db/user-by-nickname "WeGi")
             (db/user-by-nickname "wegi")
             (db/user-by-nickname "wegI"))))))

(deftest add-user-if-not-exists-test
  (testing "Test the function to add a new user if they do not exist."
    (let [new-user (db/add-user-if-not-exists "For Sure a new User that does Not exist")]
      (is (= new-user (db/add-user-if-not-exists "FOR SURE a new User that does Not exist"))))))

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
        stripe-subscription-id "sub_razupaltuff"
        stripe-customer-id "cus_kangaroo"
        pro-kangaroo (db/subscribe-pro-tier kangaroo-keycloak-id stripe-subscription-id stripe-customer-id)]
    (testing "User subscribes to pro-tier."
      (is (contains? (:user.registered/roles pro-kangaroo) :role/pro))
      (is (= stripe-subscription-id (:user.registered.subscription/stripe-id pro-kangaroo)))
      (is (= stripe-customer-id (:user.registered.subscription/stripe-customer-id pro-kangaroo))))
    (testing "User unsubscribes from pro tier."
      (let [no-pro-kangaroo (db/unsubscribe-pro-tier kangaroo-keycloak-id)]
        (is (nil? (:user.registered.subscription/stripe-id no-pro-kangaroo)))
        (is (nil? (:user.registered.subscription/stripe-customer-id no-pro-kangaroo)))))))

;; -----------------------------------------------------------------------------
;; Visited schnaqs

(deftest remove-visited-schnaq-test
  (testing "Remove a set of visited schnaqs from a user"
    (db/remove-visited-schnaq alex-keycloak-id "cat-dog-hash")
    (is (zero? (count (:user.registered/visited-schnaqs
                       (db/private-user-by-keycloak-id alex-keycloak-id)))))))

;; -----------------------------------------------------------------------------
;; Archived schnaqs

(deftest archive-schnaq-test
  (testing "Archive a schnaq for a user."
    (db/archive-schnaq alex-keycloak-id "cat-dog-hash")
    (is (= 1 (count (:user.registered/archived-schnaqs
                     (db/private-user-by-keycloak-id alex-keycloak-id)))))))

(deftest unarchive-schnaq-test
  (testing "Unarchive a schnaq for a user."
    (db/unarchive-schnaq kangaroo-keycloak-id "cat-dog-hash")
    (is (zero? (count (:user.registered/archived-schnaqs
                       (db/private-user-by-keycloak-id kangaroo-keycloak-id)))))))

;; -----------------------------------------------------------------------------

(deftest update-user-display-name-test
  (testing "Update an existing user's display name."
    (let [new-name "not-kangaroo"]
      (is (= new-name
             (:user.registered/display-name
              (db/update-user {:user.registered/keycloak-id kangaroo-keycloak-id
                               :user.registered/display-name new-name})))))))

(deftest update-user-multiple-fields-test
  (testing "Update an existing user'"
    (let [new-name "not-kangaroo"
          new-mail "foo@bar.com"
          {:user.registered/keys [display-name email]}
          (db/update-user {:user.registered/keycloak-id kangaroo-keycloak-id
                           :user.registered/display-name new-name
                           :user.registered/email new-mail})]
      (is (= new-mail email))
      (is (= new-name display-name)))))

(deftest private-user-by-keycloak-id-test
  (testing "Valid users are returned can be queried by their keycloak-id."
    (is (nil? (db/private-user-by-keycloak-id "foo")))
    (is (s/valid? ::specs/registered-user (db/private-user-by-keycloak-id kangaroo-keycloak-id)))))

;; -----------------------------------------------------------------------------

(deftest created-discussions-test
  (testing "Count number of created discussions per user."
    (is (= 0 (db/created-discussions kangaroo-keycloak-id)))
    (is (= 1 (db/created-discussions alex-keycloak-id)))
    (is (= 0 (db/created-discussions "razupaltuff")))))

;; -----------------------------------------------------------------------------

(deftest retract-user-attribute-test
  (testing "Removing an attribute results in a nil lookup."
    (is (nil? (:user.registered/email (db/retract-user-attribute {:user.registered/keycloak-id kangaroo-keycloak-id} :user.registered/email))))))

(deftest retract-user-attributes-value-test
  (testing "Retract a value from a set, returns the set without the value."
    (let [tester-admin-christian (db/update-user {:user.registered/keycloak-id (:user.registered/keycloak-id christian)
                                                  :user.registered/roles :role/tester})
          {:keys [:user.registered/roles]} (db/retract-user-attributes-value tester-admin-christian :user.registered/roles :role/tester)]
      (is (= (:user.registered/roles christian) roles)))))
