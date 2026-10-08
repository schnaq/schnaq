(ns schnaq.interface.views.user.themes-test
  (:require [cljs.test :refer [deftest is testing]]
            [re-frame.registrar :as registrar]
            [schnaq.interface.views.user.themes :as themes]))

(deftest foreground-for-test
  (testing "Labels on themed colours pick whichever of white and navy contrasts more."
    (let [foreground-for #'themes/foreground-for]
      (is (= "#ffffff" (foreground-for "#123456")))
      (is (= "#ffffff" (foreground-for "#1976d2")))
      (is (= "#001452" (foreground-for "#ff9901")))
      (is (= "#001452" (foreground-for "#cdef01"))))))

(deftest set-color-derives-companions-test
  (testing "The colour pickers pass namespaced fields and still update the derived tokens."
    (let [set-color (registrar/get-handler :fx :page.root/set-color)
          remove-color (registrar/get-handler :fx :page.root/remove-color)
          style (.. js/document -documentElement -style)]
      (set-color [:theme.colors/primary "#d00000"])
      (is (= "#d00000" (.getPropertyValue style "--theming-primary-strong")))
      (is (= "208, 0, 0" (.getPropertyValue style "--theming-primary-rgb")))
      (is (= "#ffffff" (.getPropertyValue style "--theming-primary-foreground")))
      (remove-color :theme.colors/primary)
      (is (= "" (.getPropertyValue style "--theming-primary-strong"))))))
