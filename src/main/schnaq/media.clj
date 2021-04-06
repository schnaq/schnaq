(ns schnaq.media
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn]]
            [image-resizer.core :as resizer-core]
            [image-resizer.format :as resizer-format]
            [ring.util.http-response :refer [ok bad-request forbidden]]
            [schnaq.meeting.database :as d]
            [schnaq.s3 :as s3]
            [schnaq.validator :as validator]
            [taoensso.timbre :as log])
  (:import (java.util Base64)))

(def ^:private trusted-cdn-url-regex
  (re-pattern "https://cdn\\.pixabay\\.com/photo(.+)|https://s3\\.disqtec\\.com/(.+)"))

(def ^:private error-cdn "prohibited cdn")
(def ^:private error-img "Setting image failed")
(def ^:private success-img "Setting image succeeded")

(defn- add-bucket-url-to-database [{:keys [bucket-url]} share-hash]
  (d/transact [{:db/id [:discussion/share-hash share-hash]
                :discussion/header-image-url bucket-url}]))

(defn- upload-img-and-store-url [url key share-hash]
  (try
    (let [img (client/get url {:as :stream})]
      (-> (s3/upload-stream-to-s3 :schnaq/header-images img key)
          (add-bucket-url-to-database share-hash)))
    (catch Exception e
      (log/debug (.getMessage e))
      :error-img)))

(defn- valid-url? [url]
  (re-matches trusted-cdn-url-regex url))

(defn- check-and-upload-image [image-url key share-hash]
  (if (valid-url? image-url)
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

(>defn scale-image-to-height
  "Scale image data url to a specified height and return it as input stream"
  [image-data-url height]
  [string? number? :ret any?]
  (let [[header image-without-header] (string/split image-data-url #",")
        #^bytes image-bytes (.decode (Base64/getDecoder) ^String image-without-header)
        image-type (second (re-find #"/([A-z]*);" header))]
    (when image-type
      (resizer-format/as-stream
        (resizer-core/resize-to-height (io/input-stream image-bytes) height)
        image-type))))
