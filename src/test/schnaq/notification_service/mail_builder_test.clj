(ns schnaq.notification-service.mail-builder-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.main :as main-db]
            [schnaq.notification-service.core :as notification-service]
            [schnaq.notification-service.mail-builder :as sut]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(defn- load-users []
  (#'notification-service/users-with-changed-discussions
   (main-db/days-ago 1)
   :notification-mail-interval/daily))

(deftest build-new-statements-html-test
  (testing "At least an html-encoded string must be existent."
    (is (< 10 (count (sut/build-new-statements-html (first (load-users))))))))

(deftest build-new-statements-plain-test
  (testing "Plain text message is produced on new strings"
    (is (< 10 (count (sut/build-new-statements-plain (first (load-users))))))))

(deftest build-personal-greeting-test
  (testing "display-name should be present in a greeting."
    (let [display-name "kangaroo"]
      (is (str/includes? (sut/build-personal-greeting {:user.registered/display-name display-name}) display-name)))))

(deftest build-number-unseen-statements-test
  (let [new-statements 42]
    (is (str/includes? (sut/build-number-unseen-statements new-statements) (str new-statements)))))
