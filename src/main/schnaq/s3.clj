(ns schnaq.s3
  (:require [amazonica.aws.s3 :as s3]))

(def ^:private bucket-headers "schnaq-header-images")

(def ^:private bucket-url "https://s3.disqtec.com/schnaq-header-images/")

(def ^:private credentials {:access-key "minio"
                            :secret-key "***REMOVED***"
                            :endpoint "https://s3.disqtec.com"
                            :client-config
                            {:path-style-access-enabled true}})

(defn upload-data-to-s3 [file key]
  (-> credentials
      (s3/put-object :bucket-name bucket-headers
                     :key key
                     :input-stream (:body file)
                     :metadata {:content-length (:length file)}))
  {:message "Image upload sucessfull"
   :bucket-url (str bucket-url key)})