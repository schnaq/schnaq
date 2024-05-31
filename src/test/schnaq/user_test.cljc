(ns schnaq.user-test
  (:require [clojure.test :refer [deftest is testing]]
            [schnaq.user :refer [feature-limit posts-limit-reached? usage-warning-level] :as user]))

(def ^:private feature-limits @#'user/feature-limits)

(def ^:private free-user
  {:user.registered/keycloak-id "foo"
   :user.registered/roles #{}
   :user.registered/display-name "Normalo"})

(def ^:private pro-user
  {:user.registered/keycloak-id "foo"
   :user.registered/roles #{:role/pro}
   :user.registered/display-name "Prollo"})

(def ^:private admin-user
  {:user.registered/keycloak-id "foo"
   :user.registered/roles #{:role/admin}
   :user.registered/display-name "Chef"})

(def ^:private enterprise-user
  {:user.registered/keycloak-id "foo"
   :user.registered/roles #{:role/enterprise}
   :user.registered/display-name "Beam me up!"})

(def ^:private enterprise-custom-user
  {:user.registered/keycloak-id "foo"
   :user.registered/roles #{:role/enterprise}
   :user.registered.features/concurrent-users 400
   :user.registered/display-name "Beam me up!"})

(def ^:private posts-per-schnaq-free
  (get-in feature-limits [:free :posts-per-schnaq]))

(deftest feature-limit-concurrent-users-test
  (testing "Test valid concurrent user lookup."
    (is (= (get-in feature-limits [:pro :concurrent-users])
           (feature-limit free-user :concurrent-users)))
    (is (= (get-in feature-limits [:pro :concurrent-users])
           (feature-limit pro-user :concurrent-users)))
    (is (= (get-in feature-limits [:pro :concurrent-users])
           (feature-limit enterprise-user :concurrent-users)))
    (is (= (get enterprise-custom-user :user.registered.features/concurrent-users)
           (feature-limit enterprise-custom-user :concurrent-users)))))

(deftest feature-limit-wordcloud-test
  (testing "Test wordcloud availability."
    (is (= (get-in feature-limits [:pro :wordcloud?])
           (feature-limit free-user :wordcloud?)))
    (is (= (get-in feature-limits [:pro :wordcloud?])
           (feature-limit pro-user :wordcloud?)))))

(deftest usage-warning-level-test
  (is (nil? (usage-warning-level free-user :posts-per-schnaq (/ posts-per-schnaq-free 3))))
  (is (= nil (usage-warning-level free-user :posts-per-schnaq (- posts-per-schnaq-free 10))))
  (is (= nil (usage-warning-level free-user :posts-per-schnaq (dec posts-per-schnaq-free)))))

(deftest posts-limit-reached?-pro-user-test
  (testing "Pro users have other limits."
    (is (not (posts-limit-reached? pro-user {:meta-info {:all-statements 10}})))
    (is (not (posts-limit-reached? pro-user {:meta-info {:all-statements 10000}})))))

(deftest posts-limit-reached?-admin-user-test
  (testing "Admin users have no limits."
    (is (not (posts-limit-reached? admin-user {:meta-info {:all-statements 10}})))
    (is (not (posts-limit-reached? admin-user {:meta-info {:all-statements 10000}})))))
