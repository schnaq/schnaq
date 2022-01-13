(ns schnaq.api.middlewares-test
  (:require [clojure.test :refer [deftest is are testing]]
            [schnaq.api.middlewares :as middlewares]
            [schnaq.config :as config]))

(defn- build-request [verb route-name header?]
  (cond-> {:request-method verb
           :reitit.core/match {:data {:name route-name}}}
    header? (assoc-in [:headers "x-schnaq-csrf"] "👍")))

(deftest wrap-custom-schnaq-csrf-header-test
  (testing "CSRF Header must be present if manipulating-verb is used and the route is not whitelisted."
    (let [mw (middlewares/wrap-custom-schnaq-csrf-header (constantly {:status 200}))]
      (are [status-code verb route-name header?] (= status-code (:status (mw (build-request verb route-name header?))))
      ;; Head and get are okay
        200 :head :_ false
        200 :get :_ false
        200 :get :api.schnaqs/by-hashes false

        ;; Manipulating verbs are forbidden without csrf header
        403 :post :_ false
        403 :delete :_ false
        403 :put :_ false
        403 :put :user/register false

        ;; Manipulating verbs are allowed without csrf header
        200 :post :_ true
        200 :delete :_ true
        200 :put :_ true
        200 :put :user/register true

        ;; Whitelisted routes are okay, no matter which csrf header is present
        200 :post (first config/routes-without-csrf-check) false
        200 :post (first config/routes-without-csrf-check) true))))
