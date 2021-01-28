(ns schnaq.media
  (:require [clj-http.client :as client]
            [schnaq.meeting.database :as d]
            [ghostwheel.core :refer [>defn]]
            [ring.util.http-response :refer [ok bad-request forbidden]]
            [schnaq.validator :as validator]
            [schnaq.s3 :as s3]
            [taoensso.timbre :as log]))

(def ^:private trusted-cdn-url-regex
  (re-pattern "https://cdn\\.pixabay\\.com/photo(.+)|https://s3\\.disqtec\\.com/(.+)"))

(def ^:private error-cdn "prohibited cdn")
(def ^:private error-img "Setting image failed")
(def ^:private success-img "Setting image succeeded")

(defn- add-bucket-url-to-database [{:keys [bucket-url]} share-hash]
  (d/transact [{:db/id [:meeting/share-hash share-hash]
                :meeting/header-image-url bucket-url}]))

(defn- upload-img-and-store-url [url key share-hash]
  (try
    (let [img (client/get url {:as :stream})]
      (-> (s3/upload-data-to-s3 img key)
          (add-bucket-url-to-database share-hash)))
    (catch Exception e
      (log/debug (.getMessage e))
      :error-img)))

(defn- check-url [url]
  (if (re-matches trusted-cdn-url-regex url)
    true
    false))

(defn- check-and-upload-image [image-url key share-hash]
  (if (check-url image-url)
    (do (log/debug (format "Set preview image: [%s] for schnaq [%s]" image-url share-hash))
        (upload-img-and-store-url image-url key share-hash))
    :error-forbidden-cdn))

(>defn set-preview-image
  "Check an image url for a valid source and then upload it to s3
  and set a datomic entry to the corresponding schnaq"
  [{:keys [body-params]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash edit-hash image-url]} body-params
        key (str "header-" share-hash)]
    (if (validator/valid-credentials? share-hash edit-hash)
      (case (check-and-upload-image image-url key share-hash)
        :error-img (bad-request {:error error-img})
        :error-forbidden-cdn (forbidden {:error error-cdn})
        (ok {:message success-img}))
      (validator/deny-access))))