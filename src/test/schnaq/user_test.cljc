(ns schnaq.user-test
  (:require [clojure.test :refer [deftest is testing]]
            [schnaq.config.shared :refer [feature-limits]]
            [schnaq.user :refer [feature-limit]]))

(def ^:private free-user
  {:user.registered/keycloak-id "foo"
   :user.registered/roles #{}
   :user.registered/display-name "Normalo"})

(def ^:private pro-user
  {:user.registered/keycloak-id "foo"
   :user.registered/roles #{:role/pro}
   :user.registered/display-name "Prollo"})

(def ^:private enterprise-user
  {:user.registered/keycloak-id "foo"
   :user.registered/roles #{:role/enterprise}
   :user.registered/display-name "Beam me up!"})

(def ^:private enterprise-custom-user
  {:user.registered/keycloak-id "foo"
   :user.registered/roles #{:role/enterprise}
   :user.registered.features/concurrent-users 400
   :user.registered/display-name "Beam me up!"})

(deftest feature-limit-concurrent-users-test
  (testing "Test valid concurrent user lookup."
    (is (= (get-in feature-limits [:free :concurrent-users])
           (feature-limit free-user :concurrent-users)))
    (is (= (get-in feature-limits [:pro :concurrent-users])
           (feature-limit pro-user :concurrent-users)))
    (is (= (get-in feature-limits [:pro :concurrent-users])
           (feature-limit enterprise-user :concurrent-users)))
    (is (= (get enterprise-custom-user :user.registered.features/concurrent-users)
           (feature-limit enterprise-custom-user :concurrent-users)))))

(deftest feature-limit-wordcloud-test
  (testing "Test wordcloud availability."
    (is (= (get-in feature-limits [:free :wordcloud?])
           (feature-limit free-user :wordcloud?)))
    (is (= (get-in feature-limits [:pro :wordcloud?])
           (feature-limit pro-user :wordcloud?)))))
