(ns schnaq.interface.utils.toolbelt-test
  (:require [cljs.test :refer [deftest is are testing]]
            [schnaq.interface.utils.toolbelt :as tools]))

(deftest filename-from-url-test
  (testing "Extract filenames from URLs."
    (are [expected url] (= expected (tools/filename-from-url url))
      nil "foo"
      nil "invalid.url"
      nil "https://schnaq.com"
      "foo" "https://schnaq.com/foo"
      "foo.bar" "https://schnaq.com/foo.bar"
      "sample-file.txt" "https://s3.schnaq.com/schnaq-media/CAFECAFE-CAFE-CAFE-CAFE-CAFECAFECAFE/files/dbaa17e8-2374-4abb-b996-e2f679f141e2/sample-file.txt")))

(deftest truncate-in-the-middle-test
  (testing "Truncate string in the middle."
    (is (nil? (tools/truncate-in-the-middle "Clojure" 0)))
    (is (= "Cl…re" (tools/truncate-in-the-middle "Clojure" 2)))
    (is (= "Clo…cks" (tools/truncate-in-the-middle "Clojure rocks" 3)))))
