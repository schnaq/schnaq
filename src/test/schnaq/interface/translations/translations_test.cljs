(ns schnaq.interface.translations.translations-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.set :as set]
            [schnaq.interface.translations.english :as english]
            [schnaq.interface.translations.german :as german]))

(deftest same-keys-in-all-languages-test
  (testing "Every translation key exists in English and in German."
    (let [en-keys (set (keys english/labels))
          de-keys (set (keys german/labels))]
      (is (= #{} (set/difference en-keys de-keys)) "Keys missing in German")
      (is (= #{} (set/difference de-keys en-keys)) "Keys missing in English"))))
