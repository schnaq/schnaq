(ns schnaq.api.feedback-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [schnaq.api :as api]
            [schnaq.api.dto-specs :as dto]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(defn- add-feedback-request [payload]
  (-> {:request-method :post :uri (:path (api/route-by-name :api.feedback/add))
       :headers {"accept" "application/edn"}
       :body-params {:feedback payload}}
      api/app))

(deftest add-feedback
  (testing "Minimum feedback has no image and description."
    (let [feedback {:description "Some feedback"
                    :has-image? false}]
      (is (= 201 (:status (add-feedback-request feedback))))

      (is (->> (add-feedback-request feedback)
               :body slurp read-string :feedback
               (s/valid? ::dto/feedback)))))
  (testing "Bad request if parameters are missing."
    (is (= 400 (:status (add-feedback-request {}))))))