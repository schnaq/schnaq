(ns schnaq.database.user-deletion-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.user-deletion :as user-deletion]
            [schnaq.test-data :as test-data :refer [kangaroo]]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest delete-all-statements-for-user-test
  (let [keycloak-id (:user.registered/keycloak-id kangaroo)
        _ (user-deletion/delete-all-statements-for-user keycloak-id)]
    (is (zero? (count (discussion-db/all-statements-from-user keycloak-id))))))
