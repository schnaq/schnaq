(ns schnaq.s3
  (:require [amazonica.aws.s3 :as s3]
            [ghostwheel.core :refer [>defn]]
            [schnaq.config :as config]))

(>defn absolute-file-url
  "Return absolute URL to bucket."
  [bucket file-name]
  [keyword? string? :ret string?]
  (format "%s/%s/%s" config/s3-host (config/s3-buckets bucket) file-name))

(>defn relative-file-path
  "Return relative path to file in bucket, without s3-host."
  [bucket-name file-name]
  [keyword? string? :ret string?]
  (format "%s/%s" (config/s3-buckets bucket-name) file-name))

(defn upload-stream
  "Upload a data stream to a specified s3 bucket. Returns relative path to file in bucket."
  [bucket stream file-name content-length]
  (-> config/s3-credentials
      (s3/put-object :bucket-name (config/s3-buckets bucket)
                     :key file-name
                     :input-stream stream
                     :metadata {:content-length content-length}))
  (relative-file-path bucket file-name))