(ns schnaq.config.cleverreach)

(def enabled?
  "Toggle to user cleverreach mails."
  (or (System/getenv "CLEVERREACH_ENABLED") false))

(def receiver-group
  "Define the list of receivers. Defined in 
  https://eu2.cleverreach.com/admin/customer_groups.php."
  (or (System/getenv "CLEVERREACH_RECEIVER_GROUP") ***REMOVED***))

(def double-opt-in-form
  "The form which is needed to do the double opt-in with cleverreach. For this, 
  add a form at cleverreach and define a target group. Defined in
  https://eu2.cleverreach.com/admin/forms_list.php."
  (or (System/getenv "CLEVERREACH_DOI_FORM") 320010))

(def client-id
  "OAuth Client ID. Defined in https://eu2.cleverreach.com/admin/account_rest.php"
  (or (System/getenv "CLEVERREACH_OAUTH_CLIENT_ID") "***REMOVED***"))

(def client-secret
  "OAuth Client Secret."
  (or (System/getenv "CLEVERREACH_OAUTH_CLIENT_SECRET")
      "***REMOVED***"))
