(ns schnaq.database.themes-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is use-fixtures testing]]
            [schnaq.database.specs :as specs]
            [schnaq.database.themes :as sut]
            [schnaq.test-data :refer [kangaroo christian theme-anti-social]]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(def ^:private kangaroo-keycloak-id
  (:user.registered/keycloak-id kangaroo))
(def ^:private christian-keycloak-id
  (:user.registered/keycloak-id christian))

(deftest themes-by-keycloak-id-test
  (testing "If user has created a theme, it should be returned."
    (is (= 1 (count (sut/themes-by-keycloak-id kangaroo-keycloak-id))))
    (is (zero? (count (sut/themes-by-keycloak-id christian-keycloak-id))))))

(deftest query-existing-theme-test
  (testing "If theme exists for user, return it."
    (let [query-existing-theme #'sut/query-existing-theme]
      (is (s/valid? ::specs/theme
                    (query-existing-theme kangaroo-keycloak-id theme-anti-social)))
      (is (nil? (query-existing-theme christian-keycloak-id theme-anti-social))))))

(deftest new-theme-test
  (testing "New themes"
    (testing "can be created by users.")
    (is (s/valid? ::specs/theme
                  (sut/new-theme christian-keycloak-id theme-anti-social)))
    (testing "must have unique titles within a user. If not, the title is changed."
      (is (not= (:theme/title theme-anti-social)
                (:theme/title (sut/new-theme kangaroo-keycloak-id theme-anti-social)))))))

(deftest edit-theme-test
  (let [theme (first (sut/themes-by-keycloak-id kangaroo-keycloak-id))]
    (testing "Editing themes"
      (testing "is allowed for the theme creator."
        (is (= "razupaltuff"
               (:theme/title (sut/edit-theme kangaroo-keycloak-id
                                             (assoc theme :theme/title "razupaltuff"))))))
      (testing "is forbidden for user who are not the theme authors."
        (is (nil? (sut/edit-theme christian-keycloak-id (assoc theme :theme/title "razupaltuff"))))))))

(deftest delete-theme-test
  (let [theme (first (sut/themes-by-keycloak-id kangaroo-keycloak-id))]
    (testing "Deleting themes"
      (testing "is allowed for the theme authors."
        (is (not (nil? (sut/delete-theme kangaroo-keycloak-id (:db/id theme))))))
      (testing "is not allowed for other users."
        (is (nil? (sut/delete-theme christian-keycloak-id (:db/id theme))))))))
