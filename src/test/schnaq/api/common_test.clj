(ns schnaq.api.common-test
  (:require [clojure.test :refer [deftest is testing]]
            [muuntaja.core :as m]
            [schnaq.api :as api]
            [schnaq.config :as config]
            [schnaq.test.toolbelt :refer [test-app]]))

(deftest version-test
  (testing "The backend reports the version it was built from."
    (let [response (test-app {:request-method :get
                              :uri (:path (api/route-by-name :api.other/version))})]
      (is (= 200 (:status response)))
      (is (= config/app-version (:version (m/decode-response-body response)))))))
