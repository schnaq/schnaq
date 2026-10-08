(ns schnaq.interface.views.user.themes-test
  (:require [cljs.test :refer [deftest is testing]]
            [schnaq.interface.views.user.themes :as themes]))

(deftest foreground-for-test
  (testing "Labels on themed colours pick whichever of white and navy contrasts more."
    (let [foreground-for #'themes/foreground-for]
      (is (= "#ffffff" (foreground-for "#123456")))
      (is (= "#ffffff" (foreground-for "#1976d2")))
      (is (= "#001452" (foreground-for "#ff9901")))
      (is (= "#001452" (foreground-for "#cdef01"))))))
