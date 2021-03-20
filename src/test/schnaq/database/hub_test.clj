(ns schnaq.database.hub-test
  (:require [clojure.test :refer [is use-fixtures deftest]]
            [schnaq.database.discussion-test-data :as test-data]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.hub :refer [add-discussions-to-hub] :as hub]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each
              schnaq-toolbelt/init-test-delete-db-fixture
              #(schnaq-toolbelt/init-test-delete-db-fixture % test-data/public-discussions))
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest add-discussions-to-hub-test
  (let [hub (hub/create-hub "test-hub")
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
        new-hub (hub/create-hub name)]
    (is (= name (:hub/name new-hub)))
    (is (empty? (:hub/schnaqs new-hub)))))
