(ns schnaq.interface.utils.toolbelt-test
  (:require [cljs.test :refer [deftest are testing]]
            [schnaq.interface.utils.toolbelt :as tools]))

(deftest filename-from-url-test
  (testing "Extract filenames from URLs."
    (are [expected url] (= expected (tools/filename-from-url url))
      nil "foo"
      nil "invalid.url"
      "" "https://schnaq.com"
      "foo" "https://schnaq.com/foo"
      "foo.bar" "https://schnaq.com/foo.bar"
      "sample-file.txt" "https://s3.schnaq.com/schnaq-media/CAFECAFE-CAFE-CAFE-CAFE-CAFECAFECAFE/files/dbaa17e8-2374-4abb-b996-e2f679f141e2/sample-file.txt")))
