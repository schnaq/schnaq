(ns schnaq.database.hub-test
  (:require [clojure.test :refer [is are use-fixtures deftest testing]]
            [schnaq.database.discussion-test-data :as test-data]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.hub :refer [add-discussions-to-hub] :as hub]
            [schnaq.test.toolbelt :as schnaq-toolbelt])
  (:import (java.util UUID)))

(use-fixtures :each
              schnaq-toolbelt/init-test-delete-db-fixture
              #(schnaq-toolbelt/init-test-delete-db-fixture % test-data/public-discussions))
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest add-discussions-to-hub-test
  (let [hub (hub/create-hub "test-hub" "keycloak-name")
        discussion (first (discussion-db/all-discussions-by-title "Public Test"))
        cat-dog-discussion (first (discussion-db/all-discussions-by-title "Cat or Dog?"))]
    (is (empty? (:hub/schnaqs hub)))
    (let [modified-hub (add-discussions-to-hub (:db/id hub) [(:db/id discussion)
                                                             (:db/id cat-dog-discussion)])]
      (is (= 2 (count (:hub/schnaqs modified-hub))))
      (is (= #{"Public Test" "Cat or Dog?"} (->> modified-hub
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
