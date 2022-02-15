(ns schnaq.api.themes-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [schnaq.api :as api]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(defn- save-theme-request [user-token theme]
  (-> {:request-method :post :uri (:path (api/route-by-name :api.theme/add))
       :body-params {:theme theme}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      toolbelt/accept-edn-response-header
      api/app))

(def ^:private sample-theme
  {:theme.images/logo "https://s3.schnaq.com/themes/d6d8a351-2074-46ff-aa9b-9c57ab6c6a18/awesome-title/logo.png"
   :theme.images/activation "https://s3.schnaq.com/themes/d6d8a351-2074-46ff-aa9b-9c57ab6c6a18/awesome-title/activation.png"
   :theme/title "Awesome title"
   :theme.colors/secondary "#123456"
   :theme.colors/background "#7890ab"
   :theme.colors/primary "#cdef01"})

(deftest new-theme-test
  (testing "Creating a new theme"
    (testing "succeeds for eligible users."
      (is (= 200 (:status (save-theme-request toolbelt/token-n2o-admin sample-theme))))
      (is (= 200 (:status (save-theme-request toolbelt/token-schnaqqifant-user sample-theme)))))
    (testing "fails for non-pro users"
      (is (= 403 (:status (save-theme-request toolbelt/token-wegi-no-beta-user sample-theme)))))))
