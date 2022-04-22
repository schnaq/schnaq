(ns schnaq.api.themes-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [muuntaja.core :as m]
            [schnaq.api :as api]
            [schnaq.api.themes :as themes-api]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.themes :as themes-db]
            [schnaq.test-data :refer [theme-anti-social schnaqqi kangaroo]]
            [schnaq.test.toolbelt :as toolbelt :refer [test-app]]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(def ^:private schnaqqi-keycloak-id
  (:user.registered/keycloak-id schnaqqi))

(def ^:private kangaroo-keycloak-id
  (:user.registered/keycloak-id kangaroo))

(def ^:private sample-theme
  (dissoc theme-anti-social :db/id :theme/user))

(def ^:private raw-image
  {:content "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABAQMAAAAl21bKAAAAA1BMVEUAAACnej3aAAAAAXRSTlMAQObYZgAAAApJREFUCNdjYAAAAAIAAeIhvDMAAAAASUVORK5CYII=",
   :name "1x1.png",
   :type "image/png"})

(def ^:private sample-theme-with-images
  "Remove all db-specific information and add raw images."
  (-> theme-anti-social
      (dissoc :db/id :theme/user :theme.images/logo :theme.images/header)
      (assoc :theme.images.raw/logo raw-image
             :theme.images.raw/header raw-image)))

;; -----------------------------------------------------------------------------

(defn- personal-themes-request [user-token]
  (-> {:request-method :get :uri (:path (api/route-by-name :api.themes/personal))}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      test-app
      m/decode-response-body
      :themes
      count))

(deftest personal-themes-test
  (testing "Querying personal theme returns collection of themes."
    (is (= 1 (personal-themes-request toolbelt/token-schnaqqifant-user)))
    (is (zero? (personal-themes-request toolbelt/token-n2o-admin)))
    (is (zero? (personal-themes-request toolbelt/token-wegi-no-beta-user)))))

;; -----------------------------------------------------------------------------

(defn- save-theme-request [user-token theme]
  (-> {:request-method :post :uri (:path (api/route-by-name :api.theme/add))
       :body-params {:theme theme}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      toolbelt/accept-edn-response-header
      test-app))

(deftest new-theme-test
  (testing "Creating a new theme"
    (testing "succeeds for eligible users."
      (is (= 200 (:status (save-theme-request toolbelt/token-n2o-admin sample-theme))))
      (is (= 200 (:status (save-theme-request toolbelt/token-schnaqqifant-user sample-theme)))))
    (testing "fails for non-pro users"
      (is (= 403 (:status (save-theme-request toolbelt/token-wegi-no-beta-user sample-theme)))))))

(deftest new-theme-with-images-test
  (testing "Image upload when adding a new theme should succeed."
    (let [response (m/decode-response-body (save-theme-request toolbelt/token-n2o-admin sample-theme-with-images))]
      (is (-> response :theme :theme.images/logo string?))
      (is (-> response :theme :theme.images/header string?)))))

;; -----------------------------------------------------------------------------

(defn- edit-theme-request [user-token theme]
  (-> {:request-method :put :uri (:path (api/route-by-name :api.theme/edit))
       :body-params {:theme theme
                     :delete-header? false
                     :delete-logo? false}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      toolbelt/accept-edn-response-header
      test-app))

(deftest edit-theme-test
  (testing "Editing a theme succeeds for eligible users."
    (let [theme (first (themes-db/themes-by-keycloak-id (:user.registered/keycloak-id schnaqqi)))
          modified-theme (assoc theme
                                :db/id (:db/id theme)
                                :theme/title "changed")
          response (edit-theme-request toolbelt/token-schnaqqifant-user modified-theme)]
      (is (= 200 (:status response)))
      (is (= "changed" (-> response m/decode-response-body :theme :theme/title))))))

(deftest edit-theme-with-images-test
  (testing "Image upload when adding a new theme should succeed."
    (let [theme (first (themes-db/themes-by-keycloak-id schnaqqi-keycloak-id))
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
      test-app))

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

;; -----------------------------------------------------------------------------

(defn- assign-theme-request [user-token theme-id share-hash edit-hash]
  (-> {:request-method :put :uri (:path (api/route-by-name :api.theme.discussion/assign))
       :body-params {:theme {:db/id theme-id}
                     :share-hash share-hash
                     :edit-hash edit-hash}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      toolbelt/accept-edn-response-header
      test-app))

(deftest assign-theme-test
  (testing "Assigning a theme to a discussion"
    (let [theme-id (-> (themes-db/themes-by-keycloak-id schnaqqi-keycloak-id) first :db/id)]
      (testing "succeeds for users with valid credentials and pro-account."
        (let [response (assign-theme-request toolbelt/token-schnaqqifant-user theme-id "simple-hash" "simple-hash-secret")]
          (is (= 200 (:status response)))))
      (testing "fails if wrong credentials are provided."
        (let [response (assign-theme-request toolbelt/token-schnaqqifant-user theme-id "simple-hash" "wrong-edit-hash")]
          (is (= 403 (:status response)))))
      (testing "fails if user has no pro access."
        (let [kangaroo-theme-id (-> (themes-db/themes-by-keycloak-id kangaroo-keycloak-id) first :db/id)
              response (assign-theme-request toolbelt/token-kangaroo-normal-user kangaroo-theme-id "simple-hash" "simple-hash-secret")]
          (is (= 403 (:status response))))))))

;; -----------------------------------------------------------------------------

(defn- unassign-theme-request [user-token share-hash edit-hash]
  (-> {:request-method :delete :uri (:path (api/route-by-name :api.theme.discussion/unassign))
       :body-params {:share-hash share-hash
                     :edit-hash edit-hash}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      toolbelt/accept-edn-response-header
      test-app))

(deftest unassign-theme-test
  (testing "Unassigning a theme from a discussion"
    (testing "succeeds for users with valid credentials and pro-account."
      (let [response (unassign-theme-request toolbelt/token-schnaqqifant-user "cat-dog-hash" "cat-dog-edit-hash")
            discussion (discussion-db/discussion-by-share-hash "cat-dog-hash")]
        (is (= 200 (:status response)))
        (is (nil? (:discussion/theme discussion)))))
    (testing "fails if wrong credentials are provided."
      (let [response (unassign-theme-request toolbelt/token-schnaqqifant-user "cat-dog-hash" "razupaltuff")]
        (is (= 403 (:status response)))))
    (testing "fails if user has no pro access."
      (let [response (unassign-theme-request toolbelt/token-kangaroo-normal-user "cat-dog-hash" "cat-dog-edit-hash")]
        (is (= 403 (:status response)))))))

;; -----------------------------------------------------------------------------

(deftest s3-url->path-to-file-test
  (is (= "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/themes/00000000000000/logo.png"
         (#'themes-api/url->path-to-file "https://s3.schnaq.com/user-media/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/themes/00000000000000/logo.png"))))
