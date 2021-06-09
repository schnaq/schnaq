(ns schnaq.config.shared)

#?(:clj  (def keycloak-host
           (or (System/getenv "KEYCLOAK_SERVER") "https://auth.schnaq.com"))
   :cljs (goog-define keycloak-host "https://auth.schnaq.com"))

#?(:clj  (def s3-host
           (or (System/getenv "S3_HOST") "https://s3.disqtec.com"))
   :cljs (goog-define s3-host "https://s3.disqtec.com"))

(defn s3-buckets
  "Returns bucket names"
  [bucket-name]
  (get
    {:schnaq/header-images "schnaq-header-images"
     :user/profile-pictures "schnaq-profile-pictures"
     :feedbacks/screenshots "schnaq-feedback-screenshots"}
    bucket-name))


;; -----------------------------------------------------------------------------
;; Profile Image Upload

(def allowed-mime-types
  "Define a list of allowed mime-types."
  #{"image/jpeg" "image/png"})

(def beta-tester-groups
  #{"schnaqqifantenparty" "beta-tester"})
