(ns schnaq.processors-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.meta-info :as meta-info]
            [schnaq.processors :as processors]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest with-votes-processor-test
  (testing "Result should have all statements enriched with votes-metadata"
    (let [share-hash "cat-dog-hash"
          enriched-data (processors/with-aggregated-votes (discussion-db/all-statements share-hash) 123)
          upvotes-only (remove nil? (map :statement/upvotes enriched-data))
          downvotes-only (remove nil? (map :statement/downvotes enriched-data))]
      (is (= 18 (count enriched-data)))
      ;; When there are cummulative votes, they should also yield a result
      (is (= 3 (count upvotes-only) (count downvotes-only))))))

(deftest add-meta-info-test
  (testing "Test if meta info was correctly added to schnaq"
    (let [share-hash "ameisenbär-hash"
          discussion (discussion-db/discussion-by-share-hash share-hash)
          author (:discussion/author discussion)
          discussion-with-meta-info (processors/add-meta-info-to-schnaq discussion)
          meta-info (meta-info/discussion-meta-info share-hash author)
          processed-meta-info (get discussion-with-meta-info :meta-info)]
      (is (= meta-info processed-meta-info)))))

(deftest with-answered?-info-test
  (let [statement {:db/id 17592186045670,
                   :statement/author {:db/id 17592186045540, :user.registered/keycloak-id "d6d8a351-2074-46ff-aa9b-9c57ab6c6a18", :user.registered/display-name "n2o"}
                   :statement/version 1, :statement/created-at #inst "2021-10-12T10:50:43.312-00:00", :statement/content "möp",
                   :statement/children [{:statement/labels [":foo" ":check"]}
                                        {:statement/labels [":ghost"]}]}]
    (is (:meta/answered? (processors/with-answered?-info statement)))
    (is (not (:meta/answered? (processors/with-answered?-info (dissoc statement :statement/children)))))))
