(ns schnaq.s3
  (:require [clojure.string :as str]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [com.fulcrologic.guardrails.core :refer [=> >defn ?]]
            [schnaq.config :as config]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.specs]
            [schnaq.shared-toolbelt :refer [remove-nil-values-from-map]]
            [taoensso.timbre :as log]))

(def s3-client
  "Define a client to connect to our own s3 server. Despite the name, we are not
  using aws, just their libraries."
  (let [{:keys [access-key secret-key endpoint]} config/s3-credentials
        hostname (second (str/split endpoint #"://"))]
    (aws/client {:api :s3
                 :region "eu-central-1"
                 :endpoint-override {:hostname hostname}
                 :credentials-provider (credentials/basic-credentials-provider
                                        {:access-key-id access-key
                                         :secret-access-key secret-key})})))

(>defn absolute-file-url
  "Return absolute URL to bucket."
  [bucket file-name]
  [keyword? :file/name :ret string?]
  (format "%s/%s/%s" shared-config/s3-host (shared-config/s3-buckets bucket) file-name))

(>defn upload-stream
  "Upload a data stream to a specified s3 bucket. Returns relative path to file in bucket."
  [bucket stream file-name {:keys [content-length content-type]}]
  [keyword? :type/input-stream :file/name map? => string?]
  (if-let [resolved-bucket (shared-config/s3-buckets bucket)]
    (do
      (aws/invoke s3-client
                  {:op :PutObject
                   :request (remove-nil-values-from-map
                             {:Bucket resolved-bucket
                              :Key file-name
                              :Body stream
                              :ContentLength content-length
                              :ContentType content-type})})
      (log/info (format "Uploaded file under the key %s to bucket %s, content-type: %s, content-length: %s" file-name resolved-bucket content-type content-length))
      (absolute-file-url bucket file-name))
    (throw (ex-info (format "[upload-stream] No bucket registered for key `%s`" bucket)
                    {:bucket bucket}))))

(>defn delete-file
  "Delete a file in a bucket.
    `filename` can also contain a path to the file, e.g. `foo/bar/baz.png`."
  [bucket-key file-name]
  [keyword? (? :file/name) => (? map?)]
  (when file-name
    (aws/invoke
     s3-client
     {:op :DeleteObject
      :request {:Bucket (shared-config/s3-buckets bucket-key)
                :Key file-name}})))
