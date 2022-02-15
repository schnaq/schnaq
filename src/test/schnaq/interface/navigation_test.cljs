(ns schnaq.interface.navigation-test
  (:require [clojure.test :refer [deftest testing is]]
            [schnaq.interface.navigation :as navigation]))

(deftest replace-language-in-path-test
  (let [replace-language-in-path @#'navigation/replace-language-in-path]
    (testing "Passing no element should clean the path of a language prefix."
      (is (= "/test/clean" (replace-language-in-path "https://schnaq.com/en/test/clean")))
      (is (= "/ente/clean" (replace-language-in-path "https://schnaq.com/ente/clean"))))
    (testing "Passing a locale should add it or replace the current one."
      (is (= "/de/test/clean" (replace-language-in-path "https://schnaq.com/en/test/clean" :de)))
      (is (= "/de/tu/test/clean" (replace-language-in-path "https://schnaq.com/tu/test/clean" :de)))
      (is (= "/es/test/clean" (replace-language-in-path "https://schnaq.com/de/test/clean" :es))))))