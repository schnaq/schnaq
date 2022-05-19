(ns schnaq.api.common
  (:require [com.fulcrologic.guardrails.core :refer [=> >defn-]]
            [ring.util.http-response :refer [created ok bad-request]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.specs :as specs]
            [schnaq.export :as export]
            [schnaq.media :as media]
            [taoensso.timbre :as log]))

(defn- ping
  "Route to ping the API. Used in our monitoring system."
  [_]
  (ok {:text "🧙‍♂️"}))

(defn- check-credentials-opt-add-as-admin!
  "The middleware calling this function checks for the validity of the credentials.
  If the user is logged in, add them as admin."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash]} (:body parameters)
        keycloak-id (:sub identity)]
    (when keycloak-id
      (discussion-db/add-admin-to-discussion share-hash keycloak-id))
    (ok {:valid-credentials? true})))

(defn- export-as-argdown
  "Exports the complete discussion in an argdown-formatted file."
  [{{{:keys [share-hash]} :query} :parameters}]
  (let [statements (discussion-db/all-statements share-hash)]
    (log/info "User is generating an argdown export for discussion" share-hash)
    (ok {:string-representation (export/generate-argdown statements)})))

(defn- export-as-fulltext
  "Exports the complete discussion as an fulltext file."
  [{{{:keys [share-hash]} :query} :parameters}]
  (let [statements (discussion-db/all-statements share-hash)]
    (log/info "User is generating a fulltext export for discussion" share-hash)
    (ok {:string-representation (export/generate-fulltext statements)})))

;; -----------------------------------------------------------------------------

(>defn- file-name
  "Create a file name to store assets for a schnaq."
  [share-hash file-type]
  [:discussion/share-hash :image/type => string?]
  (format "%s/files/%s/image.%s" share-hash (str (random-uuid)) (media/mime-type->file-ending file-type)))

(defn- upload-image
  "Upload an image to a given bucket."
  [{{{:keys [image bucket share-hash]} :body} :parameters}]
  (let [{:keys [image-url error message]}
        (media/upload-image!
         (file-name share-hash (:type image))
         (:type image) (:content image) config/image-width-in-statement bucket)]
    (if image-url
      (created "" {:url image-url})
      (bad-request {:error error
                    :message message}))))

;; -----------------------------------------------------------------------------

(def other-routes
  [["" {:swagger {:tags ["other"]}}
    ["/ping" {:get ping
              :name :api.other/ping
              :description (at/get-doc #'ping)
              :responses {200 {:body {:text string?}}}}]
    ["/export" {:middleware [:discussion/valid-share-hash?]
                :parameters {:query {:share-hash :discussion/share-hash}}
                :responses {200 {:body {:string-representation string?}}
                            404 at/response-error-body}}
     ["/argdown" {:get export-as-argdown
                  :description (at/get-doc #'export-as-argdown)}]
     ["/fulltext" {:get export-as-fulltext
                   :description (at/get-doc #'export-as-fulltext)}]]
    ["/upload/image" {:put upload-image
                      :description (at/get-doc #'upload-image)
                      :middleware [:discussion/valid-share-hash?]
                      :parameters {:body {:image ::specs/image
                                          :bucket keyword?
                                          :share-hash :discussion/share-hash}}
                      :responses {201 {:body {:url string?}}
                                  400 at/response-error-body}}]
    ["/credentials/validate" {:post check-credentials-opt-add-as-admin!
                              :description (at/get-doc #'check-credentials-opt-add-as-admin!)
                              :middleware [:discussion/valid-credentials?]
                              :responses {200 {:body {:valid-credentials? boolean?}}
                                          403 at/response-error-body}
                              :parameters {:body {:share-hash :discussion/share-hash
                                                  :edit-hash :discussion/edit-hash}}}]]])
