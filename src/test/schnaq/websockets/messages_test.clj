(ns schnaq.websockets.messages-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schnaq.test.toolbelt :as toolbelt]
            [schnaq.websockets.handler :as handler]
            [schnaq.websockets.messages]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(deftest messages-without-share-hash-test
  (testing "Messages without a share-hash are ignored instead of querying with nil."
    (doseq [id [:discussion.starting/update :discussion.activation/update
                :discussion.graph/update :schnaq.poll/update]]
      (is (nil? (handler/handle-message {:id id :?data {:display-name "Anonymous"}}))))))

(deftest starting-update-test
  (testing "Messages with a share-hash are answered."
    (is (seq (:starting-conclusions
              (handler/handle-message {:id :discussion.starting/update
                                       :?data {:share-hash "simple-hash"
                                               :display-name "Anonymous"}}))))))
