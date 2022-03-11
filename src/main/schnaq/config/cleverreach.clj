(ns schnaq.config.cleverreach
  (:require [clojure.string :as str]))

(def enabled?
  "Toggle to use cleverreach mails."
  (or (-> (System/getenv "CLEVERREACH_ENABLED")
          str str/lower-case (= "true"))
      true))

(def receiver-group
  "Define the list of receivers. Defined in 
  https://eu2.cleverreach.com/admin/customer_groups.php."
  (or (System/getenv "CLEVERREACH_RECEIVER_GROUP") ***REMOVED***))

(def client-id
  "OAuth Client ID. Defined in https://eu2.cleverreach.com/admin/account_rest.php"
  (or (System/getenv "CLEVERREACH_OAUTH_CLIENT_ID") "***REMOVED***"))

(def client-secret
  "OAuth Client Secret."
  (or (System/getenv "CLEVERREACH_OAUTH_CLIENT_SECRET")
      "***REMOVED***"))
