(ns schnaq.interface.components.wordcloud-test
  (:require [clojure.test :refer [deftest are testing]]
            [schnaq.interface.components.wordcloud :as wordcloud]))

(deftest remove-md-links-test
  (let [remove-md-links #'wordcloud/remove-md-links]
    (testing "Markdown links should be removed."
      (are [result input] (= result (remove-md-links input))
        "foo" "foo"
        "iframe" "[iframe](https://schnaq.com/blog/was-sind-faqs/#f%C3%BCr-nerds)"
        "siehst du hier oder in diesem Artikel." "siehst du [hier](https://schnaq.com/schnaq/e8f54922-0d88-4953-8f43-ddc819d7f201/statement/17592186067151) oder [in diesem Artikel](https://schnaq.com/blog/was-sind-faqs/#dynamische-faqs)."))))
