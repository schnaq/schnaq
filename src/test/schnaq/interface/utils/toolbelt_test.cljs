(ns schnaq.interface.utils.toolbelt-test
  (:require [clojure.test :refer [deftest is are testing]]
            [schnaq.interface.utils.toolbelt :refer [remove-from-vector]]))

(deftest remove-from-vector-test
  (are [element coll result]
    (= result (remove-from-vector element coll))
    :foo [:foo :bar] [:bar]
    :foo [:bar] [:bar]
    :foo [] []))
