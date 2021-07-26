(ns schnaq.s3
  (:require [clojure.string :as string]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [ghostwheel.core :refer [>defn]]
            [schnaq.config :as config]
            [schnaq.config.shared :as shared-config]
            [taoensso.timbre :as log]))

(def s3-client
  "Define a client to connect to our own s3 server. Despite the name, we are not
  using aws, just their libraries."
  (let [{:keys [access-key secret-key endpoint]} config/s3-credentials
        hostname (second (string/split endpoint #"://"))]
    (aws/client {:api :s3
                 :region "eu-central-1"
                 :endpoint-override {:hostname hostname}
                 :credentials-provider (credentials/basic-credentials-provider
                                         {:access-key-id access-key
                                          :secret-access-key secret-key})})))

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
  [bucket stream file-name {:keys [content-length content-type]}]
  (aws/invoke s3-client
              {:op :PutObject
               :request {:Bucket (shared-config/s3-buckets bucket)
                         :Key file-name
                         :Body stream
                         :Content-Length content-length
                         :Content-Type content-type}})
  (log/info "Uploaded file under the key" file-name "to bucket" (shared-config/s3-buckets bucket))
  (absolute-file-url bucket file-name))