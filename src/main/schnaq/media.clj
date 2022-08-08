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
  (:import (java.util Base64)
           (javax.imageio ImageIO)))

(def ^:private trusted-cdn-url-regex
  (re-pattern "https://cdn\\.pixabay\\.com/photo(.+)|https://s3\\.(disqtec|schnaq)\\.com/(.+)"))

(def ^:private error-cdn "prohibited cdn")
(def ^:private error-img "Setting image failed")
(def ^:private success-img "Setting image succeeded")

(defn image-type->file-ending
  "Either use hardcoded file-endings or guess it by its mime-type."
  [mime-type]
  (if-let [file-ending (get {"image/jpeg" "jpg"
                             "image/png" "png"
                             "image/webp" "webp"
                             "image/gif" "gif"}
                            mime-type)]
    file-ending
    (second (.split mime-type "/"))))

(>defn image?
  "Check if the type of a file is an image."
  [file]
  [::specs/file => boolean?]
  (contains? shared-config/allowed-mime-types-images (:type file)))

(>defn file->stream
  "Convert a file to a stream."
  [{:keys [content]}]
  [::specs/file => :type/input-stream]
  (let [[_header file-without-header] (string/split content #",")
        bytes (.decode (Base64/getDecoder) file-without-header)]
    (io/input-stream bytes)))

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
  "Scales images to target-width.
  If the provided image is resizable and too big, convert the width, else return
  the image as a stream."
  [image target-width]
  [::specs/file number? :ret map?]
  (if (or (= (:type image) "image/png")
          (= (:type image) "image/jpg"))
    (try
      (let [image-stream (file->stream image)
            file-ending (image-type->file-ending (:type image))
            image-width (.getWidth (ImageIO/read image-stream))]
        (if (< target-width image-width)
          (let [resized-image (resizer-core/resize-to-width (file->stream image) target-width)
                input-stream (resizer-format/as-stream resized-image file-ending)]
            {:input-stream input-stream
             :content-type (:type image)})
          ;; This stream must again be read, because ImageIO consumes the stream.
          {:input-stream (file->stream image)
           :content-type (:type image)}))
      (catch Exception e
        (log/warn "Converting image failed with exception:" e)))
    {:input-stream (file->stream image)
     :content-type (:type image)}))

;; -----------------------------------------------------------------------------

(>defn upload-image!
  "Scale and upload an image to s3."
  ([image file-name target-image-width bucket-key]
   [::specs/file :file/name number? keyword? => ::specs/file-stored]
   (if-let [{:keys [input-stream content-type]} (scale-image-to-width image target-image-width)]
     (let [absolute-url (s3/upload-stream bucket-key input-stream file-name {:content-type content-type})]
       {:url absolute-url})
     (do
       (log/warn "Conversion of image failed.")
       {:error :image.error/scaling
        :message "Could not scale image."}))))

(>defn upload-file!
  "Upload a file to s3."
  [file path-to-file bucket-key]
  [::specs/file string? keyword? => ::specs/file-stored]
  (let [absolute-url (s3/upload-stream bucket-key
                                       (file->stream file)
                                       path-to-file
                                       {:content-type (:type file)})]
    {:url absolute-url}))
