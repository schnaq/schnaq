(ns schnaq.database.hub-test
  (:require [clojure.test :refer [is are use-fixtures deftest testing]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.hub :refer [add-discussions-to-hub] :as hub]
            [schnaq.database.hub-test-data :as hub-test-data]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.test.toolbelt :as schnaq-toolbelt])
  (:import (java.util UUID)))

(use-fixtures :each
              schnaq-toolbelt/init-test-delete-db-fixture
              #(schnaq-toolbelt/init-test-delete-db-fixture % hub-test-data/hub-test-data))
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest add-discussions-to-hub-test
  (let [hub (hub/create-hub "test-hub" "keycloak-name")
        discussion (first (discussion-db/all-discussions-by-title "Tapir oder Ameisenbär?"))
        cat-dog-discussion (first (discussion-db/all-discussions-by-title "Cat or Dog?"))]
    (is (empty? (:hub/schnaqs hub)))
    (let [modified-hub (add-discussions-to-hub (:db/id hub) [(:db/id discussion)
                                                             (:db/id cat-dog-discussion)])]
      (is (= 2 (count (:hub/schnaqs modified-hub))))
      (is (= #{"Tapir oder Ameisenbär?" "Cat or Dog?"} (->> modified-hub
                                                            :hub/schnaqs
                                                            (map :discussion/title)
                                                            set))))))

(deftest create-hub-test
  (let [name "porky"
        new-hub (hub/create-hub name "keycloak-name")]
    (is (= name (:hub/name new-hub)))
    (is (empty? (:hub/schnaqs new-hub)))))

(deftest hubs-by-keycloak-names-test
  (testing "Valid keycloak-names should be resolved and return corresponding hub entities."
    (let [keycloak-names [(.toString (UUID/randomUUID)) (.toString (UUID/randomUUID))]
          _ (run! (partial hub/create-hub "hubby") keycloak-names)]
      (are [times coll]
        (= times (count (hub/hubs-by-keycloak-names coll)))
        0 []
        0 ["razupaltuff"]
        2 keycloak-names))))

(deftest all-schnaqs-for-hub-test
  (testing "Test whether the schnaqs for a hub are correctly pulled."
    (let [test-schnaqs (#'hub/all-schnaqs-for-hub [:hub/keycloak-name "test-keycloak"])]
      (is (= 2 (count test-schnaqs)))
      (is (contains? (first test-schnaqs) :hub/created-at))
      (is (contains? (second test-schnaqs) :hub/created-at))
      (is (some #{(-> test-schnaqs first :discussion/title)} ["Another Hub Discussion" "Hub Discussion"]))
      (is (empty? (#'hub/all-schnaqs-for-hub [:hub/keycloak-name "some-empty-hub"]))))))

(deftest change-hub-name-test
  (testing "When a hub exists, a group member can change it's name."
    (let [keycloak-name "schnaqqifantenparty"
          _ (hub/create-hub "hubby" keycloak-name)]
      (is (= "thisisfine" (:hub/name (hub/change-hub-name keycloak-name "thisisfine")))))))

(deftest create-hubs-if-not-existing-test
  (testing "Test whether new hubs are created in bulk, when not already there."
    (let [non-existent-hub-id "schnaqqifantenparty"
          existent-hub-id "test-keycloak"]
      (is (= {:db/id nil} (fast-pull [:hub/keycloak-name non-existent-hub-id])))
      (is (not (nil? (:db/id (fast-pull [:hub/keycloak-name existent-hub-id])))))
      (hub/create-hubs-if-not-existing [non-existent-hub-id existent-hub-id])
      (is (not (nil? (:db/id (fast-pull [:hub/keycloak-name non-existent-hub-id]))))))))
