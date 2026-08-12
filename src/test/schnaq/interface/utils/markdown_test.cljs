(ns schnaq.interface.utils.markdown-test
  (:require [cljs.test :refer [deftest is testing]]
            [schnaq.interface.utils.markdown :as markdown]))

(defn- anchor-props
  "Render the anchor and return the props of the resulting hiccup element."
  [props]
  (second (markdown/Anchor props)))

(deftest anchor-decodes-href-test
  (testing "URL-encoded hrefs are decoded."
    (is (= "https://schnaq.com/a b"
           (:href (anchor-props {:href "https://schnaq.com/a%20b"
                                 :children ["schnaq"]}))))))

(deftest anchor-decodes-and-truncates-label-test
  (testing "The link's label is decoded and truncated in the middle."
    (is (= "hello world"
           (first (:children (anchor-props {:href "https://schnaq.com"
                                            :children ["hello%20world"]})))))
    (is (= "aaaaaaaaaaaaaaaa…bbbbbbbbbbbbbbbb"
           (first (:children (anchor-props {:href "https://schnaq.com"
                                            :children [(str (apply str (repeat 16 "a"))
                                                            "cccc"
                                                            (apply str (repeat 16 "b")))]})))))))

(deftest anchor-without-children-test
  (testing "A link without a label, e.g. `[](https://schnaq.com)`, is passed to
  us without any children at all and must not blow up."
    (is (= {:href "https://schnaq.com"}
           (anchor-props {:href "https://schnaq.com"})))
    (is (= {:href "https://schnaq.com" :children []}
           (anchor-props {:href "https://schnaq.com" :children []})))))

(deftest anchor-with-element-child-test
  (testing "A link wrapping an element, e.g. an image, keeps that child as-is."
    (let [child {:type "img" :props {:src "i.png"}}]
      (is (= [child]
             (:children (anchor-props {:href "https://schnaq.com"
                                       :children [child]})))))))

(deftest anchor-without-href-test
  (testing "A missing href must not blow up either."
    (is (= {:children ["schnaq"]}
           (anchor-props {:children ["schnaq"]})))))
