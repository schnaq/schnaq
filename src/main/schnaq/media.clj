(ns schnaq.media
  (:require [ghostwheel.core :refer [>defn]]
            [clj-http.client :as client]
            [ring.util.http-response :refer [ok]]
            [schnaq.validator :as validator]
            [schnaq.s3 :as s3]
            [taoensso.timbre :as log]))

(defn upload-header-from-url [url key]
  (try
    (let [img (client/get url {:as :stream})]
      (s3/upload-data-to-s3 img key))
    (catch Exception e
      (log/debug (.getMessage e))
      {:error "Setting image failed"})))

(>defn set-preview-image
  "Set preview image"
  [{:keys [body-params]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash edit-hash image-url]} body-params
        key (str "header-" share-hash)]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/debug (str "Set preview image" image-url))
          (upload-header-from-url image-url key)
          (ok (upload-header-from-url image-url key)))
      (validator/deny-access))))

;; experimental stuff

(comment
  (try
    (upload-header-from-url
      "https://cdn.pixabay.com/photo/2020/10/23/17/47/girl-5679419_960_720.jpg"
      "fooo2")
    (catch Exception e
      {:error (.getMessage e)})))