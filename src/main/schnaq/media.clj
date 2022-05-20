(ns schnaq.media
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [com.fulcrologic.guardrails.core :refer [>defn >defn-]]
            [image-resizer.core :as resizer-core]
            [image-resizer.format :as resizer-format]
            [ring.util.http-response :refer [bad-request created forbidden]]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.main :as d]
            [schnaq.database.specs :as specs]
            [schnaq.s3 :as s3]
            [taoensso.timbre :as log])
  (:import (java.util Base64 UUID)))

(def ^:private trusted-cdn-url-regex
  (re-pattern "https://cdn\\.pixabay\\.com/photo(.+)|https://s3\\.(disqtec|schnaq)\\.com/(.+)"))

(def ^:private error-cdn "prohibited cdn")
(def ^:private error-img "Setting image failed")
(def ^:private success-img "Setting image succeeded")

(def mime-type->file-ending
  {"image/jpeg" "jpg"
   "image/png" "png"
   "image/webp" "webp"})

;; -----------------------------------------------------------------------------

(defn- add-bucket-url-to-database [relative-file-path share-hash]
  @(d/transact [[:db/add [:discussion/share-hash share-hash]
                 :discussion/header-image-url relative-file-path]]))

(defn- upload-img-and-store-url [url file-name share-hash]
  (try
    (let [img-stream (client/get url {:as :stream})
          relative-file-path (s3/upload-stream :schnaq/header-images
                                               (:body img-stream)
                                               file-name
                                               {:content-length (:length img-stream)})]
      (add-bucket-url-to-database relative-file-path share-hash))
    (catch Exception e
      (log/debug (.getMessage e))
      :error-img)))

(defn- valid-url? [url]
  (re-matches trusted-cdn-url-regex url))

(defn- check-and-upload-image [image-url file-name share-hash]
  (if (valid-url? image-url)
    (do (log/debug (format "Set preview image: [%s] for schnaq [%s]" image-url share-hash))
        (upload-img-and-store-url image-url file-name share-hash))
    :error-forbidden-cdn))

(>defn set-preview-image
  "Check an image url for a valid source and then upload it to s3
  and set a datomic entry to the corresponding schnaq."
  [{:keys [parameters]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash image-url]} (:body parameters)
        file-name (str "header-" share-hash)]
    (case (check-and-upload-image image-url file-name share-hash)
      :error-img (bad-request {:error error-img})
      :error-forbidden-cdn (forbidden {:error error-cdn})
      (created "" {:message success-img}))))

(>defn- scale-image-to-width
  "Scale image data url to a specified width and return a map containing input-stream, image-type and content-type"
  [image-data-url width]
  [string? number? :ret map?]
  (try
    (let [[header image-without-header] (string/split image-data-url #",")
          image-bytes (.decode (Base64/getDecoder) image-without-header)
          image-type (second (re-find #"/([A-z]*);" header))
          content-type (second (re-find #":(([A-z]*)/[A-z]*);" header))
          resized-image (resizer-core/resize-to-width (io/input-stream image-bytes) width)
          input-stream (when image-type (resizer-format/as-stream resized-image image-type))]
      {:input-stream input-stream
       :image-type image-type
       :content-type content-type})
    (catch Exception e
      (log/warn "Converting image failed with exception:" e))))

(>defn- create-UUID-file-name
  "Generates a UUID based on a unique id with a file type suffix."
  [id file-type]
  [string? string? :ret string?]
  (when (and id file-type)
    (str (UUID/nameUUIDFromBytes (.getBytes (str id))) "." file-type)))

(>defn upload-image!
  "Scale and upload an image to s3."
  ([file-name image-type image-content target-image-width bucket-key]
   [:file/name :file/type :file/content number? keyword? => ::specs/file-stored]
   (upload-image! file-name image-type image-content target-image-width bucket-key true))
  ([file-name image-type image-content target-image-width bucket-key uuid-filename?]
   [:file/name :file/type :file/content number? keyword? boolean? => ::specs/file-stored]
   (if (shared-config/allowed-mime-types image-type)
     (if-let [{:keys [input-stream image-type content-type]}
              (scale-image-to-width image-content target-image-width)]
       (if-let [image-name (if uuid-filename?
                             file-name
                             (create-UUID-file-name file-name image-type))]
         (let [absolute-url (s3/upload-stream bucket-key
                                              input-stream
                                              image-name
                                              {:content-type content-type})]
           {:url absolute-url})
         {:error :image.error/could-not-create-file-name
          :message "Could not create file-name. Maybe you are not authenticated or you did not provide a file-type."})
       (do
         (log/warn "Conversion of image failed.")
         {:error :image.error/scaling
          :message "Could not scale image."}))
     (do
       (log/warn "Invalid file type received.")
       {:error :image.error/invalid-file-type
        :message (format "Invalid image uploaded. Received %s, expected one of: %s" image-type (string/join ", " shared-config/allowed-mime-types))}))))

;; -----------------------------------------------------------------------------

(def ^:private sample-file
  {:name "sample-file.txt"
   :size 32
   :type "text/plain"
   :content "data:application/octet-stream;base64,V2lsbGtvbW1lbiBpbSBzY2huYXFxaXBhcmFkaWVzIQo="})

(>defn file->stream
  "Convert a file to a stream."
  [{:keys [content]}]
  [::specs/file => :type/input-stream]
  (let [[_header file-without-header] (string/split content #",")
        bytes (.decode (Base64/getDecoder) file-without-header)]
    (io/input-stream bytes)))

(>defn upload-file!
  "Upload a file to s3."
  [file path-to-file bucket-key]
  [::specs/file string? keyword? => ::specs/file-stored]
  (let [absolute-url (s3/upload-stream bucket-key
                                       (file->stream file)
                                       path-to-file
                                       {:content-type (:type file)})]
    {:url absolute-url}))
