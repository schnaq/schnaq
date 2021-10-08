(ns schnaq.database.access-codes-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest testing is use-fixtures]]
            [schnaq.database.access-codes :as ac]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest generate-code-test
  (testing "Valid codes are generated."
    (is (s/valid? :discussion.access/code (#'ac/generate-code)))))

(def sample {:db/id 17592186045451,
             :discussion.access/code 42,
             :discussion.access/discussion [:discussion/share-hash "1ea965de-bb39-4ae9-85b2-f3b3bad12af0"]
             :discussion.access/created-at #inst"2021-10-06T10:56:36.257-00:00",
             :discussion.access/expires-at #inst"2021-10-07T12:56:36.257-00:00"})

(deftest valid?-test
  (let [valid? #'ac/valid?]
    (testing "Valid access-code with all fields is okay."
      (is (valid? sample)))
    (testing "Missing discussion or missing code is invalid."
      (is (not (valid? (dissoc sample :discussion.access/code)))))
    (testing "If expired is smaller than created, the access code is invalid."
      (is (valid? (assoc sample :discussion.access/expires-at #inst"2021-10-06T10:56:36.257-00:00"))))))

(deftest code-available?-test
  (let [test-discussion (discussion-db/discussion-by-share-hash "cat-dog-hash")
        {:discussion.access/keys [code]} (ac/add-access-code-to-discussion! (:db/id test-discussion) 42)
        code-available? #'ac/code-available?]
    (testing "Verify, that the code is really available."
      (is (not (code-available? code)))
      (is (code-available? 23232323)))))

(deftest remove-invalid-access-codes-test
  (let [valid-discussion {:db/id 17592186045433,
                          :discussion/author {:db/id 17592186045431, :user/nickname "penguin"},
                          :discussion/share-hash "1ea965de-bb39-4ae9-85b2-f3b3bad12af0",
                          :discussion/created-at #inst"2021-10-06T08:15:00.073-00:00",
                          :discussion/title "Huhu was geht hier ab",
                          :discussion/access [{:db/id 17592186045510,
                                               :discussion.access/code 43236077,
                                               :discussion.access/created-at #inst"2021-10-06T12:34:22.363-00:00",
                                               :discussion.access/expires-at #inst"2021-10-07T14:34:22.363-00:00"}]}
        expired-access-code {:db/id 17592186045433,
                             :discussion/author {:db/id 17592186045431, :user/nickname "penguin"},
                             :discussion/share-hash "1ea965de-bb39-4ae9-85b2-f3b3bad12af0",
                             :discussion/created-at #inst"2021-10-06T08:15:00.073-00:00",
                             :discussion/title "Huhu was geht hier ab",
                             :discussion/access [{:db/id 17592186045510,
                                                  :discussion.access/code 43236077,
                                                  :discussion.access/created-at #inst"2021-10-06T12:34:22.363-00:00",
                                                  :discussion.access/expires-at #inst"1111-10-07T14:34:22.363-00:00"}]}]
    (testing "Valid access-code stays in discussion-map."
      (is (:discussion/access (ac/remove-invalid-and-pull-up-access-codes valid-discussion))))))
