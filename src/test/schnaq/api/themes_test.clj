(ns schnaq.api.themes-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [muuntaja.core :as m]
            [schnaq.api :as api]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(def ^:private sample-theme
  {:theme.images/logo "https://s3.schnaq.com/themes/d6d8a351-2074-46ff-aa9b-9c57ab6c6a18/awesome-title/logo.png"
   :theme.images/activation "https://s3.schnaq.com/themes/d6d8a351-2074-46ff-aa9b-9c57ab6c6a18/awesome-title/activation.png"
   :theme/title "Awesome title"
   :theme.colors/secondary "#123456"
   :theme.colors/background "#7890ab"
   :theme.colors/primary "#cdef01"})

;; -----------------------------------------------------------------------------

(defn- save-theme-request [user-token theme]
  (-> {:request-method :post :uri (:path (api/route-by-name :api.theme/add))
       :body-params {:theme theme}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      toolbelt/accept-edn-response-header
      api/app))

(deftest new-theme-test
  (testing "Creating a new theme"
    (testing "succeeds for eligible users."
      (is (= 200 (:status (save-theme-request toolbelt/token-n2o-admin sample-theme))))
      (is (= 200 (:status (save-theme-request toolbelt/token-schnaqqifant-user sample-theme)))))
    (testing "fails for non-pro users"
      (is (= 403 (:status (save-theme-request toolbelt/token-wegi-no-beta-user sample-theme)))))))

;; -----------------------------------------------------------------------------

(defn- edit-theme-request [user-token theme]
  (-> {:request-method :put :uri (:path (api/route-by-name :api.theme/edit))
       :body-params {:theme theme}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      toolbelt/accept-edn-response-header
      api/app))

(deftest edit-theme-test
  (testing "Editing a theme succeeds for eligible users."
    (let [theme (-> (save-theme-request toolbelt/token-n2o-admin sample-theme)
                    m/decode-response-body :theme)
          modified-theme (assoc theme
                                :db/id (:db/id theme)
                                :theme/title "changed")
          request (edit-theme-request toolbelt/token-n2o-admin modified-theme)]
      (is (= 200 (:status request)))
      (is (= "changed" (-> request m/decode-response-body :theme :theme/title))))))

;; -----------------------------------------------------------------------------

(defn- delete-theme-request [user-token theme-id]
  (-> {:request-method :delete :uri (:path (api/route-by-name :api.theme/delete))
       :body-params {:theme-id theme-id}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      toolbelt/accept-edn-response-header
      api/app))

(deftest delete-theme-test
  (testing "Deleting a theme"
    (let [theme (-> (save-theme-request toolbelt/token-n2o-admin sample-theme)
                    m/decode-response-body :theme)
          someones-request (delete-theme-request toolbelt/token-schnaqqifant-user (:db/id theme))
          owner-request (delete-theme-request toolbelt/token-n2o-admin (:db/id theme))]
      (testing "is not allowed for other users."
        (is (= 400 (:status someones-request))))
      (testing "is allowed for the theme's owner."
        (is (= 200 (:status owner-request)))))))
