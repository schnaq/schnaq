(ns schnaq.api.emails-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.api :as api]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.patterns :as pattern]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(defn promote-moderator-request
  [user-mail token]
  (-> {:request-method :post :uri (:path (api/route-by-name :api.moderation/promote-user))
       :body-params {:share-hash "simple-hash"
                     :recipient user-mail
                     :admin-center "link"}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header token)
      toolbelt/test-app))

(deftest promote-user-to-moderator-test
  (testing "Promote a user to be a moderator. It does not matter if they already are one. Non-existent users are not regarded."
    (promote-moderator-request "christian@schnaq.com" toolbelt/token-wegi-no-pro-user)
    (promote-moderator-request "christian@schnaq.com" toolbelt/token-wegi-no-pro-user)
    (promote-moderator-request "non-existing@foobar.de" toolbelt/token-wegi-no-pro-user)
    (is (= 3 (count (:discussion/moderators (fast-pull [:discussion/share-hash "simple-hash"] pattern/discussion)))))))

(deftest promote-user-to-moderator-test-2
  (testing "Promoting without being a mod yourself does not work."
    (is (= 403 (:status (promote-moderator-request "christian@schnaq.com" toolbelt/token-n2o-admin))))))
