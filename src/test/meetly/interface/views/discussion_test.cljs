(ns meetly.interface.views.discussion-test
  (:require [cljs.test :refer [deftest is testing run-tests]]
            [meetly.interface.views.discussion.logic :as d]))

(deftest calculate-votes-test
  (testing "Tests, whether the calculate votes method does its job."
    (let [statement-1 {:db/id 123
                       :meta/upvotes 3
                       :meta/downvotes 1}
          statement-2 {:db/id 321
                       :meta/upvotes 0
                       :meta/downvotes 1}
          vote-store {:up {123 1}
                      :down {321 1}}
          calculate-votes @#'d/calculate-votes]
      (is (= 4 (calculate-votes statement-1 :upvotes vote-store)))
      (is (= 0 (calculate-votes statement-2 :upvotes vote-store)))
      (is (= 1 (calculate-votes statement-1 :downvotes vote-store)))
      (is (= 2 (calculate-votes statement-2 :downvotes vote-store))))))