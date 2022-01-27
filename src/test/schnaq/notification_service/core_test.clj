(ns schnaq.notification-service.core-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.main :as main-db]
            [schnaq.notification-service.core :as sut]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest discussions-with-new-statements-in-interval-test
  (let [discussions-with-new-statements-in-interval #'sut/discussions-with-new-statements-in-interval]
    (testing "There should be 18 new statements after initializing the new database."
      (is (= 18 (-> (discussions-with-new-statements-in-interval (main-db/days-ago 1) :notification-mail-interval/daily)
                    (get "cat-dog-hash")
                    :new-statements))))))

;; -----------------------------------------------------------------------------

(def discussions-with-new-statements
  {"fbc512d7-89d1-4f6c-be6a-fc05a44b2976"
   {:db/id 17592186049507
    :discussion/share-hash "fbc512d7-89d1-4f6c-be6a-fc05a44b2976"
    :discussion/title "424242424242"
    :discussion/author 424242424242
    :new-statements {:total 15 :authors #{424242424242}}}
   "468f6865-dd4e-4b2e-9772-08fc8f1e2de5"
   {:db/id 17592186049512
    :discussion/title "232323232323"
    :discussion/author 232323232323
    :discussion/share-hash "468f6865-dd4e-4b2e-9772-08fc8f1e2de5"
    :new-statements {:total 42 :authors #{424242424242 232323232323}}}})

(deftest remove-discussions-from-user-test
  (let [remove-discussions-from-user #'sut/remove-discussions-with-no-other-users]
    (testing "If user is author of newly created statements, remove the discussion from the discussion."
      (is (= 1 (count (remove-discussions-from-user discussions-with-new-statements 424242424242))))
      (is (= 1 (count (remove-discussions-from-user discussions-with-new-statements 232323232323)))))
    (testing "If user is not author, don't remove anything."
      (is (= (count discussions-with-new-statements)
             (count (remove-discussions-from-user discussions-with-new-statements 111111111111)))))))

