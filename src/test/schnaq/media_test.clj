(ns schnaq.media-test
  (:require [clojure.test :refer [is deftest testing use-fixtures]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.user :as user-db]
            [schnaq.media :as media]
            [schnaq.s3 :as s3]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(defn- create-schnaq
  [share-hash]
  (discussion-db/new-discussion {:discussion/title "Test-Schnaq"
                                 :discussion/share-hash share-hash
                                 :discussion/edit-hash "secret"
                                 :discussion/author (user-db/add-user-if-not-exists "Mike")}))

(deftest test-cdn-restriction
  (testing "Test image upload to s3"
    (let [share-hash "aaaa1-bbb2-ccc3"
          _schnaq (create-schnaq share-hash)
          bad-url "https://www.hhu.de/typo3conf/ext/wiminno/Resources/Public/img/hhu_logo.png"
          url (#'s3/absolute-file-url :schnaq/header-images "for-testing-image-do-not-delete")
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
      (is allowed-url)
      (is (not bad-url-1))
      (is (not bad-url-2))
      (is (not bad-url-3)))))
