(ns schnaq.config.cleverreach
  (:require [clojure.string :as str]))

(def enabled?
  "Toggle to use cleverreach mails."
  (or (-> (System/getenv "CLEVERREACH_ENABLED")
          str str/lower-case (= "true"))
      false))

(def receiver-group
  "Define the list of receivers. Defined in 
  https://eu2.cleverreach.com/admin/customer_groups.php."
  (System/getenv "CLEVERREACH_RECEIVER_GROUP"))

(def client-id
  "OAuth Client ID. Defined in https://eu2.cleverreach.com/admin/account_rest.php"
  (System/getenv "CLEVERREACH_OAUTH_CLIENT_ID"))

(def client-secret
  "OAuth Client Secret."
  (System/getenv "CLEVERREACH_OAUTH_CLIENT_SECRET"))
