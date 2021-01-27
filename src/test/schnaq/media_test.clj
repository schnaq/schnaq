(ns schnaq.media-test
  (:require [clojure.test :refer [is deftest testing use-fixtures]]
            [schnaq.media :as media]
            [schnaq.meeting.database :as db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(defn- create-schnaq
  [share-hash]
  (db/add-meeting {:meeting/title "Test-Schnaq"
                   :meeting/start-date (db/now)
                   :meeting/end-date (db/now)
                   :meeting/share-hash share-hash
                   :meeting/author (db/add-user-if-not-exists "Mike")}))

(deftest test-cdn-restriction
  (testing "Test that only urls from pixabay are allowed"
    (let [share-hash "aaaa1-bbb2-ccc3"
          _schnaq (create-schnaq share-hash)
          bad-url "https://www.hhu.de/typo3conf/ext/wiminno/Resources/Public/img/hhu_logo.png"
          url "https://s3.disqtec.com/schnaq-header-images/fooo2"
          key "Test-Upload"
          bad-share "foo"
          bad-request-1 (@#'media/check-and-upload-image
                          bad-url
                          key
                          share-hash)
          bad-request-2 (@#'media/check-and-upload-image
                          url
                          key
                          bad-share)
          request-1 (@#'media/check-and-upload-image
                      url
                      key
                      share-hash)]
      (is (= (-> bad-request-1 :body :error) @#'media/error-cdn))
      (is (= (-> bad-request-2 :body :error) @#'media/error-img))
      (is (nil? (-> request-1 :body :error))))))