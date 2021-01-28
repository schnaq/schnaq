(ns schnaq.s3
  (:require [amazonica.aws.s3 :as s3]
            [schnaq.config :as config]))

(defn upload-data-to-s3 [file file-name]
  (-> config/s3-credentials
      (s3/put-object :bucket-name config/s3-bucket-headers
                     :key file-name
                     :input-stream (:body file)
                     :metadata {:content-length (:length file)}))
  {:message "Image upload sucessfull"
   :bucket-url (str config/s3-bucket-header-url file-name)})