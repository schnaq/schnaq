(ns schnaq.interface.views.meeting.admin-center-test
  (:require [cljs.test :refer [deftest is testing]]
            [schnaq.interface.views.meeting.admin-center :as admin]))

(deftest parse-local-storage-string-test
  (testing "Check if characters are correctly parsed"
    (let [share-1 "eba08dd5-31c9-4d9e-80d5-0bc2d2f2ad3a"
          edit-1 "2ea16d32-967e-4c6a-9c54-7305bdced0ae"
          local-string "[eba08dd5-31c9-4d9e-80d5-0bc2d2f2ad3a 2ea16d32-967e-4c6a-9c54-7305bdced0ae]"]
      (is (= edit-1 (get (@#'admin/parse-admin-access-string local-string) share-1))))))


(deftest parse-local-storage-string-two-entries-test
  (testing "Check if characters are correctly parsed"
    (let [share-1 "share-1"
          edit-1 "edit-1"
          share-2 "share-2"
          edit-2 "edit-2"
          local-string (str "[" share-1 " " edit-1 "]"
                            ","
                            "[" share-2 " " edit-2 "]")]
      (is (= edit-1 (get (@#'admin/parse-admin-access-string local-string) share-1)))
      (is (= edit-2 (get (@#'admin/parse-admin-access-string local-string) share-2))))))

(deftest add-hash-test
  (testing "Add new share edit hash tuple"
    (let [share-1 "share-1"
          edit-1 "edit-1"
          share-new "share-new"
          edit-new "edit-new"
          local-string (str "[" share-1 " " edit-1 "]")
          hashmap (@#'admin/add-admin-access-to-local-hashmap local-string share-new edit-new)]
      (is (= edit-new (get hashmap share-new))))))
