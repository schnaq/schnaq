(ns schnaq.interface.views.errors-test
  (:require [cljs.test :refer [deftest is testing]]
            [reagent.core :as reagent]
            [schnaq.interface.views.errors :as errors]))

(deftest not-found-view-stub-renders-nothing-test
  (testing "A view which renders nothing has to return nil. An empty vector is
  invalid hiccup, which reagent only catches with an assertion. Assertions are
  elided in the release build, so there it crashes with \"Cannot set properties
  of null\" deep inside `reagent.impl.template` instead."
    (is (nil? (reagent/as-element (errors/not-found-view-stub))))))
