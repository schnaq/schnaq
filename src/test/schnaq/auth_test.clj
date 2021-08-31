(ns schnaq.auth-test
  (:require [clojure.test :refer [deftest testing is]]
            [ring.mock.request :as mock]
            [schnaq.auth :as auth]))

(deftest member-of-group?-test
  (testing "Verify that user is member of called group."
    (let [identity (:identity (assoc-in (mock/request :get "/testing/stuff")
                                        [:identity :groups] ["these-are-my-groups" "schnaqqifantenparty"]))]
      (is (auth/member-of-group? identity "schnaqqifantenparty"))
      (is (not (auth/member-of-group? identity "")))
      (is (not (auth/member-of-group? identity "not-member-of"))))))
