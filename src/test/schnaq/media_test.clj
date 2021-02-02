(ns schnaq.media-test
  (:require [clojure.test :refer [is deftest testing use-fixtures]]
            [schnaq.config :as config]
            [schnaq.media :as media]
            [schnaq.meeting.database :as db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(defn- create-schnaq
  [share-hash]
  (let [meeting-id (db/add-meeting {:meeting/title "Test-Schnaq"
                                    :meeting/start-date (db/now)
                                    :meeting/end-date (db/now)
                                    :meeting/share-hash share-hash
                                    :meeting/author (db/add-user-if-not-exists "Mike")})]
    (db/add-agenda-point "Title" "desc" meeting-id 1 true share-hash "bla" (db/add-user-if-not-exists "Mike"))))

(deftest test-cdn-restriction
  (testing "Test image upload to s3"
    (let [share-hash "aaaa1-bbb2-ccc3"
          _schnaq (create-schnaq share-hash)
          bad-url "https://www.hhu.de/typo3conf/ext/wiminno/Resources/Public/img/hhu_logo.png"
          url (format "%s%s" config/s3-bucket-header-url "for-testing-image-do-not-delete")
          key "Test-Upload"
          bad-share "foo"
          check-and-upload-image #'media/check-and-upload-image
          bad-request-1 (check-and-upload-image
                          bad-url
                          key
                          share-hash)
          bad-request-2 (check-and-upload-image
                          url
                          key
                          bad-share)
          request-1 (check-and-upload-image
                      url
                      key
                      share-hash)]
      (is (= bad-request-1 :error-forbidden-cdn))
      (is (= bad-request-2 :error-img))
      (is (not (nil? (-> request-1 :db-after)))))))

(deftest test-cdn-regex
  (testing "Test that only pixabay's cdn url is allowed"
    (let [valid-url? #'media/valid-url?
          allowed-url (valid-url?
                        "https://cdn.pixabay.com/photo/2020/10/23/17/47/girl-5679419_960_720.jpg")
          bad-url-1 (valid-url?
                      "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png")
          bad-url-2 (valid-url?
                      "https://www.hhu.de/typo3conf/ext/wiminno/Resources/Public/img/hhu_logo.png")
          bad-url-3 (valid-url?
                      "https://pixabay.com/foo.jpg")]
      (is (true? allowed-url))
      (is (false? bad-url-1))
      (is (false? bad-url-2))
      (is (false? bad-url-3)))))