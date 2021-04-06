(ns schnaq.shared-config)

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