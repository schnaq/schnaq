(ns schnaq.interface.tour-test
  (:require ["react-joyride" :refer [Joyride STATUS]]
            [cljs.test :refer [deftest is testing]]
            [oops.core :refer [oget]]))

(deftest joyride-component-is-defined-test
  (testing "react-joyride 3 dropped its default export. If `Joyride` ever
  resolves to `undefined` again, React only fails at render time with
  \"Element type is invalid\", so assert the import here."
    (is (fn? Joyride))))

(deftest joyride-statuses-are-defined-test
  (testing "The statuses the tour uses to detect that it is over."
    (is (= "finished" (oget STATUS :FINISHED)))
    (is (= "skipped" (oget STATUS :SKIPPED)))))
