(ns schnaq.media-test
  (:require [clojure.test :refer [is deftest testing]]
            [schnaq.media :as media]
            [schnaq.meeting.database :as db]))


(deftest test-cdn-restriction
  (testing "Test that only urls from pixabay are allowed"
    (let [share-hash "aaaa-bbb-ccc"
          _schnaq (db/add-meeting {:meeting/title "Test"
                                   :meeting/start-date (db/now)
                                   :meeting/end-date (db/now)
                                   :meeting/share-hash share-hash
                                   :meeting/author (db/add-user-if-not-exists "Mike")})
          bad-url "https://www.hhu.de/typo3conf/ext/wiminno/Resources/Public/img/hhu_logo.png"
          url "https://cdn.pixabay.com/photo/2020/06/24/23/58/landscape-5338046_960_720.jpg"
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
      (is (= (-> request-1 :body :error) nil)))))