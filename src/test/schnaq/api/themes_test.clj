(ns schnaq.api.themes-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [schnaq.api :as api]
            [schnaq.api.themes :as themes]
            [schnaq.test-data :refer [kangaroo]]
            [schnaq.test.toolbelt :as toolbelt]
            [muuntaja.core :as m]))

(def ^:private keycloak-id "d6d8a351-2074-46ff-aa9b-9c57ab6c6a18")

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(defn- save-theme-request [user-token theme]
  (-> {:request-method :post :uri (:path (api/route-by-name :api.theme/new))
       :body-params theme}
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

(deftest save-test
  (testing "Saving theme"
    (testing "with new title succeeds."
      (is (= 200 (:status (save-theme-request toolbelt/token-n2o-admin sample-theme))))
      (is (= "theme/title-taken"
             (-> (save-theme-request toolbelt/token-schnaqqifant-user sample-theme)
                 m/decode-response-body
                 :error))))
    (testing "fails if title theme already exists for user."
      (is (= 400 (:status (save-theme-request toolbelt/token-n2o-admin sample-theme)))))))

(comment

  (-> (save-theme-request toolbelt/token-n2o-admin sample-theme)
      m/decode-response-body)

  nil)
