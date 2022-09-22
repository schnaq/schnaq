(ns schnaq.interface.navigation-test
  (:require [clojure.test :refer [deftest testing is are]]
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

(deftest canonical-route-name-test
  (testing "Valid prefixes in namespace should be stripped off."
    (are [route-name canonical-route-name] (= canonical-route-name
                                              (navigation/canonical-route-name route-name))
                                           :routes.schnaq/moderation-center :routes.schnaq/moderation-center
                                           :de.routes.schnaq/moderation-center :routes.schnaq/moderation-center
                                           :en.routes.schnaq/moderation-center :routes.schnaq/moderation-center
                                           :foo.routes.schnaq/moderation-center :foo.routes.schnaq/moderation-center)))
