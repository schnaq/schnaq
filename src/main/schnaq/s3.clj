(ns schnaq.s3
  (:require [amazonica.aws.s3 :as s3]
            [ghostwheel.core :refer [>defn]]
            [schnaq.config :as config]
            [schnaq.config.shared :as shared-config]
            [taoensso.timbre :as log]))

(>defn absolute-file-url
  "Return absolute URL to bucket."
  [bucket file-name]
  [keyword? string? :ret string?]
  (format "%s/%s/%s" shared-config/s3-host (shared-config/s3-buckets bucket) file-name))

(>defn relative-file-path
  "Return relative path to file in bucket, without s3-host."
  [bucket-name file-name]
  [keyword? string? :ret string?]
  (format "%s/%s" (shared-config/s3-buckets bucket-name) file-name))

(defn upload-stream
  "Upload a data stream to a specified s3 bucket. Returns relative path to file in bucket."
  [bucket stream file-name metadata]
  (-> config/s3-credentials
      (s3/put-object :bucket-name (shared-config/s3-buckets bucket)
                     :key file-name
                     :input-stream stream
                     :metadata metadata))
  (log/info "Uploaded file under the key" file-name "to bucket" (shared-config/s3-buckets bucket))
  (absolute-file-url bucket file-name))