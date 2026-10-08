(ns schnaq.mail.emails-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]])
  (:import (java.util Properties)
           (javax.mail Provider Session)))

(deftest single-mail-implementation-test
  (testing "Only postal's javax.mail implementation is on the classpath."
    (is (nil? (io/resource "META-INF/services/jakarta.mail.Provider")))
    (is (.isAssignableFrom Provider (Class/forName "com.sun.mail.imap.IMAPProvider")))
    (is (some? (.getProvider (Session/getInstance (Properties.)) "smtp")))))
