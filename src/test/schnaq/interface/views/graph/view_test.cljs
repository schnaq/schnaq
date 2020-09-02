(ns schnaq.interface.views.graph.view-test
  (:require [clojure.test :refer [deftest is testing]]
            [schnaq.interface.views.graph.view :as graph]))

(deftest wrap-node-labels-test
  (testing "Test whether the node labels are wrapped as expected."
    (let [wrap-node-labels @#'graph/wrap-node-labels
          easy-nodes [{:label "12345"} {:label "123456"}]
          perfect-match [{:label "wegi hat8 buchstaben"}]]
      (is (= [{:label "12345"} {:label "12345\n6"}] (wrap-node-labels 5 easy-nodes)))
      (is (= [{:label "wegi hat8 \nbuchstaben"}] (wrap-node-labels 10 perfect-match))))))
