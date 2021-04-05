(ns schnaq.s3
  (:require [amazonica.aws.s3 :as s3]
            [schnaq.config :as config]))

(defn- create-s3-bucket-url [bucket-name]
  (format "%s/%s/" config/s3-base (config/s3-buckets bucket-name)))

(defn upload-stream-to-s3
  "Upload a data stream to a specified s3 bucket."
  [bucket stream file-name content-length]
  (-> config/s3-credentials
      (s3/put-object :bucket-name (config/s3-buckets bucket)
                     :key file-name
                     :input-stream stream
                     :metadata {:content-length content-length}))
  {:message "File upload successful"
   :bucket-url (str (create-s3-bucket-url bucket) file-name)})