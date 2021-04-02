(ns schnaq.s3
  (:require [amazonica.aws.s3 :as s3]
            [schnaq.config :as config]))

(defn create-s3-bucket-url [bucket-name]
  (format "%s/%s/" config/s3-base (config/s3-buckets bucket-name)))

(defn upload-stream-to-s3 [bucket stream file-name]
  (-> config/s3-credentials
      (s3/put-object :bucket-name (config/s3-buckets bucket)
                     :key file-name
                     :input-stream (:body stream)
                     :metadata {:content-length (:length stream)}))
  {:message "File upload successful"
   :bucket-url (str (create-s3-bucket-url bucket) file-name)})