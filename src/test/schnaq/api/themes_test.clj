(ns schnaq.api.themes-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [muuntaja.core :as m]
            [schnaq.api :as api]
            [schnaq.database.themes :as themes-db]
            [schnaq.test-data :refer [theme-anti-social schnaqqi]]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(def ^:private sample-theme
  (dissoc theme-anti-social :db/id :theme/user))

(def ^:private raw-image
  {:content "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABAQMAAAAl21bKAAAAA1BMVEUAAACnej3aAAAAAXRSTlMAQObYZgAAAApJREFUCNdjYAAAAAIAAeIhvDMAAAAASUVORK5CYII=",
   :name "1x1.png",
   :type "image/png"})

(def ^:private new-theme-with-images
  "Remove all db-specific information and add raw images."
  (-> theme-anti-social
      (dissoc :db/id :theme/user :theme.images/logo :theme.images/header)
      (assoc :theme.images.raw/logo raw-image
             :theme.images.raw/header raw-image)))

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

(deftest new-theme-with-images-test
  (testing "Image upload when adding a new theme should succeed."
    (let [response (m/decode-response-body (save-theme-request toolbelt/token-n2o-admin new-theme-with-images))]
      (is (-> response :theme :theme.images/logo string?))
      (is (-> response :theme :theme.images/header string?)))))

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
    (let [theme (first (themes-db/themes-by-keycloak-id (:user.registered/keycloak-id schnaqqi)))
          modified-theme (assoc theme
                                :db/id (:db/id theme)
                                :theme/title "changed")
          response (edit-theme-request toolbelt/token-schnaqqifant-user modified-theme)]
      (is (= 200 (:status response)))
      (is (= "changed" (-> response m/decode-response-body :theme :theme/title))))))

(deftest new-theme-with-images-test
  (testing "Image upload when adding a new theme should succeed."
    (let [theme (first (themes-db/themes-by-keycloak-id (:user.registered/keycloak-id schnaqqi)))
          modified-theme (assoc theme
                                :db/id (:db/id theme)
                                :theme.images.raw/logo raw-image
                                :theme.images.raw/header raw-image)
          response (m/decode-response-body (edit-theme-request toolbelt/token-schnaqqifant-user modified-theme))]
      (is (-> response :theme :theme.images/logo string?))
      (is (-> response :theme :theme.images/header string?)))))

;; -----------------------------------------------------------------------------

(defn- delete-theme-request [user-token theme-id]
  (-> {:request-method :delete :uri (:path (api/route-by-name :api.theme/delete))
       :body-params {:theme {:db/id theme-id}}}
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
        (is (= 403 (:status someones-request))))
      (testing "is allowed for the theme's owner."
        (is (= 200 (:status owner-request)))))))
