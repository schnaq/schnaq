(ns schnaq.config.mailchimp)

(def api-key (System/getenv "MAILCHIMP_API_KEY"))
;; TODO add this to the service

(def ^:private list-id "407d47335d")
(def subscribe-uri (format "https://us8.api.mailchimp.com/3.0/lists/%s/members?skip_merge_validation=true" list-id))