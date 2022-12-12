(ns schnaq.config.cleverreach
  (:require [clojure.string :as str]
            [config.core :refer [env]]))

(def enabled?
  "Toggle to use cleverreach mails."
  (or (-> (:cleverreach-enabled env)
          str str/lower-case (= "true"))
      false))

(def receiver-group
  "Define the list of receivers. Defined in 
  https://eu2.cleverreach.com/admin/customer_groups.php."
  (:cleverreach-receiver-group env))

(def client-id
  "OAuth Client ID. Defined in https://eu2.cleverreach.com/admin/account_rest.php"
  (:cleverreach-oauth-client env))

(def client-secret
  "OAuth Client Secret."
  (:cleverreach-oauth-client-secret env))
